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

package org.apache.ode.dao.jpa;

import org.apache.ode.bpel.common.CorrelationKey;
import org.apache.ode.bpel.dao.CorrelatorDAO;
import org.apache.ode.bpel.dao.MessageExchangeDAO;
import org.apache.ode.bpel.dao.MessageRouteDAO;
import org.apache.ode.bpel.dao.ProcessInstanceDAO;
import org.apache.openjpa.persistence.jdbc.ElementJoinColumn;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

@Entity
@Table(name="ODE_CORRELATOR")
public class CorrelatorDAOImpl implements CorrelatorDAO {

    @Id @Column(name="CORRELATOR_ID")
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long _correlatorId;
    @Basic @Column(name="CORRELATOR_KEY") private String _correlatorKey;
    @OneToMany(fetch=FetchType.LAZY,cascade={CascadeType.ALL})
    @ElementJoinColumn(name="CORR_ID", referencedColumnName="ROUTE_ID")
    private Collection<MessageRouteDAOImpl> _routes = new ArrayList<MessageRouteDAOImpl>();
    @OneToMany(fetch=FetchType.LAZY,cascade={CascadeType.ALL})
    @ElementJoinColumn(name="CORR_ID", referencedColumnName="MEX_ID")
    private Collection<MessageExchangeDAOImpl> _exchanges = new ArrayList<MessageExchangeDAOImpl>();

    @Version @Column(name="VERSION") private long _version;

    public CorrelatorDAOImpl(){}
    public CorrelatorDAOImpl(String correlatorKey) {
        _correlatorKey = correlatorKey;
    }

    public void addRoute(String routeGroupId, ProcessInstanceDAO target,
                         int index, CorrelationKey correlationKey) {
        MessageRouteDAOImpl mr = new MessageRouteDAOImpl(correlationKey,routeGroupId,index,(ProcessInstanceDAOImpl)target);

        _routes.add(mr);
    }

    public MessageExchangeDAO dequeueMessage(CorrelationKey correlationKey) {
        for (Iterator itr=_exchanges.iterator(); itr.hasNext();){
            MessageExchangeDAOImpl mex = (MessageExchangeDAOImpl)itr.next();
            if (mex.getCorrelationKeys().contains(correlationKey)) {
                itr.remove();
                return mex;
            }
        }
        return null;
    }

    public void enqueueMessage(MessageExchangeDAO mex,
                               CorrelationKey[] correlationKeys) {
        for (CorrelationKey key : correlationKeys ) {
            ((MessageExchangeDAOImpl)mex).addCorrelationKey(key);
        }
        _exchanges.add((MessageExchangeDAOImpl)mex);

    }

    public MessageRouteDAO findRoute(CorrelationKey correlationKey) {
        for (MessageRouteDAOImpl mr : _routes ) {
            if ( mr.getCorrelationKey().equals(correlationKey)) return mr;
        }
        return null;
    }

    public String getCorrelatorId() {
        return _correlatorKey;
    }

    public void removeRoutes(String routeGroupId, ProcessInstanceDAO target) {
        // remove route across all correlators of the process
        ((ProcessInstanceDAOImpl)target).removeRoutes(routeGroupId);
    }

    void removeLocalRoutes(String routeGroupId, ProcessInstanceDAO target) {
        for (Iterator itr=_routes.iterator(); itr.hasNext(); ) {
            MessageRouteDAOImpl mr = (MessageRouteDAOImpl)itr.next();
            if ( mr.getGroupId().equals(routeGroupId) &&
                    mr.getTargetInstance().equals(target))
                itr.remove();
        }
    }
}
