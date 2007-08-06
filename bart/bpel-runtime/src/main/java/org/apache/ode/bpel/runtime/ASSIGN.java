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
package org.apache.ode.bpel.runtime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.common.FaultException;
import org.apache.ode.bpel.evt.PartnerLinkModificationEvent;
import org.apache.ode.bpel.evt.ScopeEvent;
import org.apache.ode.bpel.evt.VariableModificationEvent;
import org.apache.ode.bpel.explang.EvaluationContext;
import org.apache.ode.bpel.explang.EvaluationException;
import org.apache.ode.bpel.o.OAssign;
import org.apache.ode.bpel.o.OAssign.DirectRef;
import org.apache.ode.bpel.o.OAssign.LValueExpression;
import org.apache.ode.bpel.o.OAssign.PropertyRef;
import org.apache.ode.bpel.o.OAssign.VariableRef;
import org.apache.ode.bpel.o.OElementVarType;
import org.apache.ode.bpel.o.OExpression;
import org.apache.ode.bpel.o.OLink;
import org.apache.ode.bpel.o.OMessageVarType;
import org.apache.ode.bpel.o.OMessageVarType.Part;
import org.apache.ode.bpel.o.OProcess.OProperty;
import org.apache.ode.bpel.o.OScope;
import org.apache.ode.bpel.o.OScope.Variable;
import org.apache.ode.bpel.runtime.channels.FaultData;
import org.apache.ode.utils.DOMUtils;
import org.apache.ode.utils.Namespaces;
import org.apache.ode.utils.msg.MessageBundle;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Assign activity run-time template.
 */
class ASSIGN extends ACTIVITY {
    private static final long serialVersionUID = 1L;

    private static final Log __log = LogFactory.getLog(ASSIGN.class);

    private static final ASSIGNMessages __msgs = MessageBundle
            .getMessages(ASSIGNMessages.class);

    public ASSIGN(ActivityInfo self, ScopeFrame scopeFrame, LinkFrame linkFrame) {
        super(self, scopeFrame, linkFrame);
    }

    public void run() {
        OAssign oassign = getOAsssign();

        FaultData faultData = null;

        for (OAssign.Copy aCopy : oassign.copy) {
            try {
                copy(aCopy);
            } catch (FaultException fault) {
                faultData = createFault(fault.getQName(), aCopy, fault
                        .getMessage());
                break;
            }
        }

        if (faultData != null) {
            __log.error("Assignment Fault: " + faultData.getFaultName()
                    + ",lineNo=" + faultData.getFaultLineNo());
            _self.parent.completed(faultData, CompensationHandler.emptySet());
        } else {
            _self.parent.completed(null, CompensationHandler.emptySet());
        }
    }

    protected Log log() {
        return __log;
    }

    private OAssign getOAsssign() {
        return (OAssign) _self.o;
    }

    private Node evalLValue(OAssign.LValue to) throws FaultException {
        final BpelRuntimeContext napi = getBpelRuntimeContext();
        Node lval = null;
        if (!(to instanceof OAssign.PartnerLinkRef)) {
            VariableInstance lvar = _scopeFrame.resolve(to.getVariable());
            if (!napi.isVariableInitialized(lvar)) {
                Document doc = DOMUtils.newDocument();
                Node val = to.getVariable().type.newInstance(doc);
                if (val.getNodeType() == Node.TEXT_NODE) {
                    Element tempwrapper = doc.createElementNS(null, "temporary-simple-type-wrapper");
                    doc.appendChild(tempwrapper);
                    tempwrapper.appendChild(val);
                    val = tempwrapper;
                } else
                    doc.appendChild(val);
                lval = getBpelRuntimeContext().initializeVariable(lvar, val);
            } else
                lval = napi.fetchVariableData(lvar, true);
        }
        return lval;
    }

