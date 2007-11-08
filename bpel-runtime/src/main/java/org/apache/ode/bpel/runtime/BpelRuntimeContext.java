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

import org.apache.ode.bpel.common.CorrelationKey;
import org.apache.ode.bpel.common.FaultException;
import org.apache.ode.bpel.evt.ProcessInstanceEvent;
import org.apache.ode.bpel.o.OMessageVarType;
import org.apache.ode.bpel.o.OMessageVarType.Part;
import org.apache.ode.bpel.o.OPartnerLink;
import org.apache.ode.bpel.o.OProcess;
import org.apache.ode.bpel.o.OScope;
import org.apache.ode.bpel.runtime.channels.FaultData;
import org.apache.ode.bpel.runtime.channels.InvokeResponseChannel;
import org.apache.ode.bpel.runtime.channels.PickResponseChannel;
import org.apache.ode.bpel.runtime.channels.TimerResponseChannel;
import org.apache.ode.bpel.runtime.channels.ActivityRecoveryChannel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.wsdl.Operation;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import javax.xml.namespace.QName;

/**
 * <p>A facade for accessing all the BPEL functionality that is not implemented
 * directly in the JACOB framework, but rather "natively" . Things that are so
 * implemented include variables (i.e. the JACOB state-space does not include
 * dimensions for variables, these are instead implemented as database rows of
 * some sort), the communication activities pick/receive/invoke (i.e. there
 * are no JACOB channels representing partnerLink links), and correlation (i.e.
 * the JACOB objects do not match message to process instances, this happens
 * in this <em>native</em> layer).</p>  
 */
public interface BpelRuntimeContext {

    Long getPid();

    /**
     * Checks for variable initialization, i.e. has had a 'write'
     *
     * @param variable variable
     *
     * @return <code>true</code> if initialized
     */
    boolean isVariableInitialized(VariableInstance variable);

    /**
     * Create a scope instance object.
     * @param parentScopeId _id of parent scope (null if root scope)
     * @param scopeType the type of scope, i.e. the name of the scope
     *
     * @return scope instance identifier
     */
    Long createScopeInstance(Long parentScopeId, OScope scopeType);

    /**
     * Initializes endpoint references for partner links inside a scope.
     * @param parentScopeId
     * @param partnerLinks
     */
    void initializePartnerLinks(Long parentScopeId, Collection<OPartnerLink> partnerLinks);

    /**
     *
     * @param var variable to read
     * @return
     */
    Node fetchVariableData(VariableInstance var, boolean forWriting)
            throws FaultException;

    Node fetchVariableData(VariableInstance var, OMessageVarType.Part partname, boolean forWriting)
            throws FaultException;

    /**
     * Fetches the my-role endpoint reference data.
     * @param pLink
     * @param isMyEPR
     * @return
     * @throws FaultException
     */
    Element fetchMyRoleEndpointReferenceData(PartnerLinkInstance pLink);

    Element fetchPartnerRoleEndpointReferenceData(PartnerLinkInstance pLink) throws FaultException;

    /**
     * Determine if the partner role of an endpoint has been initialized (either explicitly throug assginment or via the
     * deployment descriptor)
     * @param pLink partner link
     * @return
     */
    boolean isPartnerRoleEndpointInitialized(PartnerLinkInstance pLink);

    /**
     * Fetches our session id associated with the partner link instance.  This will always return a
     * non-null value.
     * @param pLink partner link
     */
    String fetchMySessionId(PartnerLinkInstance pLink);

    /**
     * Fetches the partner's session id associated with the partner link instance.
     * @param pLink partner link
     */
    String fetchPartnersSessionId(PartnerLinkInstance pLink);

    /**
     * Initialize the partner's session id for this partner link instance.
     * @param pLink partner link
     * @param session session identifier
     */
    void initializePartnersSessionId(PartnerLinkInstance pLink, String session);

    /**
     * Evaluate a property alias query expression against a variable, returning the normalized
     * {@link String} representation of the property value.
     * @param var variable to read
     * @param property property to read
     * @return value of property for variable, in String form
     * @throws FaultException in case of selection or other fault
     */
    String readProperty(VariableInstance var, OProcess.OProperty property)
            throws FaultException;

    Node initializeVariable(VariableInstance var, Node initData) ;

    /**
     * Writes a partner EPR.
     *
     * @param variable
     * @param data
     * @throws FaultException
     */
    void writeEndpointReference(PartnerLinkInstance variable, Element data) throws FaultException;

    Node convertEndpointReference(Element epr, Node targetNode);

    void commitChanges(VariableInstance var, Node changes);

    boolean isCorrelationInitialized(CorrelationSetInstance cset);

    CorrelationKey readCorrelation(CorrelationSetInstance cset);

    void writeCorrelation(CorrelationSetInstance cset, CorrelationKey correlation);

    void forceFlush();
    /**
     * Should be invoked by process template, signalling process completion
     * with no faults.
     *
     */
    void completedOk();

    /**
     * Should be invoked by process template, signalling process completion
     * with fault.
     */
    void completedFault(FaultData faultData);


    /**
     * Non-deterministic selection on incoming message-exchanges.
     */
    void select(PickResponseChannel response, Date timeout, boolean createInstnace,
                Selector[] selectors) throws FaultException;

    /**
     * Cancel a timer, or pick.
     * @param timerResponseChannel
     */
    void cancel(TimerResponseChannel timerResponseChannel);

    /**
     * Send a reply to an open message-exchange.
     * @param msg response message
     * @param fault fault name, if this is a fault reply, otherwise <code>null</code>
     */
    void reply(PartnerLinkInstance plink, String opName, String mexId, Element msg,
               QName fault)
            throws FaultException;


    String invoke(PartnerLinkInstance partnerLinkInstance,
                  Operation operation,
                  Element outboundMsg,
                  InvokeResponseChannel invokeResponseChannel) throws FaultException;


    /**
     * Registers a timer for future notification.
     * @param timerChannel channel for timer notification
     * @param timeToFire future time to fire timer notification
     */
    void registerTimer(TimerResponseChannel timerChannel, Date timeToFire);

    /**
     * Terminates the process / sets state flag to terminate
     * and ceases all processing on the VPU.
     */
    void terminate();

    /**
     * Sends the bpel event.
     * @param event
     */
    void sendEvent(ProcessInstanceEvent event);

    ExpressionLanguageRuntimeRegistry getExpLangRuntime();


    /**
     * Generate a unique (and monotonic) ID in the context of this instance.
     * @return
     */
    long genId();

    Element getPartnerResponse(String mexId);

    Element getMyRequest(String mexId);

    QName getPartnerFault(String mexId);

    String getPartnerFaultExplanation(String mexId);

    QName getPartnerResponseType(String mexId);

    Node getPartData(Element message, Part part);

    Element getSourceEPR(String mexId);

    void registerActivityForRecovery(ActivityRecoveryChannel channel, long activityId, String reason,
                                     Date dateTime, Element details, String[] actions, int retries);

    void unregisterActivityForRecovery(ActivityRecoveryChannel channel);

    void recoverActivity(String channel, long activityId, String action, FaultData fault);

    String getSourceSessionId(String mexId);

    void releasePartnerMex(String mexId);

    void initializeExternalVariable(VariableInstance instance, HashMap<String, String> keymap);
}
