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

import org.apache.ode.bpel.engine.BpelManagementFacadeImpl;
import org.apache.ode.bpel.iapi.Message;
import org.apache.ode.bpel.iapi.MessageExchange;
import org.apache.ode.bpel.iapi.MessageExchangeContext;
import org.apache.ode.bpel.iapi.PartnerRoleMessageExchange;
import org.apache.ode.bpel.o.OFailureHandling;
import org.apache.ode.bpel.pmapi.BpelManagementFacade;
import org.apache.ode.bpel.pmapi.TActivityInfo;
import org.apache.ode.bpel.pmapi.TActivityStatus;
import org.apache.ode.bpel.pmapi.TFailureInfo;
import org.apache.ode.bpel.pmapi.TFailuresInfo;
import org.apache.ode.bpel.pmapi.TInstanceInfo;
import org.apache.ode.bpel.pmapi.TInstanceInfoList;
import org.apache.ode.bpel.pmapi.TInstanceStatus;
import org.apache.ode.bpel.pmapi.TInstanceSummary;
import org.apache.ode.bpel.pmapi.TScopeInfo;
import org.apache.ode.bpel.pmapi.TScopeRef;
import org.apache.ode.utils.DOMUtils;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.Invocation;
import org.jmock.core.InvocationMatcher;
import org.jmock.core.Stub;
import org.jmock.core.matcher.StatelessInvocationMatcher;
import org.jmock.core.stub.CustomStub;
import org.jmock.core.stub.StubSequence;

import javax.xml.namespace.QName;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;

/**
 * Test activity recovery and failure handling.
 */
public class ActivityRecoveryTest extends MockObjectTestCase {

    static final String   NAMESPACE = "http://ode.apache.org/bpel/unit-test";
    static final String[] ACTIONS = new String[]{ "retry", "cancel", "fault" };
    MockBpelServer        _server;
    BpelManagementFacade  _management;
    QName                 _processQName;
    QName                 _processId;
    private Mock _testService;


    /**
     * The process calls the failing service, simulated by a call to invoke.
     * The method returns true if the call succeeded, false for failure.
     * If the process completes, it calls the completed method.
     */
    interface TestService {
        public boolean invoke(); 
        public void completed();
    }


    public void testInvokeSucceeds() throws Exception {
        // Since the service invocation succeeds, the process completes.
        _testService.expects(once()).method("invoke").will(returnValue(true));
        _testService.expects(once()).method("completed").after("invoke");

        execute("FailureToRecovery");
        assertTrue(lastInstance().getStatus() == TInstanceStatus.COMPLETED);
        assertNoFailures();
    }

    public void testFailureWithRecoveryAfterRetry() throws Exception {
        // Since the invocation is repeated 3 times, the process completes after
        // the third (successful) invocation.
        _testService.expects(exactly(3)).method("invoke").will(failTheFirst(2));
        _testService.expects(once()).method("completed").after("invoke");

        execute("FailureToRecovery");
        assertTrue(lastInstance().getStatus() == TInstanceStatus.COMPLETED);
        assertNoFailures();
    }

    public void testFailureWithManualRecovery() throws Exception {
        // Recovery required after three failures. Only one attempt made after recovery.
        // Only the fifth invocation succeeds.
        _testService.expects(exactly(5)).method("invoke").will(failTheFirst(4));
        _testService.expects(once()).method("completed").after("invoke");

        execute("FailureToRecovery");
        recover("retry");
        recover("retry");
        assertTrue(lastInstance().getStatus() == TInstanceStatus.COMPLETED);
        assertNoFailures();
    }

    public void testFailureWithFaultAction() throws Exception {
        // Recovery required after three failures. Only one attempt made after recovery.
        // Use the last failure to cause a fault.
        _testService.expects(exactly(4)).method("invoke").will(failTheFirst(4));
        _testService.expects(never()).method("completed").after("invoke");

        execute("FailureToRecovery");
        recover("retry");
        recover("fault");
        assertTrue(lastInstance().getStatus() == TInstanceStatus.FAILED);
        assertTrue(OFailureHandling.FAILURE_FAULT_NAME.equals(lastInstance().getFaultInfo().getName()));
        assertNoFailures();
    }

    public void testFailureWithCancelAction() throws Exception {
        // Recovery required after three failures. Only one attempt made after recovery.
        // Use the last failure to cancel the activity, allowing the process to complete.
        _testService.expects(exactly(4)).method("invoke").will(failTheFirst(4));
        _testService.expects(once()).method("completed").after("invoke");

        execute("FailureToCancel");
        recover("retry");
        recover("cancel");
        assertTrue(lastInstance().getStatus() == TInstanceStatus.COMPLETED);
        assertNoFailures();
    }

    public void testImmediateFailure() throws Exception {
        // This process does not attempt to retry, entering recovery immediately.
        _testService.expects(exactly(1)).method("invoke").will(returnValue(false));
        _testService.expects(never()).method("completed").after("invoke");

        execute("FailureNoRetry");
        assertRecovery(1, ACTIONS);
    }

