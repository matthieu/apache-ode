/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ode.axis2.service;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.activation.DataHandler;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMText;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.receivers.AbstractMessageReceiver;
import org.apache.axis2.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.axis2.OdeFault;
import org.apache.ode.axis2.deploy.DeploymentPoller;
import org.apache.ode.axis2.hooks.ODEAxisService;
import org.apache.ode.bpel.iapi.BpelServer;
import org.apache.ode.bpel.iapi.ProcessConf;
import org.apache.ode.bpel.iapi.ProcessStore;
import org.apache.ode.il.OMUtils;
import org.apache.ode.utils.fs.FileUtils;

/**
 * Axis wrapper for process deployment.
 */
public class DeploymentWebService {

    private static final Log __log = LogFactory.getLog(DeploymentWebService.class);

    private final OMNamespace _pmapi;

    private File _deployPath;
    private DeploymentPoller _poller;
    private ProcessStore _store;


    public DeploymentWebService() {
        _pmapi = OMAbstractFactory.getOMFactory().createOMNamespace("http://www.apache.org/ode/pmapi","pmapi");
    }

    public void enableService(AxisConfiguration axisConfig, BpelServer server, ProcessStore store,
                              DeploymentPoller poller, String rootpath, String workPath) {
        _deployPath = new File(workPath, "processes");
        _store = store;

        Definition def;
        try {
            WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
            wsdlReader.setFeature("javax.wsdl.verbose", false);

            File wsdlFile = new File(rootpath + "/deploy.wsdl");
            def = wsdlReader.readWSDL(wsdlFile.toURI().toString());
            AxisService deployService = ODEAxisService.createService(
                    axisConfig, new QName("http://www.apache.org/ode/deployapi", "DeploymentService"),
                    "DeploymentPort", "DeploymentService", def, new DeploymentMessageReceiver());
            axisConfig.addService(deployService);
            _poller = poller;
        } catch (WSDLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class DeploymentMessageReceiver extends AbstractMessageReceiver {

        public void invokeBusinessLogic(MessageContext messageContext) throws AxisFault {
            String operation = messageContext.getAxisOperation().getName().getLocalPart();
            SOAPFactory factory = getSOAPFactory(messageContext);
            boolean unknown = false;

            try {
                if (operation.equals("deploy")) {
                    OMElement namePart = messageContext.getEnvelope().getBody().getFirstElement().getFirstElement();
                    OMElement zipPart = (OMElement) namePart.getNextOMSibling();
                    OMElement zip = (zipPart == null) ? null : zipPart.getFirstElement();
                    if (zip == null || !zipPart.getQName().getLocalPart().equals("package") 
                            || !zip.getQName().getLocalPart().equals("zip"))
                        throw new OdeFault("Your message should contain an element named 'package' with a 'zip' element"); 

                    OMText binaryNode = (OMText) zip.getFirstOMChild();
                    if (binaryNode == null) {
                        throw new OdeFault("Empty binary node under <zip> element");
                    }
                    binaryNode.setOptimize(true);
                    try {
                        // We're going to create a directory under the deployment root and put
                        // files in there. The poller shouldn't pick them up so we're asking
                        // it to hold on for a while.
                        _poller.hold();

                        File dest = new File(_deployPath, namePart.getText() + "-" + _store.getCurrentVersion());
                        dest.mkdir();
                        unzip(dest, (DataHandler) binaryNode.getDataHandler());

                        // Check that we have a deploy.xml
                        File deployXml = new File(dest, "deploy.xml");
                        if (!deployXml.exists())
                            throw new OdeFault("The deployment doesn't appear to contain a deployment " +
                                    "descriptor in its root directory named deploy.xml, aborting.");

                        Collection<QName> deployed = _store.deploy(dest);

                        File deployedMarker = new File(_deployPath, dest.getName() + ".deployed");
                        deployedMarker.createNewFile();

                        // Telling the poller what we deployed so that it doesn't try to deploy it again
                        _poller.markAsDeployed(dest);
                        __log.info("Deployment of artifact " + dest.getName() + " successful.");

                        OMElement response = factory.createOMElement("response", null);

                        if (__log.isDebugEnabled()) __log.debug("Deployed package: "+dest.getName());
                        OMElement d = factory.createOMElement("name", null);
                        d.setText(dest.getName());
                        response.addChild(d);

                        for (QName pid : deployed) {
                            if (__log.isDebugEnabled()) __log.debug("Deployed PID: "+pid);
                            d = factory.createOMElement("id", null);
                            d.setText(pid);
                            response.addChild(d);
                        }
                        sendResponse(factory, messageContext, "deployResponse", response);
                    } finally {
                        _poller.release();
                    }
                } else if (operation.equals("undeploy")) {
                    OMElement part = messageContext.getEnvelope().getBody().getFirstElement().getFirstElement();

                    String pkg = part.getText();
                    File deploymentDir = new File(_deployPath, pkg);
                    if (!deploymentDir.exists())
                        throw new OdeFault("Couldn't find deployment package " + pkg + " in directory " + _deployPath);

                    try {
                        // We're going to create a directory under the deployment root and put
                        // files in there. The poller shouldn't pick them up so we're asking
                        // it to hold on for a while.
                        _poller.hold();

                        Collection<QName> undeployed = _store.undeploy(deploymentDir);

                        File deployedMarker = new File(_deployPath, pkg + ".deployed");
                        deployedMarker.delete();
                        FileUtils.deepDelete(new File(_deployPath, pkg));

                        OMElement response = factory.createOMElement("response", null);
                        response.setText("" + (undeployed.size() > 0));
                        sendResponse(factory, messageContext, "undeployResponse", response);
                        _poller.markAsUndeployed(deploymentDir);
                    } finally {
                        _poller.release();
                    }
                } else if (operation.equals("listDeployedPackages")) {
                    Collection<String> packageNames = _store.getPackages();
                    OMElement response = factory.createOMElement("deployedPackages", null);
                    for (String name : packageNames) {
                        OMElement nameElmt = factory.createOMElement(new QName( "http://www.apache.org/ode/deployapi","name"));
                        nameElmt.setText(name);
                        response.addChild(nameElmt);
                    }
                    sendResponse(factory, messageContext, "listDeployedPackagesResponse", response);
                } else if (operation.equals("listProcesses")) {
                    OMElement namePart = messageContext.getEnvelope().getBody().getFirstElement().getFirstElement();
                    List<QName> processIds = _store.listProcesses(namePart.getText());
                    OMElement response = factory.createOMElement("processIds", null);
                    for (QName qname : processIds) {
                        OMElement nameElmt = factory.createOMElement("id", null);
                        nameElmt.setText(qname);
                        response.addChild(nameElmt);
                    }
                    sendResponse(factory, messageContext, "listProcessResponse", response);
                } else if (operation.equals("getProcessPackage")) {
                    OMElement qnamePart = messageContext.getEnvelope().getBody().getFirstElement().getFirstElement();
                    ProcessConf process = _store.getProcessConfiguration(OMUtils.getTextAsQName(qnamePart));
                    if (process == null) {
                        throw new OdeFault("Could not find process: " + qnamePart.getTextAsQName());
                    }
                    String packageName = _store.getProcessConfiguration(OMUtils.getTextAsQName(qnamePart)).getPackage();
                    OMElement response = factory.createOMElement("packageName", null);
                    response.setText(packageName);
                    sendResponse(factory, messageContext, "getProcessPackageResponse", response);
                } else unknown = true;
            } catch (Throwable t) {
                // Trying to extract a meaningful message
                Throwable source = t;
                while (source.getCause() != null && source.getCause() != source) source = source.getCause();
                __log.warn("Invocation of operation " + operation + " failed", t);
                throw new OdeFault("Invocation of operation " + operation + " failed: " + source.toString(), t);
            }
            if (unknown) throw new OdeFault("Unknown operation: '"
                    + messageContext.getAxisOperation().getName() + "'");
        }

        private File buildUnusedDir(File deployPath, String dirName) {
            int v = 1;
            while (new File(deployPath, dirName + "-" + v).exists()) v++;
            return new File(deployPath, dirName + "-" + v);
        }

        private void unzip(File dest, DataHandler dataHandler) throws AxisFault {
            try {
                ZipInputStream zis = new ZipInputStream(dataHandler.getDataSource().getInputStream());
                ZipEntry entry;
                // Processing the package
                while((entry = zis.getNextEntry()) != null) {
                    if(entry.isDirectory()) {
                        __log.debug("Extracting directory: " + entry.getName());
                        new File(dest, entry.getName()).mkdir();
                        continue;
                    }
                    __log.debug("Extracting file: " + entry.getName());
                    File destFile = new File(dest, entry.getName());
                    if (!destFile.getParentFile().exists()) destFile.getParentFile().mkdirs();
                    copyInputStream(zis, new BufferedOutputStream(
                            new FileOutputStream(destFile)));
                }
                zis.close();
            } catch (IOException e) {
                throw new OdeFault("An error occured on deployment.", e);
            }
        }

        private void sendResponse(SOAPFactory factory, MessageContext messageContext, String op,
                                  OMElement response) throws AxisFault {
            MessageContext outMsgContext = Utils.createOutMessageContext(messageContext);
            outMsgContext.getOperationContext().addMessageContext(outMsgContext);

            SOAPEnvelope envelope = factory.getDefaultEnvelope();
            outMsgContext.setEnvelope(envelope);

            OMElement responseOp = factory.createOMElement(op, _pmapi);
            responseOp.addChild(response);
            envelope.getBody().addChild(responseOp);
            AxisEngine.send(outMsgContext);
        }
    }

    private static void copyInputStream(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while((len = in.read(buffer)) >= 0)
            out.write(buffer, 0, len);
        out.close();
    }

}
