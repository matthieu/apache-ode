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

package org.apache.ode.axis2.hooks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.transport.jms.JMSConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.axis2.OdeFault;
import org.apache.ode.axis2.util.Axis2UriResolver;
import org.apache.ode.axis2.util.Axis2WSDLLocator;
import org.apache.ode.bpel.iapi.ProcessConf;
import org.apache.ode.utils.AxisUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.w3c.dom.Element;

/**
 * Implementation of Axis Service used by ODE iapi to enlist itself its service. Allows us to build the service using a
 * WSDL definition using our own receiver.
 */
public class ODEAxisService {

    private static final Log LOG = LogFactory.getLog(ODEAxisService.class);

    public static AxisService createService(AxisConfiguration axisConfig, ProcessConf pconf, QName wsdlServiceName,
                                            String portName, String axisServiceName) throws AxisFault {
        Definition wsdlDefinition = pconf.getDefinitionForService(wsdlServiceName);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Create AxisService:"+" service="+wsdlServiceName+" port="+portName
                    +" WSDL="+wsdlDefinition.getDocumentBaseURI()+" BPEL="+pconf.getBpelDocument());
        }

        try {
            URI baseUri = pconf.getBaseURI().resolve(wsdlDefinition.getDocumentBaseURI());
            InputStream is = baseUri.toURL().openStream();
            WSDL11ToAxisServiceBuilder serviceBuilder = new WSDL11ToAxisPatchedBuilder(is, wsdlServiceName, portName);
            serviceBuilder.setBaseUri(baseUri.toString());
            serviceBuilder.setCustomResolver(new Axis2UriResolver());
            serviceBuilder.setCustomWSLD4JResolver(new Axis2WSDLLocator(baseUri));
            serviceBuilder.setServerSide(true);

            AxisService axisService = serviceBuilder.populateService();
            axisService.setParent(axisConfig);
            axisService.setName(axisServiceName);
            axisService.setWsdlFound(true);
            axisService.setCustomWsdl(true);
            axisService.setClassLoader(axisConfig.getServiceClassLoader());
        
            URL wsdlUrl = null;
            for (File file : pconf.getFiles()) {
                if (file.getAbsolutePath().indexOf(wsdlDefinition.getDocumentBaseURI()) > 0)
                    wsdlUrl = file.toURI().toURL();
            }
            if (wsdlUrl != null) axisService.setFileName(wsdlUrl);

            // axis2 service configuration  
            URL service_file = pconf.getBaseURI().resolve(wsdlServiceName.getLocalPart()+".axis2").toURL();
            LOG.debug("Looking for Axis2 service configuration file: "+service_file);
            try {
                AxisUtils.configureService(axisService, service_file);
            } catch (FileNotFoundException except) {
                LOG.debug("Axis2 service configuration not found: " + service_file);
            } catch (IOException except) {
                LOG.warn("Exception while configuring service: " + service_file, except);
            }

            // In doc/lit we need to declare a mapping between operations and message element names
            // to be able to route properly.
            declarePartsElements(wsdlDefinition, wsdlServiceName, axisServiceName, portName);

            Iterator operations = axisService.getOperations();
            ODEMessageReceiver msgReceiver = new ODEMessageReceiver();
            while (operations.hasNext()) {
                AxisOperation operation = (AxisOperation) operations.next();
                if (operation.getMessageReceiver() == null) {
                    operation.setMessageReceiver(msgReceiver);
                }
            }
            
            // Set the JMS destination name on the Axis Service
            if (isJmsEndpoint(pconf, wsdlServiceName, portName)) {
                axisService.addParameter(new Parameter(JMSConstants.DEST_PARAM, 
                        extractJMSDestinationName(axisServiceName, deriveBaseServiceUri(pconf))));
            }

            return axisService;
        } catch (Exception e) {
            throw AxisFault.makeFault(e);
        }
    }

    /**
     * Extract the JMS destination name that is embedded in the Axis service name.
     * @param serviceName the name of the axis service
     * @return the corresponding JMS destination name
     */
    private static String extractJMSDestinationName(String serviceName, String baseUri) {
        String destinationPrefix = "dynamicQueues/";
        int index = serviceName.indexOf(destinationPrefix);
        if (index == -1) {
            destinationPrefix = "dynamicTopics/";
            index = serviceName.indexOf(destinationPrefix);
        }
        if (index == -1) {
            destinationPrefix = baseUri + "/";
            index = serviceName.indexOf(destinationPrefix);
            return (index != -1) ? serviceName.substring(destinationPrefix.length()) : serviceName;
        } else {
            return serviceName.substring(index);
        }
    }

    public static AxisService createService(AxisConfiguration axisConfig, QName serviceQName, String port,
                                            String axisName, Definition wsdlDef, MessageReceiver receiver) throws AxisFault {

        WSDL11ToAxisServiceBuilder serviceBuilder = new WSDL11ToAxisServiceBuilder(wsdlDef, serviceQName, port);
        AxisService axisService = serviceBuilder.populateService();
        axisService.setName(axisName);
        axisService.setWsdlFound(true);
        axisService.setClassLoader(axisConfig.getServiceClassLoader());
        Iterator operations = axisService.getOperations();
        while (operations.hasNext()) {
            AxisOperation operation = (AxisOperation) operations.next();
            if (operation.getMessageReceiver() == null) {
                operation.setMessageReceiver(receiver);
            }
        }
        return axisService;
    }

    private static String extractEndpointUri(ProcessConf pconf, QName wsdlServiceName, String portName) 
            throws AxisFault {
        Definition wsdlDefinition = pconf.getDefinitionForService(wsdlServiceName);
        String url = null;
        Service service = wsdlDefinition.getService(wsdlServiceName);
        if (service == null) {
            throw new OdeFault("Unable to find service " + wsdlServiceName + " from service WSDL definition "
                    + wsdlDefinition.getDocumentBaseURI());
        }
        Port port = service.getPort(portName);
        if (port == null) {
            throw new OdeFault("Couldn't find port " + portName + " in definition " + wsdlServiceName);
        }
        for (Object oext : port.getExtensibilityElements()) {
            if (oext instanceof SOAPAddress)
                url = ((SOAPAddress) oext).getLocationURI();
        }
        if (url == null) {
            throw new OdeFault("Could not extract any soap:address from service WSDL definition " + wsdlServiceName
                    + " (necessary to establish the process target address)!");
        }       
        return url;
    }
    
    private static boolean isJmsEndpoint(ProcessConf pconf, QName wsdlServiceName, String portName) 
            throws AxisFault {
        String url = extractEndpointUri(pconf, wsdlServiceName, portName);
        return url.startsWith("jms:");
    }
    
    public static String extractServiceName(ProcessConf pconf, QName wsdlServiceName, String portName)
            throws AxisFault {
        String serviceName = parseURLForService(extractEndpointUri(pconf, wsdlServiceName, portName), deriveBaseServiceUri(pconf));
        if (serviceName == null) {
            throw new OdeFault("The soap:address used for service WSDL definition " + wsdlServiceName + " and port "
                    + portName + " should be of the form http://hostname:port/ode/processes/myProcessEndpointName");
        }
        return serviceName;
    }

    /**
     * Obtain the service name from the request URL. The request URL is expected to use the path "/processes/" under
     * which all processes and their services are listed. Returns null if the path does not contain this part.
     */
    protected static String parseURLForService(String path, String baseUri) {
        // Assume that path is HTTP-based, by default
        String servicePrefix = "/processes/";
        // Don't assume JMS-based paths start the same way 
        if (path.startsWith("jms:/")) {
            servicePrefix = "jms:/";
        }
        int index = path.indexOf(servicePrefix);
        if (-1 != index) {
            String service;

            int serviceStart = index + servicePrefix.length();
            if (path.length() > serviceStart + 1) {
                service = path.substring(serviceStart);
                // Path may contain query string, not interesting for us.
                int queryIndex = service.indexOf('?');
                if (queryIndex > 0) {
                    service = service.substring(0, queryIndex);
                }
                // Qualify shared JMS names with unique baseUri
                if (path.startsWith("jms:/")) {
                    boolean slashPresent = baseUri.endsWith("/") || service.startsWith("/");
                    // service = baseUri + (slashPresent ? "" : "/") + service; // allow successive slashes ("//") in the URI
                    service = baseUri + "/" + service;
                }
                return service;
            }
        }
        return null;
    }

    private static void declarePartsElements(Definition wsdlDefinition, QName wsdlServiceName, String axisServiceName,
                                             String portName) {
        List wsldOps = wsdlDefinition.getService(wsdlServiceName).getPort(portName).getBinding().getPortType()
                .getOperations();
        for (Object wsldOp : wsldOps) {
            Operation wsdlOp = (Operation) wsldOp;
            Collection parts = wsdlOp.getInput().getMessage().getParts().values();
            // More than one part, it's rpc/enc, no mapping needs to be declared
            if (parts.size() == 1) {
                Part part = (Part) parts.iterator().next();
                // Parts are types, it's rpc/enc, no mapping needs to be declared
                if (part.getElementName() != null)
                    ODEAxisOperationDispatcher.addElmtToOpMapping(axisServiceName, wsdlOp.getName(), part.getElementName()
                            .getLocalPart());
            }
        }
    }

    // Axis2 monkey patching to force the usage of the read(element,baseUri) method
    // of XmlSchema as the normal read is broken.
    public static class WSDL11ToAxisPatchedBuilder extends WSDL11ToAxisServiceBuilder {
        public WSDL11ToAxisPatchedBuilder(InputStream in, QName serviceName, String portName) {
            super(in, serviceName, portName);
        }
        public WSDL11ToAxisPatchedBuilder(Definition def, QName serviceName, String portName) {
            super(def, serviceName, portName);
        }
        public WSDL11ToAxisPatchedBuilder(Definition def, QName serviceName, String portName, boolean isAllPorts) {
            super(def, serviceName, portName, isAllPorts);
        }
        public WSDL11ToAxisPatchedBuilder(InputStream in, AxisService service) {
            super(in, service);
        }
        public WSDL11ToAxisPatchedBuilder(InputStream in) {
            super(in);
        }

        protected XmlSchema getXMLSchema(Element element, String baseUri) {
            XmlSchemaCollection schemaCollection = new XmlSchemaCollection();
            if (baseUri != null) {
                schemaCollection.setBaseUri(baseUri);
            }
            return schemaCollection.read(element, baseUri);
        }
    }

    /*
     * Generates a URI of the following form:
     *     ${deploy_bundleNcName}/${diagram_relativeURL}/${process_relativeURL}
     * When a service name (local part only) is qualified (prefixed) with the above,
     * it results in a unique identifier that may be used as that service's name.  
     */
    public static String deriveBaseServiceUri(ProcessConf pconf) {
        if (pconf != null) {
            StringBuffer baseServiceUri = new StringBuffer();
            String bundleName = pconf.getPackage();
            if (bundleName != null) {
                baseServiceUri.append(bundleName).append("/");
                if (pconf.getBpelDocument() != null) {
                    String bpelDocumentName = pconf.getBpelDocument();
                    if (bpelDocumentName.indexOf(".") > 0) {
                        bpelDocumentName = bpelDocumentName.substring(0, bpelDocumentName.indexOf("."));
                    }
                    baseServiceUri.append(bpelDocumentName).append("/");
                    String processName = pconf.getType() != null 
                        ? pconf.getType().getLocalPart() : null;
                    if (processName != null) {
                        baseServiceUri.append(processName);
                        return baseServiceUri.toString();
                    }
                }
            }
            
        }
        return null;
    }

}