    /**
     * Get the r-value. There are several possibilities:
     * <ul>
     * <li>a message is selected - an element representing the whole message is
     * returned.</li>
     * <li>a (element) message part is selected - the element is returned.
     * </li>
     * <li>a (typed) message part is select - a wrapper element is returned.
     * </li>
     * <li>an attribute is selected - an attribute node is returned. </li>
     * <li>a text node/string expression is selected - a text node is returned.
     * </li>
     * </ul>
     *
     * @param from
     *
     * @return Either {@link Element}, {@link org.w3c.dom.Text}, or
     *         {@link org.w3c.dom.Attr} node representing the r-value.
     *
     * @throws FaultException
     *             DOCUMENTME
     * @throws UnsupportedOperationException
     *             DOCUMENTME
     * @throws IllegalStateException
     *             DOCUMENTME
     */
    private Node evalRValue(OAssign.RValue from) throws FaultException {
        if (__log.isDebugEnabled())
            __log.debug("Evaluating FROM expression \"" + from + "\".");

        Node retVal;
        if (from instanceof DirectRef) {
            OAssign.DirectRef dref = (OAssign.DirectRef) from;
            Node data = getBpelRuntimeContext().fetchVariableData(
                    _scopeFrame.resolve(dref.variable), false);
            retVal = DOMUtils.findChildByName((Element)data, dref.elName);
        } else if (from instanceof OAssign.VariableRef) {
            OAssign.VariableRef varRef = (OAssign.VariableRef) from;
            Node data = getBpelRuntimeContext().fetchVariableData(
                    _scopeFrame.resolve(varRef.variable), false);
            retVal = evalQuery(data, varRef.part, varRef.location,
                    getEvaluationContext());
        } else if (from instanceof OAssign.PropertyRef) {
            OAssign.PropertyRef propRef = (OAssign.PropertyRef) from;
            Node data = getBpelRuntimeContext().fetchVariableData(
                    _scopeFrame.resolve(propRef.variable), false);

            retVal = evalQuery(data, propRef.propertyAlias.part,
                    propRef.propertyAlias.location, getEvaluationContext());

        } else if (from instanceof OAssign.PartnerLinkRef) {
            OAssign.PartnerLinkRef pLinkRef = (OAssign.PartnerLinkRef) from;
            PartnerLinkInstance pLink = _scopeFrame
                    .resolve(pLinkRef.partnerLink);
            Node tempVal =pLinkRef.isMyEndpointReference ?
                    getBpelRuntimeContext().fetchMyRoleEndpointReferenceData(pLink)
                    : getBpelRuntimeContext().fetchPartnerRoleEndpointReferenceData(pLink);
            if (__log.isDebugEnabled())
                __log.debug("RValue is a partner link, corresponding endpoint "
                        + tempVal.getClass().getName() + " has value "
                        + DOMUtils.domToString(tempVal));
            retVal = tempVal;
        } else if (from instanceof OAssign.Expression) {
            List l;
            OExpression expr = ((OAssign.Expression) from).expression;
            try {
                l = getBpelRuntimeContext().getExpLangRuntime().evaluate(expr,
                        getEvaluationContext());
            } catch (EvaluationException e) {
                String msg = __msgs.msgEvalException(from.toString(), e
                        .getMessage());
                if (__log.isDebugEnabled())
                    __log.debug(from + ": " + msg);
                if (e.getCause() instanceof FaultException) throw (FaultException)e.getCause();
                throw new FaultException(
                        getOAsssign().getOwner().constants.qnSelectionFailure,
                        msg);
            }
            if (l.size() == 0) {
                String msg = __msgs.msgRValueNoNodesSelected(expr.toString());
                if (__log.isDebugEnabled())
                    __log.debug(from + ": " + msg);
                throw new FaultException(
                        getOAsssign().getOwner().constants.qnSelectionFailure,
                        msg);

            } else if (l.size() > 1) {
                String msg = __msgs.msgRValueMultipleNodesSelected(expr
                        .toString());
                if (__log.isDebugEnabled())
                    __log.debug(from + ": " + msg);
                throw new FaultException(
                        getOAsssign().getOwner().constants.qnSelectionFailure,
                        msg);
            }
            retVal = (Node) l.get(0);
        } else if (from instanceof OAssign.Literal) {
            Element literalRoot = ((OAssign.Literal) from).getXmlLiteral().getDocumentElement();
            assert literalRoot.getLocalName().equals("literal");
            // We'd like a single text node...

            literalRoot.normalize();
            retVal = literalRoot.getFirstChild();

            // Adjust for whitespace before an element.
            if (retVal != null && retVal.getNodeType() == Node.TEXT_NODE
                    && retVal.getTextContent().trim().length() == 0
                    && retVal.getNextSibling() != null) {
                retVal = retVal.getNextSibling();
            }

            if (retVal == null) {
                // Special case, no children --> empty TII
                retVal = literalRoot.getOwnerDocument().createTextNode("");
            } else if (retVal.getNodeType() == Node.ELEMENT_NODE) {
                // Make sure there is no more elements.
                Node x = retVal.getNextSibling();
                while (x != null) {
                    if (x.getNodeType() == Node.ELEMENT_NODE) {
                        String msg = __msgs.msgLiteralContainsMultipleEIIs();
                        if (__log.isDebugEnabled())
                            __log.debug(from + ": " + msg);
                        throw new FaultException(
                                getOAsssign().getOwner().constants.qnSelectionFailure,
                                msg);

                    }
                    x = x.getNextSibling();
                }
            } else if (retVal.getNodeType() == Node.TEXT_NODE) {
                // Make sure there are no elements following this text node.
                Node x = retVal.getNextSibling();
                while (x != null) {
                    if (x.getNodeType() == Node.ELEMENT_NODE) {
                        String msg = __msgs.msgLiteralContainsMixedContent();
                        if (__log.isDebugEnabled())
                            __log.debug(from + ": " + msg);
                        throw new FaultException(
                                getOAsssign().getOwner().constants.qnSelectionFailure,
                                msg);

                    }
                    x = x.getNextSibling();
                }

            }

            if (retVal == null) {
                String msg = __msgs.msgLiteralMustContainTIIorEII();
                if (__log.isDebugEnabled())
                    __log.debug(from + ": " + msg);
                throw new FaultException(
                        getOAsssign().getOwner().constants.qnSelectionFailure,
                        msg);
            }
        } else {
            String msg = __msgs
                    .msgInternalError("Unknown RVALUE type: " + from);
            if (__log.isErrorEnabled())
                __log.error(from + ": " + msg);
            throw new FaultException(
                    getOAsssign().getOwner().constants.qnSelectionFailure, msg);
        }

        // Now verify we got something.
        if (retVal == null) {
            String msg = __msgs.msgEmptyRValue();
            if (__log.isDebugEnabled())
                __log.debug(from + ": " + msg);
            throw new FaultException(
                    getOAsssign().getOwner().constants.qnSelectionFailure, msg);
        }

        // Now check that we got the right thing.
        switch (retVal.getNodeType()) {
            case Node.TEXT_NODE:
            case Node.ATTRIBUTE_NODE:
            case Node.ELEMENT_NODE:
            case Node.CDATA_SECTION_NODE:
                break;
            default:
                String msg = __msgs.msgInvalidRValue();
                if (__log.isDebugEnabled())
                    __log.debug(from + ": " + msg);

                throw new FaultException(
                        getOAsssign().getOwner().constants.qnSelectionFailure, msg);

        }

        return retVal;
    }

