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
package org.apache.ode.bpel.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.common.CorrelationKey;
import org.apache.ode.bpel.common.FaultException;
import org.apache.ode.bpel.common.ProcessState;
import org.apache.ode.bpel.dao.CorrelationSetDAO;
import org.apache.ode.bpel.dao.CorrelatorDAO;
import org.apache.ode.bpel.dao.MessageDAO;
import org.apache.ode.bpel.dao.MessageExchangeDAO;
import org.apache.ode.bpel.dao.MessageRouteDAO;
import org.apache.ode.bpel.dao.PartnerLinkDAO;
import org.apache.ode.bpel.dao.ProcessDAO;
import org.apache.ode.bpel.dao.ProcessInstanceDAO;
import org.apache.ode.bpel.dao.ScopeDAO;
import org.apache.ode.bpel.dao.XmlDataDAO;
import org.apache.ode.bpel.evt.CorrelationSetWriteEvent;
import org.apache.ode.bpel.evt.ProcessCompletionEvent;
import org.apache.ode.bpel.evt.ProcessInstanceEvent;
import org.apache.ode.bpel.evt.ProcessInstanceStateChangeEvent;
import org.apache.ode.bpel.evt.ProcessMessageExchangeEvent;
import org.apache.ode.bpel.evt.ProcessTerminationEvent;
import org.apache.ode.bpel.iapi.BpelEngineException;
import org.apache.ode.bpel.iapi.ContextException;
import org.apache.ode.bpel.iapi.Endpoint;
import org.apache.ode.bpel.iapi.EndpointReference;
import org.apache.ode.bpel.iapi.Message;
import org.apache.ode.bpel.iapi.MessageExchange;
import org.apache.ode.bpel.iapi.MessageExchange.FailureType;
import org.apache.ode.bpel.iapi.MessageExchange.MessageExchangePattern;
import org.apache.ode.bpel.iapi.MyRoleMessageExchange;
import org.apache.ode.bpel.iapi.PartnerRoleMessageExchange;
import org.apache.ode.bpel.memdao.ProcessInstanceDaoImpl;
import org.apache.ode.bpel.o.OMessageVarType;
import org.apache.ode.bpel.o.OMessageVarType.Part;
import org.apache.ode.bpel.o.OPartnerLink;
import org.apache.ode.bpel.o.OProcess;
import org.apache.ode.bpel.o.OScope;
import org.apache.ode.bpel.runtime.BpelJacobRunnable;
import org.apache.ode.bpel.runtime.BpelRuntimeContext;
import org.apache.ode.bpel.runtime.CorrelationSetInstance;
import org.apache.ode.bpel.runtime.ExpressionLanguageRuntimeRegistry;
import org.apache.ode.bpel.runtime.PROCESS;
import org.apache.ode.bpel.runtime.PartnerLinkInstance;
import org.apache.ode.bpel.runtime.Selector;
import org.apache.ode.bpel.runtime.VariableInstance;
import org.apache.ode.bpel.runtime.channels.ActivityRecoveryChannel;
import org.apache.ode.bpel.runtime.channels.FaultData;
import org.apache.ode.bpel.runtime.channels.InvokeResponseChannel;
import org.apache.ode.bpel.runtime.channels.PickResponseChannel;
import org.apache.ode.bpel.runtime.channels.TimerResponseChannel;
import org.apache.ode.jacob.JacobRunnable;
import org.apache.ode.jacob.vpu.ExecutionQueueImpl;
import org.apache.ode.jacob.vpu.JacobVPU;
import org.apache.ode.utils.DOMUtils;
import org.apache.ode.utils.GUID;
import org.apache.ode.utils.Namespaces;
import org.apache.ode.utils.ObjectPrinter;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.wsdl.Operation;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

class BpelRuntimeContextImpl implements BpelRuntimeContext {

    private static final Log __log = LogFactory.getLog(BpelRuntimeContextImpl.class);

    /** Data-access object for process instance. */
    private ProcessInstanceDAO _dao;

    /** Process Instance ID */
    private final Long _iid;

    /** JACOB VPU */
    protected JacobVPU _vpu;

    /** JACOB ExecutionQueue (state) */
    protected ExecutionQueueImpl _soup;

    private MyRoleMessageExchangeImpl _instantiatingMessageExchange;

    private OutstandingRequestManager _outstandingRequests;

    private BpelProcess _bpelProcess;

    /** Five second maximum for continous execution. */
    private long _maxReductionTimeMs = 2000000;

