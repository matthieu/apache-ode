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
package org.apache.ode.axis2;

import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.PortType;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import junit.framework.TestCase;

import org.apache.axiom.om.impl.llom.factory.OMXMLBuilderFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.axiom.soap.impl.dom.soap11.SOAP11Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.ode.axis2.util.SoapMessageConverter;
import org.apache.ode.utils.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SoapMessageConverterTest extends TestCase {

    Definition wsdl1, wsdlHW;

    String wsdl1tns = "http://documentum.com/ws/2005/services";

    QName repoService = new QName(wsdl1tns, "RepoAccessorService");

    QName portTypeName = new QName(wsdl1tns, "RepoAccessor");

    String portName = "RepoAccessor";

    SoapMessageConverter portmapper;

    PortType portType, portTypeHW;

    Document req1bad;

    Document req1;

    private Operation op1, opHello;

    public SoapMessageConverterTest() throws Exception {
        req1bad = DOMUtils.parse(getClass().getResourceAsStream("/testRequest1Bad.xml"));
        req1 = DOMUtils.parse(getClass().getResourceAsStream("/testRequest1.xml"));
        WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
        wsdl1 = reader.readWSDL(getClass().getResource("/test1.wsdl").toExternalForm());
        portType = wsdl1.getPortType(portTypeName);
        op1 = portType.getOperation("getObjectId", null, null);

        wsdlHW = reader.readWSDL(getClass().getResource("/HelloWorld.wsdl").toExternalForm());
        portTypeHW = wsdlHW.getPortType(new QName(wsdlHW.getTargetNamespace(), "HelloPortType"));
        opHello = portTypeHW.getOperation("hello", null, null);
    }

    public void setUp() throws Exception {
        portmapper = new SoapMessageConverter(wsdl1, repoService, portName, true);
    }

    public void tearDown() {

    }

    public void testBadPortName() {
        try {
            new SoapMessageConverter(wsdl1, repoService, "badPort", true);
            fail("Should have thrown axis error.");
        } catch (AxisFault af) {
            ;// expected
        }
    }

    public void testBadServiceName() {
        try {
            new SoapMessageConverter(wsdl1, new QName(wsdl1tns, "foobar"), portName, true);
            fail("Should have thrown axis error.");
        } catch (AxisFault af) {
            ;// expected
        }
    }

    public void testCreateSOAPRequest() throws Exception {
        MessageContext msgCtx = new MessageContext();
        portmapper.createSoapRequest(msgCtx, req1.getDocumentElement(), portType.getOperation("getObjectId", null, null));
        SOAPEnvelope env = msgCtx.getEnvelope();
        System.out.println("testCreateSOAPRequest: " + env);
        QName elPartName = new QName(wsdl1tns, "getObjectId");
        assertNotNull(env.getBody());
        assertNotNull(env.getBody().getFirstElement());
        // doc-lit style, no part wrapper
        assertEquals(elPartName, env.getBody().getFirstElement().getQName());

    }

    public void testCreateSOAPRequestFail() throws Exception {
        MessageContext msgCtx = new MessageContext();
        try {
            portmapper.createSoapRequest(msgCtx, req1bad.getDocumentElement(), portType.getOperation("getObjectId", null,
                    null));
            fail("Should have caused an ex");
        } catch (AxisFault af) {
            ; // expected
        }
    }

    public void testGetSoapAction() throws Exception {
        assertEquals("getObjectIdAction", portmapper.getSoapAction("getObjectId"));
        assertEquals("", portmapper.getSoapAction("foo"));
    }

    public void testParseRequest() throws Exception {
        MessageContext msgCtx = new MessageContext();
        portmapper.createSoapRequest(msgCtx, req1.getDocumentElement(), op1);
        SOAPEnvelope env = msgCtx.getEnvelope();
        System.out.println("testParseRequest: " + env);
        Element odeMsg = DOMUtils.stringToDOM("<message/>");
        portmapper.parseSoapRequest(odeMsg, env, op1);
        System.out.println("testParseRequest: " + DOMUtils.domToString(odeMsg));
        Element params = DOMUtils.findChildByName(odeMsg, new QName(null, "parameters"));
        assertNotNull(params);
        Element hdr = DOMUtils.findChildByName(odeMsg, new QName("urn:ode.apache.org/axis2-il/header",
                "DocumentumRequestHeader"));
        assertNotNull(hdr);
    }

    /** Make sure hello world request parses correctly. */
    public void testHelloWorldRequest() throws Exception {
        SoapMessageConverter portmaper1 = new SoapMessageConverter(wsdlHW, new QName(wsdlHW
                .getTargetNamespace(), "HelloService"), "HelloPort", false);

        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(
                getClass().getResourceAsStream("/HelloWorldRequest.soap"));
        StAXSOAPModelBuilder builder = OMXMLBuilderFactory.createStAXSOAPModelBuilder(new SOAP11Factory(), sr);
        SOAPEnvelope se = builder.getSOAPEnvelope();

        Element msg = DOMUtils.stringToDOM("<message/>");
        portmaper1.parseSoapRequest(msg, se, opHello);
        System.out.println(DOMUtils.domToString(msg));
    }

}