    private void copy(OAssign.Copy ocopy) throws FaultException {

        if (__log.isDebugEnabled())
            __log.debug("Assign.copy(" + ocopy + ")");

        final BpelRuntimeContext napi = getBpelRuntimeContext();

        // Check for message to message - copy, we can do this efficiently in
        // the database.
        if ((ocopy.to instanceof VariableRef && ((VariableRef) ocopy.to)
                .isMessageRef())
                || (ocopy.from instanceof VariableRef && ((VariableRef) ocopy.from)
                .isMessageRef())) {

            if ((ocopy.to instanceof VariableRef && ((VariableRef) ocopy.to)
                    .isMessageRef())
                    && ocopy.from instanceof VariableRef
                    && ((VariableRef) ocopy.from).isMessageRef()) {

                final VariableInstance lval = _scopeFrame.resolve(ocopy.to
                        .getVariable());
                final VariableInstance rval = _scopeFrame
                        .resolve(((VariableRef) ocopy.from).getVariable());
                Element lvalue = (Element) napi.fetchVariableData(rval, false);
                napi.initializeVariable(lval, lvalue);
            } else {
                // This really should have been cought by the compiler.
                __log
                        .fatal("Message/Non-Message Assignment, should be caught by compiler:"
                                + ocopy);
                throw new FaultException(
                        ocopy.getOwner().constants.qnSelectionFailure,
                        "Message/Non-Message Assignment:  " + ocopy);
            }
        } else {
            // Conventional Assignment logic.
            Node rvalue = evalRValue(ocopy.from);
            Node lvalue = evalLValue(ocopy.to);
            if (__log.isDebugEnabled()) {
                __log.debug("lvalue after eval " + lvalue);
                if (lvalue != null) __log.debug("content " + DOMUtils.domToString(lvalue));
            }

            // Get a pointer within the lvalue.
            Node lvaluePtr = lvalue;

            if (ocopy.to instanceof OAssign.DirectRef) {
                DirectRef dref = ((DirectRef) ocopy.to);
                Element el = DOMUtils.findChildByName((Element)lvalue, dref.elName);
                if (el == null) {
                    el = (Element) ((Element)lvalue).appendChild(lvalue.getOwnerDocument()
                            .createElementNS(dref.elName.getNamespaceURI(), dref.elName.getLocalPart()));
                }
                lvaluePtr = el;
            } else if (ocopy.to instanceof OAssign.VariableRef) {
                VariableRef varRef = ((VariableRef) ocopy.to);
                lvaluePtr = evalQuery(
                        lvalue,
                        varRef.part,
                        varRef.location,
                        new EvaluationContextProxy(varRef.getVariable(), lvalue));
            } else if (ocopy.to instanceof OAssign.PropertyRef) {
                PropertyRef propRef = ((PropertyRef) ocopy.to);
                lvaluePtr = evalQuery(lvalue, propRef.propertyAlias.part,
                        propRef.propertyAlias.location,
                        new EvaluationContextProxy(propRef.getVariable(),
                                lvalue));
            } else if (ocopy.to instanceof OAssign.LValueExpression) {
                LValueExpression lexpr = (LValueExpression) ocopy.to;
                lvaluePtr = evalQuery(lvalue, null, lexpr.expression,
                        new EvaluationContextProxy(lexpr.getVariable(), lvalue));
                if (__log.isDebugEnabled())
                    __log.debug("lvaluePtr expr res " + lvaluePtr);
            }

            // For partner link assignmenent, the whole content is assigned.
            if (ocopy.to instanceof OAssign.PartnerLinkRef) {
                OAssign.PartnerLinkRef pLinkRef = ((OAssign.PartnerLinkRef) ocopy.to);
                PartnerLinkInstance plval = _scopeFrame
                        .resolve(pLinkRef.partnerLink);
                replaceEndpointRefence(plval, rvalue);
            } else {
                // Sneakily converting the EPR if it's not the format expected by the lvalue
                if (ocopy.from instanceof OAssign.PartnerLinkRef) {
                    rvalue = getBpelRuntimeContext().convertEndpointReference((Element)rvalue, lvaluePtr);
                    if (rvalue.getNodeType() == Node.DOCUMENT_NODE)
                        rvalue = ((Document)rvalue).getDocumentElement();
                }

                if (rvalue.getNodeType() == Node.ELEMENT_NODE
                        && lvaluePtr.getNodeType() == Node.ELEMENT_NODE) {
                    lvalue = replaceElement((Element)lvalue, (Element) lvaluePtr, (Element) rvalue,
                            ocopy.keepSrcElementName);
                } else {
                    lvalue = replaceContent(lvalue, lvaluePtr, rvalue
                            .getTextContent());
                }
                final VariableInstance lval = _scopeFrame.resolve(ocopy.to
                        .getVariable());
                if (__log.isDebugEnabled())
                    __log.debug("ASSIGN Writing variable '" + lval.declaration.name +
                                "' value '" + DOMUtils.domToString(lvalue) +"'");
                napi.commitChanges(lval, lvalue);
            }
        }

        ScopeEvent se;
        if (ocopy.to instanceof OAssign.PartnerLinkRef) {
            // myRole can't be updated, only a partnerRole is updated.
            se = new PartnerLinkModificationEvent(
                    ((OAssign.PartnerLinkRef) ocopy.to).partnerLink.getName());
        } else {
            final VariableInstance lval = _scopeFrame.resolve(ocopy.to
                    .getVariable());
            se = new VariableModificationEvent(lval.declaration.name);
        }
        if (ocopy.debugInfo != null)
            se.setLineNo(ocopy.debugInfo.startLine);
        sendEvent(se);
    }