    public void testImmediateFailureAndFault() throws Exception {
        // This process responds to failure with a fault.
        _testService.expects(exactly(1)).method("invoke").will(returnValue(false));
        _testService.expects(never()).method("completed").after("invoke");

        execute("FailureToFault");
        assertTrue(lastInstance().getStatus() == TInstanceStatus.FAILED);
        assertTrue(OFailureHandling.FAILURE_FAULT_NAME.equals(lastInstance().getFaultInfo().getName()));
        assertNoFailures();
    }

    public void testFailureHandlingInheritence() throws Exception {
        // Since the invocation is repeated 3 times, the process completes after
        // the third (successful) invocation.
        _testService.expects(exactly(3)).method("invoke").will(failTheFirst(2));
        _testService.expects(once()).method("completed").after("invoke");

        execute("FailureInheritence");
        assertTrue(lastInstance().getStatus() == TInstanceStatus.COMPLETED);
        assertNoFailures();
    }

    public void testInstanceSummary() throws Exception {
        _processQName = new QName(NAMESPACE, "FailureToRecovery");
        _processId = new QName(NAMESPACE, "FailureToRecovery-1");
        // Failing the first three times and recovering, the process completes.
        _testService.expects(exactly(4)).method("invoke").will(failTheFirst(3));
        _testService.expects(once()).method("completed").after("invoke");
        _server.invoke(_processQName, "instantiate", DOMUtils.newDocument().createElementNS(NAMESPACE, "tns:RequestElement"));
        _server.waitForBlocking();
        recover("retry"); // Completed.
        // Failing the first three times, we can then fault the process.
        _testService.expects(exactly(3)).method("invoke").will(failTheFirst(3));
        _server.invoke(_processQName, "instantiate", DOMUtils.newDocument().createElementNS(NAMESPACE, "tns:RequestElement"));
        _server.waitForBlocking();
        recover("fault"); // Faulted.
        // Failing the first three times, we can then leave it waiting for recovery.
        _testService.expects(exactly(3)).method("invoke").will(failTheFirst(3));
        _server.invoke(_processQName, "instantiate", DOMUtils.newDocument().createElementNS(NAMESPACE, "tns:RequestElement"));
        _server.waitForBlocking(); // Active, recovery.
        // Stay active, awaiting recovery.

        TInstanceSummary summary = _management.getProcessInfo(_processId).getProcessInfo().getInstanceSummary();
        for (TInstanceSummary.Instances instances : summary.getInstancesList()) {
            switch (instances.getState().intValue()) {
              case TInstanceStatus.INT_COMPLETED:
                assertTrue(instances.getCount() == 1);
                break;
              case TInstanceStatus.INT_FAILED:
                assertTrue(instances.getCount() == 1);
                break;
              case TInstanceStatus.INT_ACTIVE:
                assertTrue(instances.getCount() == 1);
                break;
              default:
                assertTrue(instances.getCount() == 0);
                break;
            }
        }
        assertTrue(summary.getFailures().getCount() == 1);
        assertNotNull(summary.getFailures().getDtFailure());
    }


    protected void setUp() throws Exception {
        // Override testService in test case.
        _testService = mock(TestService.class);
        // We use one partner to simulate failing service and receive message upon process completion.
        final Mock partner = mock(MessageExchangeContext.class);
        // Some processes will complete, but not all.
        partner.expects(atMostOnce()).match(invokeOnOperation("respond")).will(new CustomStub("process completed") {
            public Object invoke(Invocation invocation) {
                ((TestService)_testService.proxy()).completed();
                return null;
            }
        });
        // There will be multiple calls to invoke.
        partner.expects(atLeastOnce()).match(invokeOnOperation("invoke")).will(new CustomStub("invoke failing service") {
            public Object invoke(Invocation invocation) {
                PartnerRoleMessageExchange mex = (PartnerRoleMessageExchange) invocation.parameterValues.get(0);
                if (((TestService)_testService.proxy()).invoke()) {
                    Message response = mex.createMessage(mex.getOperation().getOutput().getMessage().getQName());
                    response.setMessage(DOMUtils.newDocument().createElementNS(NAMESPACE, "tns:ResponseElement"));
                    mex.reply(response);
                } else {
                    mex.replyWithFailure(MessageExchange.FailureType.COMMUNICATION_ERROR, "BangGoesInvoke", null);
                }
                return null;
            }
        });
        // Faulting a process would send the fault message asynchronously.
        // (Which might be a bug, but right now we swallow it).
        partner.expects(atMostOnce()).method("onAsyncReply").will(new CustomStub("async reply") {
            public Object invoke(Invocation invocation) {
                return null;
            }
        });
        
        _server = new MockBpelServer() {
            protected MessageExchangeContext createMessageExchangeContext() {
                return (MessageExchangeContext) partner.proxy();
            }
        };
        _server.deploy(new File(new URI(this.getClass().getResource("/recovery").toString())));
        _management = new BpelManagementFacadeImpl(_server._server,_server._store);
    }

    protected void tearDown() throws Exception {
        _management.delete(null);
        _server.shutdown();
    }

