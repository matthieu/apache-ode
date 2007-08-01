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
import org.apache.ode.bpel.dao.MessageDAO;
import org.apache.ode.bpel.dao.MessageExchangeDAO;
import org.apache.ode.bpel.iapi.BpelEngineException;
import org.apache.ode.bpel.iapi.InvocationStyle;
import org.apache.ode.bpel.iapi.Message;
import org.apache.ode.bpel.iapi.MessageExchange;
import org.apache.ode.bpel.iapi.MyRoleMessageExchange;
import org.apache.ode.bpel.iapi.Scheduler;
import org.apache.ode.bpel.iapi.MyRoleMessageExchange.CorrelationStatus;
import org.apache.ode.bpel.intercept.AbortMessageExchangeException;
import org.apache.ode.bpel.intercept.FaultMessageExchangeException;
import org.apache.ode.bpel.intercept.InterceptorInvoker;
import org.apache.ode.bpel.intercept.MessageExchangeInterceptor;
import org.apache.ode.bpel.intercept.MessageExchangeInterceptor.InterceptorContext;
import org.apache.ode.bpel.o.OPartnerLink;

import javax.wsdl.Operation;
import javax.xml.namespace.QName;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provides an implementation of the {@link MyRoleMessageExchange} inteface for interactions performed in the
 * {@link InvocationStyle#RELIABLE} style.
 * 
 * @author Maciej Szefler
 */
class ReliableMyRoleMessageExchangeImpl extends MyRoleMessageExchangeImpl implements MyRoleMessageExchange {

    private static final Log __log = LogFactory.getLog(ReliableMyRoleMessageExchangeImpl.class);

    public static final int TIMEOUT = 2 * 60 * 1000;

    
    public ReliableMyRoleMessageExchangeImpl(BpelProcess process, String mexId, OPartnerLink oplink, Operation operation, QName callee) {
        super(process, mexId, oplink, operation, callee);
    }

    public void invokeReliable() {
        // For reliable, we MUST HAVE A TRANSACTION!
        assertTransaction();

        // Cover the case where invoke was already called. 
        if (getStatus() == Status.REQUEST)
            return;
        
        if (getStatus() != Status.NEW)
            throw new BpelEngineException("Invalid state: " + getStatus());
        
        if (!processInterceptors(InterceptorInvoker.__onBpelServerInvoked, getDAO())) {
            throw new BpelEngineException("Intercepted.");
        }
        
        if (__log.isDebugEnabled())
            __log.debug("invoke() EPR= " + _epr + " ==> " + _process);
        setStatus(Status.REQUEST);
        save(getDAO());
        scheduleInvoke();
    }


    @Override
    public InvocationStyle getInvocationStyle() {
        return InvocationStyle.RELIABLE;
    }


}