    private void replaceEndpointRefence(PartnerLinkInstance plval, Node rvalue) throws FaultException {
        // Eventually wrapping with service-ref element if we've been directly assigned some
        // value that isn't wrapped.
        if (rvalue.getNodeType() == Node.TEXT_NODE ||
                (rvalue.getNodeType() == Node.ELEMENT_NODE && !rvalue.getLocalName().equals("service-ref"))) {
            Document doc = DOMUtils.newDocument();
            Element serviceRef = doc.createElementNS(Namespaces.WSBPEL2_0_FINAL_SERVREF, "service-ref");
            doc.appendChild(serviceRef);
            NodeList children = rvalue.getChildNodes();
            for (int m = 0; m < children.getLength(); m++) {
                Node child = children.item(m);
                serviceRef.appendChild(doc.importNode(child, true));
            }
            rvalue = serviceRef;
        }

        getBpelRuntimeContext().writeEndpointReference(plval, (Element)rvalue);
    }

    private Element replaceElement(Element lval, Element ptr, Element src,
                                boolean keepSrcElement) {
        Document doc = ptr.getOwnerDocument();
        Node parent = ptr.getParentNode();
        if (keepSrcElement) {
            Element replacement = (Element)doc.importNode(src, true);
            parent.replaceChild(replacement, ptr);
            return (lval == ptr) ? replacement :  lval;
        }

        Element replacement = doc.createElementNS(ptr.getNamespaceURI(), ptr
                .getLocalName());
        NodeList nl = src.getChildNodes();
        for (int i = 0; i < nl.getLength(); ++i)
            replacement.appendChild(doc.importNode(nl.item(i), true));
        NamedNodeMap attrs = src.getAttributes();
        for (int i = 0; i < attrs.getLength(); ++i)
            replacement.setAttributeNodeNS((Attr)doc.importNode(attrs.item(i), true));
        parent.replaceChild(replacement, ptr);
        return (lval == ptr) ? replacement :  lval;
    }