    /**
     * Returns a stub that will fail (return false) for the first n number of times,
     * and on the last call succeed (return true).
     */
    protected Stub failTheFirst(int times) {
        Stub[] stubs = new Stub[times + 1];
        for (int i = 0; i < times; ++i)
            stubs[i] = returnValue(false);
        stubs[times] = returnValue(true);
        return new StubSequence(stubs);
    }

    protected InvocationMatcher invokeOnOperation(final String opName) {
        // Decides which method to call the TestService mock based on the operation.
        return new StatelessInvocationMatcher() {
            public boolean matches(Invocation invocation) {
                return invocation.invokedMethod.getName().equals("invokePartner") &&
                    invocation.parameterValues.size() == 1 &&
                    ((PartnerRoleMessageExchange) invocation.parameterValues.get(0)).getOperation().getName().equals(opName);
            }

            public StringBuffer describeTo(StringBuffer buffer) {
                return buffer.append("check that the operation ").append(opName).append(" is invoked");
            }
        };
    }

    /**
     * Call this to execute the process so it fails the specified number of times.
     * Returns when the process has either completed, or waiting for recovery to happen.
     */
    protected void execute(String process) throws Exception {
        _management.delete(null);
        // We need the process QName to make assertions on its state.
        _processQName = new QName(NAMESPACE, process);
        _processId = new QName(NAMESPACE, process + "-1");
        _server.invoke(_processQName, "instantiate", DOMUtils.newDocument().createElementNS(NAMESPACE, "tns:RequestElement"));
        _server.waitForBlocking();
    }

    protected void assertNoFailures() {
        TFailuresInfo failures = lastInstance().getFailures();
        assertTrue(failures == null || failures.getCount() == 0);
        failures = _management.getProcessInfo(_processId).getProcessInfo().getInstanceSummary().getFailures();
        assertTrue(failures == null || failures.getCount() == 0);
    }

    protected TInstanceInfo lastInstance() {
        TInstanceInfoList instances = _management.listAllInstances().getInstanceInfoList();
        return instances.getInstanceInfoArray(instances.sizeOfInstanceInfoArray() - 1);
    }

    /**
     * Asserts that the process has one activity in the recovery state.
     */
    protected void assertRecovery(int invoked, String[] actions) {
        // Process is still active, none of the completed states.
        assertTrue(lastInstance().getStatus() == TInstanceStatus.ACTIVE);
        // Tests here will only generate one failure.
        TFailuresInfo failures = lastInstance().getFailures();
        assertTrue(failures != null && failures.getCount() == 1);
        failures = _management.getProcessInfo(_processId).getProcessInfo().getInstanceSummary().getFailures();
        assertTrue(failures != null && failures.getCount() == 1);
        // Look for individual activities inside the process instance.
        @SuppressWarnings("unused")
        ArrayList<TActivityInfo> recoveries = getRecoveriesInScope(lastInstance(), null, null);
        assertTrue(recoveries.size() == 1);
        TFailureInfo failure = recoveries.get(0).getFailure();
        assertTrue(failure.getRetries() == invoked - 1);
        assertTrue(failure.getReason().equals("BangGoesInvoke"));
        assertTrue(failure.getDtFailure() != null);
        java.util.HashSet<String> actionSet = new java.util.HashSet<String>();
        for (String action : failure.getActions().split(" "))
            actionSet.add(action);
        for (String action : actions)
            assertTrue(actionSet.remove(action));
    }

    /**
     * Performs the specified recovery action. Also asserts that there is one
     * recovery channel for the activity in question.
     */
    protected void recover(String action) {
        ArrayList<TActivityInfo> recoveries = getRecoveriesInScope(lastInstance(), null, null);
        assertTrue(recoveries.size() == 1);
        TActivityInfo activity = recoveries.get(0);
        assertNotNull(activity);
        _management.recoverActivity(Long.valueOf(lastInstance().getIid()), Long.valueOf(activity.getAiid()), action);
        _server.waitForBlocking();
    }

    protected ArrayList<TActivityInfo> getRecoveriesInScope(TInstanceInfo instance, TScopeInfo scope,
                                                            ArrayList<TActivityInfo> recoveries) {
        if (scope == null)
            scope = _management.getScopeInfoWithActivity(lastInstance().getRootScope().getSiid(), true).getScopeInfo();
        if (recoveries == null)
            recoveries = new ArrayList<TActivityInfo>();
        TScopeInfo.Activities activities = scope.getActivities();
        for (int i = 0; i < activities.sizeOfActivityInfoArray(); ++i) {
            TActivityInfo activity = activities.getActivityInfoArray(i);
            if (activity.getStatus() == TActivityStatus.FAILURE) {
                assertNotNull(activity.getFailure());
                recoveries.add(activity);
            } else
                assertNull(activity.getFailure());
        }
        for (TScopeRef ref : scope.getChildren().getChildRefList()) {
            TScopeInfo child = _management.getScopeInfoWithActivity(ref.getSiid(), true).getScopeInfo();
            if (child != null)
                getRecoveriesInScope(instance, child, recoveries);
        }
        return recoveries;
    }

}