    public BpelRuntimeContextImpl(BpelProcess bpelProcess, ProcessInstanceDAO dao, PROCESS PROCESS,
                                  MyRoleMessageExchangeImpl instantiatingMessageExchange) {
        _bpelProcess = bpelProcess;
        _dao = dao;
        _iid = dao.getInstanceId();
        _instantiatingMessageExchange = instantiatingMessageExchange;
        _vpu = new JacobVPU();
        _vpu.registerExtension(BpelRuntimeContext.class, this);

        _soup = new ExecutionQueueImpl(null);
        _soup.setReplacementMap(_bpelProcess.getReplacementMap());
        _outstandingRequests = new OutstandingRequestManager();
        _vpu.setContext(_soup);

        if (bpelProcess.isInMemory()) {
            ProcessInstanceDaoImpl inmem = (ProcessInstanceDaoImpl) _dao;
            if (inmem.getSoup() != null) {
                _soup = (ExecutionQueueImpl) inmem.getSoup();
                _outstandingRequests = (OutstandingRequestManager) _soup.getGlobalData();
                _vpu.setContext(_soup);
            }
        } else {
            byte[] daoState = dao.getExecutionState();
            if (daoState != null) {
                ByteArrayInputStream iis = new ByteArrayInputStream(daoState);
                try {
                    _soup.read(iis);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                _outstandingRequests = (OutstandingRequestManager) _soup.getGlobalData();
            }
        }

        if (PROCESS != null) {
            _vpu.inject(PROCESS);
        }

        if (BpelProcess.__log.isDebugEnabled()) {
            __log.debug("BpelRuntimeContextImpl created for instance " + _iid + ". INDEXED STATE=" + _soup.getIndex());
        }
    }

    public Long getPid() {
        return _iid;
    }

    public long genId() {
        return _dao.genMonotonic();
    }

    /**
     * @see BpelRuntimeContext#isCorrelationInitialized(org.apache.ode.bpel.runtime.CorrelationSetInstance)
     */
    public boolean isCorrelationInitialized(CorrelationSetInstance correlationSet) {
        ScopeDAO scopeDAO = _dao.getScope(correlationSet.scopeInstance);
        CorrelationSetDAO cs = scopeDAO.getCorrelationSet(correlationSet.declaration.name);

        return cs.getValue() != null;
    }

    /**
     * @see BpelRuntimeContext#isVariableInitialized(org.apache.ode.bpel.runtime.VariableInstance)
     */
    public boolean isVariableInitialized(VariableInstance var) {
        ScopeDAO scopeDAO = _dao.getScope(var.scopeInstance);
        XmlDataDAO dataDAO = scopeDAO.getVariable(var.declaration.name);
        return !dataDAO.isNull();
    }

    public boolean isPartnerRoleEndpointInitialized(PartnerLinkInstance pLink) {
        PartnerLinkDAO spl = fetchPartnerLinkDAO(pLink);

        return spl.getPartnerEPR() != null || _bpelProcess.getInitialPartnerRoleEPR(pLink.partnerLink) != null;
    }

    /**
     * @see BpelRuntimeContext#completedFault(org.apache.ode.bpel.runtime.channels.FaultData)
     */
    public void completedFault(FaultData faultData) {
        if (BpelProcess.__log.isDebugEnabled()) {
            BpelProcess.__log.debug("ProcessImpl completed with fault '" + faultData.getFaultName() + "'");
        }

        _dao.setFault(faultData.getFaultName(), faultData.getExplanation(), faultData.getFaultLineNo(), faultData
                .getActivityId(), faultData.getFaultMessage());

        // send event
        ProcessInstanceStateChangeEvent evt = new ProcessInstanceStateChangeEvent();
        evt.setOldState(_dao.getState());
        _dao.setState(ProcessState.STATE_COMPLETED_WITH_FAULT);
        evt.setNewState(ProcessState.STATE_COMPLETED_WITH_FAULT);
        sendEvent(evt);

        sendEvent(new ProcessCompletionEvent(faultData.getFaultName()));
        _dao.finishCompletion();

        faultOutstandingMessageExchanges(faultData);
    }

    /**
     * @see BpelRuntimeContext#completedOk()
     */
    public void completedOk() {
        if (BpelProcess.__log.isDebugEnabled()) {
            BpelProcess.__log.debug("ProcessImpl " + _bpelProcess.getPID() + " completed OK.");
        }

        // send event
        ProcessInstanceStateChangeEvent evt = new ProcessInstanceStateChangeEvent();
        evt.setOldState(_dao.getState());
        _dao.setState(ProcessState.STATE_COMPLETED_OK);
        evt.setNewState(ProcessState.STATE_COMPLETED_OK);
        sendEvent(evt);

        sendEvent(new ProcessCompletionEvent(null));
        _dao.finishCompletion();

        completeOutstandingMessageExchanges();
    }

    /**
     * @see BpelRuntimeContext#createScopeInstance(Long,
     *      org.apache.ode.bpel.o.OScope)
     */
    public Long createScopeInstance(Long parentScopeId, OScope scope) {
        if (BpelProcess.__log.isTraceEnabled()) {
            BpelProcess.__log.trace(ObjectPrinter.stringifyMethodEnter("createScopeInstance", new Object[] {
                    "parentScopeId", parentScopeId, "scope", scope }));
        }

        ScopeDAO parent = null;

        if (parentScopeId != null) {
            parent = _dao.getScope(parentScopeId);
        }

        ScopeDAO scopeDao = _dao.createScope(parent, scope.name, scope.getId());
        return scopeDao.getScopeInstanceId();
    }

    public void initializePartnerLinks(Long parentScopeId, Collection<OPartnerLink> partnerLinks) {

        if (BpelProcess.__log.isTraceEnabled()) {
            BpelProcess.__log.trace(ObjectPrinter.stringifyMethodEnter("initializeEndpointReferences", new Object[] {
                    "parentScopeId", parentScopeId, "partnerLinks", partnerLinks }));
        }

        ScopeDAO parent = _dao.getScope(parentScopeId);
        for (OPartnerLink partnerLink : partnerLinks) {
            PartnerLinkDAO pdao = parent.createPartnerLink(partnerLink.getId(), partnerLink.name,
                    partnerLink.myRoleName, partnerLink.partnerRoleName);
            // If there is a myrole on the link, initialize the session id so it
            // is always
            // available for opaque correlations. The myrole session id should
            // never be changed.
            if (partnerLink.hasMyRole())
                pdao.setMySessionId(new GUID().toString());
        }
    }

    public void select(PickResponseChannel pickResponseChannel, Date timeout, boolean createInstance,
                       Selector[] selectors) throws FaultException {
        if (BpelProcess.__log.isTraceEnabled())
            BpelProcess.__log.trace(ObjectPrinter.stringifyMethodEnter("select", new Object[] { "pickResponseChannel",
                    pickResponseChannel, "timeout", timeout, "createInstance", createInstance,
                    "selectors", selectors }));

        ProcessDAO processDao = _dao.getProcess();

        // check if this is first pick
        if (_dao.getState() == ProcessState.STATE_NEW) {
            assert createInstance;
            // send event
            ProcessInstanceStateChangeEvent evt = new ProcessInstanceStateChangeEvent();
            evt.setOldState(ProcessState.STATE_NEW);
            _dao.setState(ProcessState.STATE_READY);
            evt.setNewState(ProcessState.STATE_READY);
            sendEvent(evt);
        }

        final String pickResponseChannelStr = pickResponseChannel.export();

        List<CorrelatorDAO> correlators = new ArrayList<CorrelatorDAO>(selectors.length);
        for (Selector selector : selectors) {
            String correlatorId = BpelProcess.genCorrelatorId(selector.plinkInstance.partnerLink, selector.opName);
            if (BpelProcess.__log.isDebugEnabled()) {
                BpelProcess.__log.debug("SELECT: " + pickResponseChannel + ": USING CORRELATOR " + correlatorId);
            }
            correlators.add(processDao.getCorrelator(correlatorId));
        }

        int conflict = _outstandingRequests.findConflict(selectors);
        if (conflict != -1)
            throw new FaultException(_bpelProcess.getOProcess().constants.qnConflictingReceive, selectors[conflict]
                    .toString());

        _outstandingRequests.register(pickResponseChannelStr, selectors);

        // TODO - ODE-58

        // First check if we match to a new instance.
        if (_instantiatingMessageExchange != null && _dao.getState() == ProcessState.STATE_READY) {
            if (BpelProcess.__log.isDebugEnabled()) {
                BpelProcess.__log.debug("SELECT: " + pickResponseChannel + ": CHECKING for NEW INSTANCE match");
            }
            for (int i = 0; i < correlators.size(); ++i) {
                CorrelatorDAO ci = correlators.get(i);
                if (ci.equals(_dao.getInstantiatingCorrelator())) {
                    inputMsgMatch(pickResponseChannelStr, i, _instantiatingMessageExchange);
                    if (BpelProcess.__log.isDebugEnabled()) {
                        BpelProcess.__log.debug("SELECT: " + pickResponseChannel
                                + ": FOUND match for NEW instance mexRef=" + _instantiatingMessageExchange);
                    }
                    return;
                }
            }
        }

        // if (BpelProcess.__log.isDebugEnabled()) {
        // BpelProcess.__log.debug("SELECT: " + pickResponseChannel
        // + ": NEW instance match NOT FOUND; CHECKING MESSAGES. ");
        // }
        //
        //
        // for (int i = 0; i < selectors.length; ++i) {
        // CorrelatorDAO correlator = correlators.get(i);
        // Selector selector = selectors[i];
        // MessageExchangeDAO mexdao = correlator
        // .dequeueMessage(selector.correlationKey);
        // if (mexdao != null) {
        // // Found message matching one of our selectors.
        // if (BpelProcess.__log.isDebugEnabled()) {
        // BpelProcess.__log.debug("SELECT: " + pickResponseChannel
        // + ": FOUND match to MESSAGE " + mexdao + " on CKEY "
        // + selector.correlationKey);
        // }
        //
        // MyRoleMessageExchangeImpl mex = new MyRoleMessageExchangeImpl(
        // _bpelProcess._engine, mexdao);
        //
        // inputMsgMatch(pickResponseChannel.export(), i, mex);
        // return;
        // }
        // }
        //
        // if (BpelProcess.__log.isDebugEnabled()) {
        // BpelProcess.__log.debug("SELECT: " + pickResponseChannel
        // + ": MESSAGE match NOT FOUND.");
        // }

        if (timeout != null) {
            registerTimer(pickResponseChannel, timeout);
            if (BpelProcess.__log.isDebugEnabled()) {
                BpelProcess.__log.debug("SELECT: " + pickResponseChannel + "REGISTERED TIMEOUT for " + timeout);
            }
        }

        for (int i = 0; i < selectors.length; ++i) {
            CorrelatorDAO correlator = correlators.get(i);
            Selector selector = selectors[i];

            correlator.addRoute(pickResponseChannel.export(), _dao, i, selector.correlationKey);
            scheduleCorrelatorMatcher(correlator.getCorrelatorId(), selector.correlationKey);

            if (BpelProcess.__log.isDebugEnabled()) {
                BpelProcess.__log.debug("SELECT: " + pickResponseChannel + ": ADDED ROUTE " + correlator.getCorrelatorId() + ": "
                        + selector.correlationKey + " --> " + _dao.getInstanceId());
            }
        }


    }

    /**
     * @see BpelRuntimeContext#readCorrelation(org.apache.ode.bpel.runtime.CorrelationSetInstance)
     */
    public CorrelationKey readCorrelation(CorrelationSetInstance cset) {
        ScopeDAO scopeDAO = _dao.getScope(cset.scopeInstance);
        CorrelationSetDAO cs = scopeDAO.getCorrelationSet(cset.declaration.name);
        return cs.getValue();
    }

    public Node fetchVariableData(VariableInstance variable, boolean forWriting) throws FaultException {
        ScopeDAO scopeDAO = _dao.getScope(variable.scopeInstance);
        XmlDataDAO dataDAO = scopeDAO.getVariable(variable.declaration.name);

        if (dataDAO.isNull()) {
            throw new FaultException(_bpelProcess.getOProcess().constants.qnUninitializedVariable,
                    "The variable " + variable.declaration.name + " isn't properly initialized.");
        }

        return dataDAO.get();
    }

    public Node fetchVariableData(VariableInstance var, OMessageVarType.Part part, boolean forWriting)
            throws FaultException {
        Node container = fetchVariableData(var, forWriting);

        // If we want a specific part, we will need to navigate through the
        // message/part structure
        if (var.declaration.type instanceof OMessageVarType && part != null) {
            container = getPartData((Element) container, part);
        }
        return container;
    }

    public Element fetchPartnerRoleEndpointReferenceData(PartnerLinkInstance pLink) throws FaultException {
        PartnerLinkDAO pl = fetchPartnerLinkDAO(pLink);
        Element epr = pl.getPartnerEPR();

        if (epr == null) {
            EndpointReference e = _bpelProcess.getInitialPartnerRoleEPR(pLink.partnerLink);
            if (e != null)
                epr = e.toXML().getDocumentElement();
        }

        if (epr == null) {
            throw new FaultException(_bpelProcess.getOProcess().constants.qnUninitializedPartnerRole);
        }

        return epr;
    }

    public Element fetchMyRoleEndpointReferenceData(PartnerLinkInstance pLink) {
        return _bpelProcess.getInitialMyRoleEPR(pLink.partnerLink).toXML().getDocumentElement();
    }

    private PartnerLinkDAO fetchPartnerLinkDAO(PartnerLinkInstance pLink) {
        ScopeDAO scopeDAO = _dao.getScope(pLink.scopeInstanceId);
        return scopeDAO.getPartnerLink(pLink.partnerLink.getId());
    }

    /**
     * Evaluate a property alias query expression against a variable, returning
     * the normalized {@link String} representation of the property value.
     *
     * @param variable
     *            variable to read
     * @param property
     *            property to read
     * @return value of property for variable, in String form
     * @throws org.apache.ode.bpel.common.FaultException
     *             in case of selection or other fault
     */
    public String readProperty(VariableInstance variable, OProcess.OProperty property) throws FaultException {
        Node varData = fetchVariableData(variable, false);

        OProcess.OPropertyAlias alias = property.getAlias(variable.declaration.type);
        String val = _bpelProcess.extractProperty((Element) varData, alias, variable.declaration.getDescription());

        if (BpelProcess.__log.isTraceEnabled()) {
            BpelProcess.__log.trace("readPropertyAlias(variable=" + variable + ", alias=" + alias + ") = "
                    + val.toString());
        }

        return val;
    }

    public Node initializeVariable(VariableInstance variable, Node initData) {
        ScopeDAO scopeDAO = _dao.getScope(variable.scopeInstance);
        XmlDataDAO dataDAO = scopeDAO.getVariable(variable.declaration.name);

        dataDAO.set(initData);

        writeProperties(variable, initData, dataDAO);

        return dataDAO.get();
    }

    public void writeEndpointReference(PartnerLinkInstance variable, Element data) throws FaultException {
        if (__log.isDebugEnabled()) {
            __log.debug("Writing endpoint reference " + variable.partnerLink.getName() + " with value "
                    + DOMUtils.domToString(data));
        }

        PartnerLinkDAO eprDAO = fetchPartnerLinkDAO(variable);
        eprDAO.setPartnerEPR(data);
    }

    public String fetchEndpointSessionId(PartnerLinkInstance pLink, boolean isMyEPR) throws FaultException {
        PartnerLinkDAO dao = fetchPartnerLinkDAO(pLink);
        return isMyEPR ? dao.getMySessionId() : dao.getPartnerSessionId();
    }

    public Node convertEndpointReference(Element sourceNode, Node targetNode) {
        QName nodeQName;
        if (targetNode.getNodeType() == Node.TEXT_NODE) {
            nodeQName = new QName(Namespaces.XML_SCHEMA, "string");
        } else {
            // We have an element
            nodeQName = new QName(targetNode.getNamespaceURI(), targetNode.getLocalName());
        }
        return _bpelProcess._engine._contexts.eprContext.convertEndpoint(nodeQName, sourceNode).toXML();
    }

    public void commitChanges(VariableInstance variable, Node changes) {
        ScopeDAO scopeDAO = _dao.getScope(variable.scopeInstance);
        XmlDataDAO dataDAO = scopeDAO.getVariable(variable.declaration.name);
        dataDAO.set(changes);

        writeProperties(variable, changes, dataDAO);
    }

    public void reply(final PartnerLinkInstance plinkInstnace, final String opName, final String mexId, Element msg,
                      QName fault) throws FaultException {
        String mexRef = _outstandingRequests.release(plinkInstnace, opName, mexId);

        if (mexRef == null) {
            throw new FaultException(_bpelProcess.getOProcess().constants.qnMissingRequest);
        }

        // prepare event
        ProcessMessageExchangeEvent evt = new ProcessMessageExchangeEvent();
        evt.setMexId(mexId);
        evt.setOperation(opName);
        evt.setPortType(plinkInstnace.partnerLink.myRolePortType.getQName());

        MessageExchangeDAO mex = _dao.getConnection().getMessageExchange(mexRef);

        MessageDAO message = mex.createMessage(plinkInstnace.partnerLink.getMyRoleOperation(opName).getOutput()
                .getMessage().getQName());
        message.setData(msg);

        MyRoleMessageExchangeImpl m = new MyRoleMessageExchangeImpl(_bpelProcess._engine, mex);
        _bpelProcess.initMyRoleMex(m);
        m.setResponse(new MessageImpl(message));

        if (fault != null) {
            mex.setStatus(MessageExchange.Status.FAULT.toString());
            mex.setFault(fault);
            evt.setAspect(ProcessMessageExchangeEvent.PROCESS_FAULT);
        } else {
            mex.setStatus(MessageExchange.Status.RESPONSE.toString());
            evt.setAspect(ProcessMessageExchangeEvent.PROCESS_OUTPUT);
        }

        if (mex.getPipedMessageExchangeId() != null) {
            PartnerRoleMessageExchange pmex = (PartnerRoleMessageExchange) _bpelProcess
                    .getEngine().getMessageExchange(mex.getPipedMessageExchangeId());
            if (BpelProcess.__log.isDebugEnabled()) {
                __log.debug("Replying to a p2p mex, myrole " + m + " - partnerole " + pmex);
            }
            try {
                switch (m.getStatus()) {
                    case FAILURE:
                        // We can't seem to get the failure out of the myrole mex?
                        pmex.replyWithFailure(MessageExchange.FailureType.OTHER, "operation failed", null);
                        break;
                    case FAULT:
                        Message faultRes = pmex.createMessage(pmex.getOperation().getFault(m.getFault().getLocalPart())
                                .getMessage().getQName());
                        faultRes.setMessage(m.getResponse().getMessage());
                        pmex.replyWithFault(m.getFault(), faultRes);
                        break;
                    case RESPONSE:
                        Message response = pmex.createMessage(pmex.getOperation().getOutput().getMessage().getQName());
                        response.setMessage(m.getResponse().getMessage());
                        pmex.reply(response);
                        break;
                    default:
                        __log.warn("Unexpected state: " + m.getStatus());
                        break;
                }
            } finally {
                mex.release();
            }
        } else _bpelProcess._engine._contexts.mexContext.onAsyncReply(m);

        // send event
        sendEvent(evt);
    }

    /**
     * @see BpelRuntimeContext#writeCorrelation(org.apache.ode.bpel.runtime.CorrelationSetInstance,
     *      org.apache.ode.bpel.common.CorrelationKey)
     */
    public void writeCorrelation(CorrelationSetInstance cset, CorrelationKey correlation) {
        ScopeDAO scopeDAO = _dao.getScope(cset.scopeInstance);
        CorrelationSetDAO cs = scopeDAO.getCorrelationSet(cset.declaration.name);
        OScope.CorrelationSet csetdef = (OScope.CorrelationSet) _bpelProcess.getOProcess()
                .getChild(correlation.getCSetId());
        QName[] propNames = new QName[csetdef.properties.size()];
        for (int m = 0; m < csetdef.properties.size(); m++) {
            OProcess.OProperty oProperty = csetdef.properties.get(m);
            propNames[m] = oProperty.name;
        }
        cs.setValue(propNames, correlation);

        CorrelationSetWriteEvent cswe = new CorrelationSetWriteEvent(cset.declaration.name, correlation);
        cswe.setScopeId(cset.scopeInstance);
        sendEvent(cswe);

    }

    /**
     * Common functionality to initialize a correlation set based on data
     * available in a variable.
     *
     * @param cset
     *            the correlation set instance
     * @param variable
     *            variable instance
     *
     * @throws IllegalStateException
     *             DOCUMENTME
     */
    public void initializeCorrelation(CorrelationSetInstance cset, VariableInstance variable) throws FaultException {
        if (BpelProcess.__log.isDebugEnabled()) {
            BpelProcess.__log.debug("Initializing correlation set " + cset.declaration.name);
        }
        // if correlation set is already initialized, then skip
        if (isCorrelationInitialized(cset)) {
            // if already set, we ignore
            if (BpelProcess.__log.isDebugEnabled()) {
                BpelProcess.__log.debug("OCorrelation set " + cset + " is already set: ignoring");
            }
            return;
        }

        String[] propNames = new String[cset.declaration.properties.size()];
        String[] propValues = new String[cset.declaration.properties.size()];

        for (int i = 0; i < cset.declaration.properties.size(); ++i) {
            OProcess.OProperty property = cset.declaration.properties.get(i);
            propValues[i] = readProperty(variable, property);
            propNames[i] = property.name.toString();
        }

        CorrelationKey ckeyVal = new CorrelationKey(cset.declaration.getId(), propValues);
        writeCorrelation(cset, ckeyVal);
    }

    public ExpressionLanguageRuntimeRegistry getExpLangRuntime() {
        return _bpelProcess._expLangRuntimeRegistry;
    }

    /**
     * @see BpelRuntimeContext#terminate()
     */
    public void terminate() {
        // send event
        ProcessInstanceStateChangeEvent evt = new ProcessInstanceStateChangeEvent();
        evt.setOldState(_dao.getState());
        _dao.setState(ProcessState.STATE_TERMINATED);
        evt.setNewState(ProcessState.STATE_TERMINATED);
        sendEvent(evt);
        sendEvent(new ProcessTerminationEvent());

        failOutstandingMessageExchanges();
    }

    public void registerTimer(TimerResponseChannel timerChannel, Date timeToFire) {
        WorkEvent we = new WorkEvent();
        we.setIID(_dao.getInstanceId());
        we.setChannel(timerChannel.export());
        we.setType(WorkEvent.Type.TIMER);
        we.setInMem(_bpelProcess.isInMemory());
        _bpelProcess._engine._contexts.scheduler.schedulePersistedJob(we.getDetail(), timeToFire);
    }

    private void scheduleCorrelatorMatcher(String correlatorId, CorrelationKey key) {
        WorkEvent we = new WorkEvent();
        we.setIID(_dao.getInstanceId());
        we.setType(WorkEvent.Type.MATCHER);
        we.setCorrelatorId(correlatorId);
        we.setCorrelationKey(key);
        we.setInMem(_bpelProcess.isInMemory());
        _bpelProcess._engine._contexts.scheduler.scheduleVolatileJob(true, we.getDetail());
    }

    public String invoke(PartnerLinkInstance partnerLink, Operation operation, Element outgoingMessage,
                         InvokeResponseChannel channel) throws FaultException {

        PartnerLinkDAO plinkDAO = fetchPartnerLinkDAO(partnerLink);
        // The target (partner endpoint) -- if it has not been explicitly
        // initialized
        // then use the value from bthe deployment descriptor ..
        Element partnerEPR = plinkDAO.getPartnerEPR();
        EndpointReference partnerEpr;

        if (partnerEPR == null) {
            partnerEpr = _bpelProcess.getInitialPartnerRoleEPR(partnerLink.partnerLink);
            // In this case, the partner link has not been initialized.
            if (partnerEpr == null)
                throw new FaultException(partnerLink.partnerLink.getOwner().constants.qnUninitializedPartnerRole);
        } else {
            partnerEpr = _bpelProcess._engine._contexts.eprContext.resolveEndpointReference(partnerEPR);
        }

        if (BpelProcess.__log.isDebugEnabled()) {
            BpelProcess.__log.debug("INVOKING PARTNER: partnerLink=" + partnerLink +
                    ", op=" + operation.getName() + " channel=" + channel + ")");
        }

        // prepare event
        ProcessMessageExchangeEvent evt = new ProcessMessageExchangeEvent();
        evt.setOperation(operation.getName());
        evt.setPortType(partnerLink.partnerLink.partnerRolePortType.getQName());
        evt.setAspect(ProcessMessageExchangeEvent.PARTNER_INPUT);

        MessageExchangeDAO mexDao = _dao.getConnection().createMessageExchange(
                MessageExchangeDAO.DIR_BPEL_INVOKES_PARTNERROLE);
        mexDao.setStatus(MessageExchange.Status.NEW.toString());
        mexDao.setOperation(operation.getName());
        mexDao.setPortType(partnerLink.partnerLink.partnerRolePortType.getQName());
        mexDao.setPartnerLinkModelId(partnerLink.partnerLink.getId());
        mexDao.setPartnerLink(plinkDAO);
        mexDao.setProcess(_dao.getProcess());
        mexDao.setInstance(_dao);
        mexDao.setPattern((operation.getOutput() != null ? MessageExchangePattern.REQUEST_RESPONSE
                : MessageExchangePattern.REQUEST_ONLY).toString());
        mexDao.setChannel(channel == null ? null : channel.export());

        // Properties used by stateful-exchange protocol.
        String mySessionId = plinkDAO.getMySessionId();
        String partnerSessionId = plinkDAO.getPartnerSessionId();

        if ( mySessionId != null )
            mexDao.setProperty(MessageExchange.PROPERTY_SEP_MYROLE_SESSIONID, mySessionId);
        if ( partnerSessionId != null )
            mexDao.setProperty(MessageExchange.PROPERTY_SEP_PARTNERROLE_SESSIONID, partnerSessionId);

        if (__log.isDebugEnabled())
            __log.debug("INVOKE PARTNER (SEP): sessionId=" + mySessionId + " partnerSessionId=" + partnerSessionId);

        MessageDAO message = mexDao.createMessage(operation.getInput().getMessage().getQName());
        mexDao.setRequest(message);
        message.setData(outgoingMessage);
        message.setType(operation.getInput().getMessage().getQName());

        // Get he my-role EPR (if myrole exists) for optional use by partner
        // (for callback mechanism).
        EndpointReference myRoleEndpoint = partnerLink.partnerLink.hasMyRole() ? _bpelProcess
                .getInitialMyRoleEPR(partnerLink.partnerLink) : null;
        PartnerRoleMessageExchangeImpl mex = new PartnerRoleMessageExchangeImpl(_bpelProcess._engine, mexDao,
                partnerLink.partnerLink.partnerRolePortType, operation, partnerEpr, myRoleEndpoint, _bpelProcess
                .getPartnerRoleChannel(partnerLink.partnerLink));

        BpelProcess p2pProcess = null;
        Endpoint partnerEndpoint = _bpelProcess.getInitialPartnerRoleEndpoint(partnerLink.partnerLink);
        if (partnerEndpoint != null)
            p2pProcess = _bpelProcess.getEngine().route(partnerEndpoint.serviceName, mex.getRequest());

        if (p2pProcess != null) {
            // Creating a my mex using the same message id as partner mex to "pipe" them
            MyRoleMessageExchange myRoleMex = _bpelProcess.getEngine().createMessageExchange(
                    mex.getMessageExchangeId(), partnerEndpoint.serviceName,
                    operation.getName(), mex.getMessageExchangeId());

            if (BpelProcess.__log.isDebugEnabled()) {
                __log.debug("Invoking in a p2p interaction, partnerrole " + mex + " - myrole " + myRoleMex);
            }

            Message odeRequest = myRoleMex.createMessage(operation.getInput().getMessage().getQName());
            odeRequest.setMessage(outgoingMessage);

            if (BpelProcess.__log.isDebugEnabled()) {
                __log.debug("Setting myRoleMex session ids for p2p interaction, mySession "
                        + partnerSessionId + " - partnerSess " + mySessionId);
            }
            if ( partnerSessionId != null )
                myRoleMex.setProperty(MessageExchange.PROPERTY_SEP_MYROLE_SESSIONID, partnerSessionId);
            if ( mySessionId != null )
                myRoleMex.setProperty(MessageExchange.PROPERTY_SEP_PARTNERROLE_SESSIONID, mySessionId);

            mex.setStatus(MessageExchange.Status.REQUEST);
            myRoleMex.invoke(odeRequest);

            // Can't expect any sync response
            mex.replyAsync();
        } else {
            // If we couldn't find the endpoint, then there is no sense
            // in asking the IL to invoke.
            if (partnerEpr != null) {
                mexDao.setEPR(partnerEpr.toXML().getDocumentElement());
                mex.setStatus(MessageExchange.Status.REQUEST);
                _bpelProcess._engine._contexts.mexContext.invokePartner(mex);
            } else {
                __log.error("Couldn't find endpoint for partner EPR " + DOMUtils.domToString(partnerEPR));
                mex.setFailure(FailureType.UNKNOWN_ENDPOINT, "UnknownEndpoint", partnerEPR);
            }
        }

        evt.setMexId(mexDao.getMessageExchangeId());
        sendEvent(evt);

        // MEX pattern is request only, at this point the status can only be a one way
        if (mexDao.getPattern().equals(MessageExchangePattern.REQUEST_ONLY.toString())) {
            mexDao.setStatus(MessageExchange.Status.ASYNC.toString());
            // This mex can now be released
            mexDao.release();
        }
        // Check if there is a synchronous response, if so, we need to inject the
        // message on the response channel.
        switch (mex.getStatus()) {
            case NEW:
                throw new AssertionError("Impossible!");
            case ASYNC:
                break;
            case RESPONSE:
            case FAULT:
            case FAILURE:
                invocationResponse(mex);
                break;
            default:
                __log.error("Partner did not acknowledge message exchange: " + mex);
                mex.setFailure(FailureType.NO_RESPONSE, "Partner did not acknowledge.", null);
                invocationResponse(mex);
        }

        return mexDao.getMessageExchangeId();

    }

    void execute() {
        long maxTime = System.currentTimeMillis() + _maxReductionTimeMs;
        boolean canReduce = true;
        while (ProcessState.canExecute(_dao.getState()) && System.currentTimeMillis() < maxTime && canReduce) {
            canReduce = _vpu.execute();
        }
        _dao.setLastActiveTime(new Date());
        if (!ProcessState.isFinished(_dao.getState())) {
            if (__log.isDebugEnabled()) __log.debug("Setting execution state on instance " + _iid);
            _soup.setGlobalData(_outstandingRequests);

            if (_bpelProcess.isInMemory()) {
                // don't serialize in-memory processes
                ((ProcessInstanceDaoImpl) _dao).setSoup(_soup);
            } else {
                ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
                try {
                    _soup.write(bos);
                    bos.close();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                _dao.setExecutionState(bos.toByteArray());
            }

            if (ProcessState.canExecute(_dao.getState()) && canReduce) {
                // Max time exceeded (possibly an infinite loop).
                if (__log.isDebugEnabled())
                    __log.debug("MaxTime exceeded for instance # " + _iid);
                try {
                    WorkEvent we = new WorkEvent();
                    we.setIID(_iid);
                    we.setType(WorkEvent.Type.RESUME);
                    we.setInMem(_bpelProcess.isInMemory());
                    if (_bpelProcess.isInMemory())
                        _bpelProcess._engine._contexts.scheduler.scheduleVolatileJob(true, we.getDetail());
                    else
                        _bpelProcess._engine._contexts.scheduler.schedulePersistedJob(we.getDetail(), new Date());
                } catch (ContextException e) {
                    __log.error("Failed to schedule resume task.", e);
                    throw new BpelEngineException(e);
                }
            }
        }
    }

    void inputMsgMatch(final String responsechannel, final int idx, MyRoleMessageExchangeImpl mex) {
        // if we have a message match, this instance should be marked
        // active if it isn't already
        if (_dao.getState() == ProcessState.STATE_READY) {
            if (BpelProcess.__log.isDebugEnabled()) {
                BpelProcess.__log.debug("INPUTMSGMATCH: Changing process instance state from ready to active");
            }

            _dao.setState(ProcessState.STATE_ACTIVE);

            // send event
            ProcessInstanceStateChangeEvent evt = new ProcessInstanceStateChangeEvent();
            evt.setOldState(ProcessState.STATE_READY);
            evt.setNewState(ProcessState.STATE_ACTIVE);
            sendEvent(evt);
        }

        _outstandingRequests.associate(responsechannel, mex.getMessageExchangeId());

        final String mexId = mex.getMessageExchangeId();
        _vpu.inject(new JacobRunnable() {
            private static final long serialVersionUID = 3168964409165899533L;

            public void run() {
                PickResponseChannel responseChannel = importChannel(responsechannel, PickResponseChannel.class);
                responseChannel.onRequestRcvd(idx, mexId);
            }
        });
    }

    void timerEvent(final String timerResponseChannel) {
        // In case this is a pick event, we remove routes,
        // and cancel the outstanding requests.
        _dao.getProcess().removeRoutes(timerResponseChannel, _dao);
        _outstandingRequests.cancel(timerResponseChannel);

        // Ignore timer events after the process is finished.
        if (ProcessState.isFinished(_dao.getState())) {
            return;
        }

        _vpu.inject(new JacobRunnable() {
            private static final long serialVersionUID = -7767141033611036745L;

            public void run() {
                TimerResponseChannel responseChannel = importChannel(timerResponseChannel, TimerResponseChannel.class);
                responseChannel.onTimeout();
            }
        });
        execute();
    }

    public void cancel(final TimerResponseChannel timerResponseChannel) {
        // In case this is a pick response channel, we need to cancel routes and
        // receive/reply association.
        final String id = timerResponseChannel.export();
        _dao.getProcess().removeRoutes(id, _dao);
        _outstandingRequests.cancel(id);

        _vpu.inject(new JacobRunnable() {
            private static final long serialVersionUID = 6157913683737696396L;

            public void run() {
                TimerResponseChannel responseChannel = importChannel(id, TimerResponseChannel.class);
                responseChannel.onCancel();
            }
        });
    }

    void invocationResponse(PartnerRoleMessageExchangeImpl mex) {
        invocationResponse(mex.getDAO().getMessageExchangeId(), mex.getDAO().getChannel());
    }

    void invocationResponse(final String mexid, final String responseChannelId) {
        if (responseChannelId == null)
            throw new NullPointerException("Null responseChannelId");
        if (mexid == null)
            throw new NullPointerException("Null mexId");

        if (BpelProcess.__log.isDebugEnabled()) {
            __log.debug("Invoking message response for mexid " + mexid + " and channel " + responseChannelId);
        }
        _vpu.inject(new BpelJacobRunnable() {
            private static final long serialVersionUID = -1095444335740879981L;

            public void run() {
                ((BpelRuntimeContextImpl) getBpelRuntimeContext()).invocationResponse2(mexid, importChannel(
                        responseChannelId, InvokeResponseChannel.class));
            }
        });
    }

    /**
     * Continuation of the above.
     *
     * @param mexid
     * @param responseChannel
     */
    private void invocationResponse2(String mexid, InvokeResponseChannel responseChannel) {
        __log.debug("Triggering response");
        MessageExchangeDAO mex = _dao.getConnection().getMessageExchange(mexid);

        ProcessMessageExchangeEvent evt = new ProcessMessageExchangeEvent();
        evt.setPortType(mex.getPortType());
        evt.setMexId(mexid);
        evt.setOperation(mex.getOperation());

        MessageExchange.Status status = MessageExchange.Status.valueOf(mex.getStatus());

        switch (status) {
            case FAULT:
                evt.setAspect(ProcessMessageExchangeEvent.PARTNER_FAULT);
                responseChannel.onFault();
                break;
            case RESPONSE:
                evt.setAspect(ProcessMessageExchangeEvent.PARTNER_OUTPUT);
                responseChannel.onResponse();
                break;
            case FAILURE:
                evt.setAspect(ProcessMessageExchangeEvent.PARTNER_FAILURE);
                responseChannel.onFailure();
                break;
            default:
                __log.error("Invalid response state for mex " + mexid + ": " + status);
        }
        sendEvent(evt);
    }

    /**
     * @see BpelRuntimeContext#sendEvent(org.apache.ode.bpel.evt.ProcessInstanceEvent)
     */
    public void sendEvent(ProcessInstanceEvent event) {
        // fill in missing pieces
        event.setProcessId(_dao.getProcess().getProcessId());
        event.setProcessName(_dao.getProcess().getType());
        event.setProcessInstanceId(_dao.getInstanceId());
        _bpelProcess._debugger.onEvent(event);

        // notify the listeners
        _bpelProcess._engine.fireEvent(event);

        // saving
        _bpelProcess.saveEvent(event, _dao);
    }

    /**
     * We record all values of properties of a 'MessageType' variable for
     * efficient lookup.
     */
    private void writeProperties(VariableInstance variable, Node value, XmlDataDAO dao) {
        if (variable.declaration.type instanceof OMessageVarType) {
            for (OProcess.OProperty property : variable.declaration.getOwner().properties) {
                OProcess.OPropertyAlias alias = property.getAlias(variable.declaration.type);
                if (alias != null) {
                    try {
                        String val = _bpelProcess.extractProperty((Element) value, alias, variable.declaration
                                .getDescription());
                        if (val != null) {
                            dao.setProperty(property.name.toString(), val);
                        }
                    } catch (FaultException e) {
                        // This will fail as we're basically trying to extract properties on all
                        // received messages for optimization purposes.
                        if (__log.isDebugEnabled())
                            __log.debug("Couldn't extract property '" + property.toString()
                                    + "' in property pre-extraction: " + e.toString());
                    }
                }
            }
        }
    }

    private void completeOutstandingMessageExchanges() {
        String[] mexRefs = _outstandingRequests.releaseAll();
        for (String mexId : mexRefs) {
            MessageExchangeDAO mexDao = _dao.getConnection().getMessageExchange(mexId);
            if (mexDao != null) {
                MyRoleMessageExchangeImpl mex = new MyRoleMessageExchangeImpl(_bpelProcess._engine, mexDao);
                switch (mex.getStatus()) {
                    case ASYNC:
                    case RESPONSE:
                        mex.setStatus(MessageExchange.Status.COMPLETED_OK);
                        break;
                    case REQUEST:
                        if (mex.getPattern().equals(MessageExchange.MessageExchangePattern.REQUEST_ONLY)) {
                            mex.setStatus(MessageExchange.Status.COMPLETED_OK);
                            break;
                        }
                    default:
                        mex.setFailure(FailureType.OTHER, "No response.", null);
                        _bpelProcess._engine._contexts.mexContext.onAsyncReply(mex);
                        mex.release();
                }
            }
        }
    }

    private void faultOutstandingMessageExchanges(FaultData faultData) {
        String[] mexRefs = _outstandingRequests.releaseAll();
        for (String mexId : mexRefs) {
            MessageExchangeDAO mexDao = _dao.getConnection().getMessageExchange(mexId);
            if (mexDao != null) {
                MyRoleMessageExchangeImpl mex = new MyRoleMessageExchangeImpl(_bpelProcess._engine, mexDao);
                _bpelProcess.initMyRoleMex(mex);

                Message message = mex.createMessage(faultData.getFaultName());
                if (faultData.getFaultMessage() != null)
                    message.setMessage(faultData.getFaultMessage());
                mex.setResponse(message);

                mex.setFault(faultData.getFaultName(), message);
                mex.setFaultExplanation(faultData.getExplanation());
                _bpelProcess._engine._contexts.mexContext.onAsyncReply(mex);
            }
        }
    }

    private void failOutstandingMessageExchanges() {
        String[] mexRefs = _outstandingRequests.releaseAll();
        for (String mexId : mexRefs) {
            MessageExchangeDAO mexDao = _dao.getConnection().getMessageExchange(mexId);
            MyRoleMessageExchangeImpl mex = new MyRoleMessageExchangeImpl(_bpelProcess._engine, mexDao);
            _bpelProcess.initMyRoleMex(mex);
            mex.setFailure(FailureType.OTHER, "No response.", null);
            _bpelProcess._engine._contexts.mexContext.onAsyncReply(mex);
        }
    }

    public Element getPartnerResponse(String mexId) {
        return _getPartnerResponse(mexId).getData();
    }

    public Element getMyRequest(String mexId) {
        MessageExchangeDAO dao = _dao.getConnection().getMessageExchange(mexId);
        if (dao == null) {
            // this should not happen....
            String msg = "Engine requested non-existent message exchange: " + mexId;
            __log.fatal(msg);
            throw new BpelEngineException(msg);
        }

        if (dao.getDirection() != MessageExchangeDAO.DIR_PARTNER_INVOKES_MYROLE) {
            // this should not happen....
            String msg = "Engine requested my-role request for a partner-role mex: " + mexId;
            __log.fatal(msg);
            throw new BpelEngineException(msg);
        }

        MessageExchange.Status status = MessageExchange.Status.valueOf(dao.getStatus());
        switch (status) {
            case ASYNC:
            case REQUEST:
                MessageDAO request = dao.getRequest();
                if (request == null) {
                    // this also should not happen
                    String msg = "Engine requested request for message exchange that did not have one: " + mexId;
                    __log.fatal(msg);
                    throw new BpelEngineException(msg);
                }

                return request.getData();

            default:
                // We should not be in any other state when requesting this.
                String msg = "Engine requested response while the message exchange " + mexId + " was in the state "
                        + status;
                __log.fatal(msg);
                throw new BpelEngineException(msg);
        }

    }

    public QName getPartnerFault(String mexId) {
        MessageExchangeDAO mex = _getPartnerResponse(mexId).getMessageExchange();
        return  mex.getFault();
    }

    public QName getPartnerResponseType(String mexId) {
        return _getPartnerResponse(mexId).getType();
    }

    public String getPartnerFaultExplanation(String mexId) {
        MessageExchangeDAO dao = _dao.getConnection().getMessageExchange(mexId);
        return dao != null ? dao.getFaultExplanation() : null;
    }

    private MessageDAO _getPartnerResponse(String mexId) {
        MessageExchangeDAO dao = _dao.getConnection().getMessageExchange(mexId);
        if (dao == null) {
            // this should not happen....
            String msg = "Engine requested non-existent message exchange: " + mexId;
            __log.fatal(msg);
            throw new BpelEngineException(msg);
        }
        if (dao.getDirection() != MessageExchangeDAO.DIR_BPEL_INVOKES_PARTNERROLE) {
            // this should not happen....
            String msg = "Engine requested partner response for a my-role mex: " + mexId;
            __log.fatal(msg);
            throw new BpelEngineException(msg);
        }

        MessageDAO response;
        MessageExchange.Status status = MessageExchange.Status.valueOf(dao.getStatus());
        switch (status) {
            case FAULT:
            case RESPONSE:
                response = dao.getResponse();
                if (response == null) {
                    // this also should not happen
                    String msg = "Engine requested response for message exchange that did not have one: " + mexId;
                    __log.fatal(msg);
                    throw new BpelEngineException(msg);
                }
                break;
            default:
                // We should not be in any other state when requesting this.
                String msg = "Engine requested response while the message exchange " + mexId + " was in the state "
                        + status;
                __log.fatal(msg);
                throw new BpelEngineException(msg);
        }
        return response;
    }

    public void releasePartnerMex(String mexId) {
        MessageExchangeDAO dao = _dao.getConnection().getMessageExchange(mexId);
        dao.release();
    }

    public Node getPartData(Element message, Part part) {
        Element partEl = DOMUtils.findChildByName((Element) message, new QName(null, part.name), false);

        // This could occur if the message does not contain the required part.
        if (partEl == null)
            return null;

        Node container = DOMUtils.getFirstChildElement(partEl);
        if (container == null)
            container = partEl.getFirstChild(); // either a text node / element
        // /
        // xsd-type-wrapper
        return container;
    }

    public Element getSourceEPR(String mexId) {
        MessageExchangeDAO dao = _dao.getConnection().getMessageExchange(mexId);
        String epr = dao.getProperty(MessageExchange.PROPERTY_SEP_PARTNERROLE_EPR);
        if (epr == null)
            return null;
        try {
            Element eepr = DOMUtils.stringToDOM(epr);
            return eepr;
        } catch (Exception ex) {
            __log.error("Invalid value for SEP property " + MessageExchange.PROPERTY_SEP_PARTNERROLE_EPR + ": " + epr);
        }

        return null;
    }

    public String getSourceSessionId(String mexId) {
        MessageExchangeDAO dao = _dao.getConnection().getMessageExchange(mexId);
        return dao.getProperty(MessageExchange.PROPERTY_SEP_PARTNERROLE_SESSIONID);
    }

    public void registerActivityForRecovery(ActivityRecoveryChannel channel, long activityId, String reason,
                                            Date dateTime, Element details, String[] actions, int retries) {
        if (reason == null)
            reason = "Unspecified";
        if (dateTime == null)
            dateTime = new Date();
        __log.info("ActivityRecovery: Registering activity " + activityId + ", failure reason: " + reason +
                " on channel " + channel.export());
        _dao.createActivityRecovery(channel.export(), (int) activityId, reason, dateTime, details, actions, retries);
    }

    public void unregisterActivityForRecovery(ActivityRecoveryChannel channel) {
        _dao.deleteActivityRecovery(channel.export());
    }

    public void recoverActivity(final String channel, final long activityId, final String action, final FaultData fault) {
        _vpu.inject(new JacobRunnable() {
            private static final long serialVersionUID = 3168964409165899533L;

            public void run() {
                ActivityRecoveryChannel recovery = importChannel(channel, ActivityRecoveryChannel.class);
                __log.info("ActivityRecovery: Recovering activity " + activityId + " with action " + action +
                        " on channel " + recovery);
                if (recovery != null) {
                    if ("cancel".equals(action))
                        recovery.cancel();
                    else if ("retry".equals(action))
                        recovery.retry();
                    else if ("fault".equals(action))
                        recovery.fault(fault);
                }
            }
        });
        //_dao.deleteActivityRecovery(channel);
        execute();
    }

    /**
     * Fetch the session-identifier for the partner link from the database.
     */
    public String fetchMySessionId(PartnerLinkInstance pLink) {
        String sessionId = fetchPartnerLinkDAO(pLink).getMySessionId();
        assert sessionId != null : "Session ID should always be set!";
        return sessionId;
    }

    public String fetchPartnersSessionId(PartnerLinkInstance pLink) {
        return fetchPartnerLinkDAO(pLink).getPartnerSessionId();
    }

    public void initializePartnersSessionId(PartnerLinkInstance pLink, String session) {
        if (__log.isDebugEnabled())
            __log.debug("initializing partner " + pLink + "  sessionId to " + session);
        fetchPartnerLinkDAO(pLink).setPartnerSessionId(session);

    }

    /**
     * Attempt to match message exchanges on a correlator.
     *
     */
    public void matcherEvent(String correlatorId, CorrelationKey ckey) {
        if (BpelProcess.__log.isDebugEnabled()) {
            __log.debug("MatcherEvent handling: correlatorId=" + correlatorId + ", ckey=" + ckey);
        }
        CorrelatorDAO correlator = _dao.getProcess().getCorrelator(correlatorId);

        // Find the route first, this is a SELECT FOR UPDATE on the "selector" row,
        // So we want to acquire the lock before we do anthing else.
        MessageRouteDAO mroute = correlator.findRoute(ckey);
        if (mroute == null) {
            // Ok, this means that a message arrived before we did, so nothing to do.
            __log.debug("MatcherEvent handling: nothing to do, route no longer in DB");
            return;
        }

        // Now see if there is a message that matches this selector.
        MessageExchangeDAO mexdao = correlator.dequeueMessage(ckey);
        if (mexdao != null) {
            __log.debug("MatcherEvent handling: found matching message in DB (i.e. message arrived before <receive>)");

            // We have a match, so we can get rid of the routing entries.
            correlator.removeRoutes(mroute.getGroupId(),_dao);

            // Found message matching one of our selectors.
            if (BpelProcess.__log.isDebugEnabled()) {
                BpelProcess.__log.debug("SELECT: " + mroute.getGroupId() + ": matched to MESSAGE " + mexdao
                        + " on CKEY " + ckey);
            }

            MyRoleMessageExchangeImpl mex = new MyRoleMessageExchangeImpl(_bpelProcess._engine, mexdao);

            inputMsgMatch(mroute.getGroupId(), mroute.getIndex(), mex);
            execute();
        } else {
            __log.debug("MatcherEvent handling: nothing to do, no matching message in DB");

        }
    }
}