    /**
     * isInsert flag desginates this as an 'element' type insertion, which
     * requires insert the actual element value, rather than it's children
     *
     * @return
     * @throws FaultException
     */
    private Node replaceContent(Node lvalue, Node lvaluePtr, String rvalue)
            throws FaultException {
        Document d = lvaluePtr.getOwnerDocument();

        if (__log.isDebugEnabled()) {
            __log.debug("lvaluePtr type " + lvaluePtr.getNodeType());
            __log.debug("lvaluePtr " + DOMUtils.domToString(lvaluePtr));
            __log.debug("lvalue " + lvalue);
        }

        switch (lvaluePtr.getNodeType()) {
            case Node.ELEMENT_NODE:

                // Remove all the children.
                while (lvaluePtr.hasChildNodes())
                    lvaluePtr.removeChild(lvaluePtr.getFirstChild());

                // Append a new text node.
                lvaluePtr.appendChild(d.createTextNode(rvalue));

                // If lvalue is a text, removing all lvaluePtr children had just removed it
                // so we need to rebuild it as a child of lvaluePtr
                if (lvalue instanceof Text)
                    lvalue = lvaluePtr.getFirstChild();
                break;

            case Node.TEXT_NODE:

                Node newval = d.createTextNode(rvalue);
                // Replace ourselves .
                lvaluePtr.getParentNode().replaceChild(newval, lvaluePtr);

                // A little kludge, let our caller know that the root element has changed.
                // (used for assignment to a simple typed variable)
                if (lvalue.getNodeType() == Node.ELEMENT_NODE) {
                    // No children, adding an empty text children to point to
                    if (lvalue.getFirstChild() == null) {
                        Text txt = lvalue.getOwnerDocument().createTextNode("");
                        lvalue.appendChild(txt);
                    }
                    if (lvalue.getFirstChild().getNodeType() == Node.TEXT_NODE)
                        lvalue = lvalue.getFirstChild();
                }
                if (lvalue.getNodeType() == Node.TEXT_NODE && ((Text) lvalue).getWholeText().equals(
                        ((Text) lvaluePtr).getWholeText()))
                    lvalue = lvaluePtr = newval;
                break;

            case Node.ATTRIBUTE_NODE:

                ((Attr) lvaluePtr).setValue(rvalue);
                break;

            default:
                // This could occur if the expression language selects something
                // like
                // a PI or a CDATA.
                String msg = __msgs.msgInvalidLValue();
                if (__log.isDebugEnabled())
                    __log.debug(lvaluePtr + ": " + msg);
                throw new FaultException(
                        getOAsssign().getOwner().constants.qnSelectionFailure, msg);
        }

        return lvalue;
    }

    private Node evalQuery(Node lvalue, OMessageVarType.Part part,
                           OExpression expression, EvaluationContext ec) throws FaultException {
        assert lvalue != null;

        if (part != null) {
            QName partName = new QName(null, part.name);
            Node qualLVal = DOMUtils
                    .findChildByName((Element) lvalue, partName);
            if (part.type instanceof OElementVarType) {
                QName elName = ((OElementVarType) part.type).elementType;
                qualLVal = DOMUtils.findChildByName((Element) qualLVal, elName);
            }
            lvalue = qualLVal;
        }

        if (expression != null) {
            // Neat little trick....
            lvalue = ec.evaluateQuery(lvalue, expression);
        }

        return lvalue;
    }

    private class EvaluationContextProxy implements EvaluationContext {

        private Variable _var;

        private Node _varNode;

        private Node _rootNode;

        private EvaluationContext _ctx;

        private EvaluationContextProxy(Variable var, Node varNode) {
            _var = var;
            _varNode = varNode;
            _ctx = getEvaluationContext();

        }

        public Node readVariable(OScope.Variable variable, OMessageVarType.Part part) throws FaultException {
            if (variable.name.equals(_var.name)) {
                if (part == null) return _varNode;
                return _ctx.getPartData((Element)_varNode, part);

            } else
                return _ctx.readVariable(variable, part);

        }		/**
     * @see org.apache.ode.bpel.explang.EvaluationContext#readMessageProperty(org.apache.ode.bpel.o.OScope.Variable,
     *      org.apache.ode.bpel.o.OProcess.OProperty)
     */
    public String readMessageProperty(Variable variable, OProperty property)
            throws FaultException {
        return _ctx.readMessageProperty(variable, property);
    }

        /**
         * @see org.apache.ode.bpel.explang.EvaluationContext#isLinkActive(org.apache.ode.bpel.o.OLink)
         */
        public boolean isLinkActive(OLink olink) throws FaultException {
            return _ctx.isLinkActive(olink);
        }

        /**
         * @see org.apache.ode.bpel.explang.EvaluationContext#getRootNode()
         */
        public Node getRootNode() {
            return _rootNode;
        }

        /**
         * @see org.apache.ode.bpel.explang.EvaluationContext#evaluateQuery(org.w3c.dom.Node,
         *      org.apache.ode.bpel.o.OExpression)
         */
        public Node evaluateQuery(Node root, OExpression expr)
                throws FaultException {
            _rootNode = root;
            try {
                return getBpelRuntimeContext().getExpLangRuntime()
                        .evaluateNode(expr, this);
            } catch (org.apache.ode.bpel.explang.EvaluationException e) {
                throw new InvalidProcessException("Expression Failed: " + expr,
                        e);
            }
        }

        public Node getPartData(Element message, Part part) throws FaultException {
            return _ctx.getPartData(message,part);
        }

        public Long getProcessId() {
            return _ctx.getProcessId();
        }

        public boolean narrowTypes() {
            return false;
        }
    }

}
