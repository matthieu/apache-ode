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
package org.apache.ode.daohib.bpel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.common.BpelEventFilter;
import org.apache.ode.bpel.common.InstanceFilter;
import org.apache.ode.bpel.dao.*;
import org.apache.ode.bpel.evt.BpelEvent;
import org.apache.ode.bpel.evt.ScopeEvent;
import org.apache.ode.daohib.SessionManager;
import org.apache.ode.daohib.bpel.hobj.*;
import org.apache.ode.daohib.bpel.ql.HibernateInstancesQueryCompiler;
import org.apache.ode.ql.eval.skel.CommandEvaluator;
import org.apache.ode.ql.tree.Builder;
import org.apache.ode.ql.tree.BuilderFactory;
import org.apache.ode.ql.tree.nodes.Query;
import org.apache.ode.utils.SerializableUtils;
import org.apache.ode.utils.stl.CollectionsX;
import org.apache.ode.utils.stl.UnaryFunctionEx;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Projections;

import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.util.*;

/**
 * Hibernate-based {@link BpelDAOConnection} implementation.
 */
class BpelDAOConnectionImpl implements BpelDAOConnection {

    private static final Log __log = LogFactory.getLog(BpelDAOConnectionImpl.class);

    private Session _session;

    private SessionManager _sm;

    BpelDAOConnectionImpl(SessionManager sm) {
        _sm = sm;
        _session = _sm.getSession();
    }

    public MessageExchangeDAO createMessageExchange(String mexId, char dir) {
        HMessageExchange mex = new HMessageExchange();
        mex.setMexId(mexId);
        mex.setDirection(dir);
        _session.save(mex);
        return new MessageExchangeDaoImpl(_sm, mex);
    }

    public MessageExchangeDAO getMessageExchange(String mexId) {
        try {
            Criteria criteria = _session.createCriteria(HProcess.class);
            criteria.add(Expression.eq("mexId", mexId));
            HMessageExchange mex = (HMessageExchange) criteria.uniqueResult();
            return mex == null ? null : new MessageExchangeDaoImpl(_sm, mex);
        } catch (HibernateException e) {
            __log.error("DbError", e);
            throw e;
        }
    }

    public ProcessDAO createProcess(QName pid, QName type, String guid, long version) {
        HProcess process = new HProcess();
        process.setProcessId(pid.toString());
        process.setTypeName(type.getLocalPart());
        process.setTypeNamespace(type.getNamespaceURI());
        process.setDeployDate(new Date());
        process.setGuid(guid);
        process.setVersion(version);
        _session.save(process);
        return new ProcessDaoImpl(_sm, process);
    }

    public ProcessDAO getProcess(QName processId) {

        try {
            Criteria criteria = _session.createCriteria(HProcess.class);
            criteria.add(Expression.eq("processId", processId.toString()));
            // For the moment we are expecting only one result.
            HProcess hprocess = (HProcess) criteria.uniqueResult();
            return hprocess == null ? null : new ProcessDaoImpl(_sm, hprocess);
        } catch (HibernateException e) {
            __log.error("DbError", e);
            throw e;
        }

    }

    public void close() {
    }

    /**
     * @see org.apache.ode.bpel.dao.ProcessDAO#getInstance(java.lang.Long)
     */
    public ProcessInstanceDAO getInstance(Long instanceId) {
        return _getInstance(_sm, _session, instanceId);
    }

    public ScopeDAO getScope(Long siidl) {
        return _getScope(_sm, _session, siidl);
    }

    public Collection<ProcessInstanceDAO> instanceQuery(InstanceFilter criteria) {
        if (criteria.getLimit() == 0) {
            return Collections.emptyList();
        }
        List<ProcessInstanceDAO> daos = new ArrayList<ProcessInstanceDAO>();

        Iterator<HProcessInstance> iter = _instanceQuery(_session, false, criteria);
        while (iter.hasNext()) {
            daos.add(new ProcessInstanceDaoImpl(_sm, iter.next()));
        }

        return daos;
    }

    @SuppressWarnings("unchecked")
    static Iterator<HProcessInstance> _instanceQuery(Session session, boolean countOnly, InstanceFilter filter) {
        Criteria crit = session.createCriteria(HProcessInstance.class);
        CriteriaBuilder cb = new CriteriaBuilder();
        cb.buildCriteria(crit, filter);
        return crit.list().iterator();
    }

    static ProcessInstanceDAO _getInstance(SessionManager sm, Session session, Long iid) {
        HProcessInstance instance = (HProcessInstance) session.get(HProcessInstance.class, iid);
        return instance != null ? new ProcessInstanceDaoImpl(sm, instance) : null;
    }

    static ScopeDAO _getScope(SessionManager sm, Session session, Long siid) {
        HScope scope = (HScope) session.get(HScope.class, siid);
        return scope != null ? new ScopeDaoImpl(sm, scope) : null;
    }

    public void insertBpelEvent(BpelEvent event, ProcessDAO process, ProcessInstanceDAO instance) {
        _insertBpelEvent(_sm.getSession(), event, process, instance);
    }

    /**
     * Helper method for inserting bpel events into the database.
     * 
     * @param sess
     * @param event
     * @param process
     * @param instance
     */
    static void _insertBpelEvent(Session sess, BpelEvent event, ProcessDAO process, ProcessInstanceDAO instance) {
        HBpelEvent hevent = new HBpelEvent();
        hevent.setTstamp(new Timestamp(System.currentTimeMillis()));
        hevent.setType(BpelEvent.eventName(event));
        hevent.setDetail(event.toString());
        if (process != null)
            hevent.setProcess((HProcess) ((ProcessDaoImpl) process).getHibernateObj());
        if (instance != null)
            hevent.setInstance((HProcessInstance) ((ProcessInstanceDaoImpl) instance).getHibernateObj());
        if (event instanceof ScopeEvent)
            hevent.setScopeId(((ScopeEvent) event).getScopeId());
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(event);
            oos.flush();
            HLargeData ld = new HLargeData(bos.toByteArray());
            hevent.setData(ld);
            sess.save(ld);
        } catch (Throwable ex) {
            // this is really unexpected.
            __log.fatal("InternalError: BpelEvent serialization failed.", ex);
        }
        sess.save(hevent);
    }

    @SuppressWarnings( { "unchecked", "deprecation" })
    public List<Date> bpelEventTimelineQuery(InstanceFilter ifilter, BpelEventFilter efilter) {
        CriteriaBuilder cb = new CriteriaBuilder();
        Criteria crit = _session.createCriteria(HBpelEvent.class);
        if (ifilter != null)
            cb.buildCriteria(crit, efilter);
        if (ifilter != null)
            cb.buildCriteria(crit.createCriteria("instance"), ifilter);
        crit.setFetchMode("tstamp", FetchMode.EAGER);
        crit.setProjection(Projections.property("tstamp"));
        return crit.list();
    }

    @SuppressWarnings("unchecked")
    public List<BpelEvent> bpelEventQuery(InstanceFilter ifilter, BpelEventFilter efilter) {
        CriteriaBuilder cb = new CriteriaBuilder();
        Criteria crit = _session.createCriteria(HBpelEvent.class);
        if (efilter != null)
            cb.buildCriteria(crit, efilter);
        if (ifilter != null)
            cb.buildCriteria(crit.createCriteria("instance"), ifilter);
        List<HBpelEvent> hevents = crit.list();
        List<BpelEvent> ret = new ArrayList<BpelEvent>(hevents.size());
        try {
            CollectionsX.transformEx(ret, hevents, new UnaryFunctionEx<HBpelEvent, BpelEvent>() {
                public BpelEvent apply(HBpelEvent x) throws Exception {
                    return (BpelEvent) SerializableUtils.toObject(x.getData().getBinary(), BpelEvent.class.getClassLoader());
                }

            });
        } catch (Exception ex) {
            __log.fatal("Internal error: unable to transform HBpelEvent", ex);
            throw new RuntimeException(ex);
        }
        return ret;
    }

    /**
     * @see org.apache.ode.bpel.dao.BpelDAOConnection#instanceQuery(String)
     */
    @SuppressWarnings("unchecked")
    public Collection<ProcessInstanceDAO> instanceQuery(String expression) {
        Builder<String> builder = BuilderFactory.getInstance().createBuilder();
        final org.apache.ode.ql.tree.nodes.Node rootNode = builder.build(expression);

        HibernateInstancesQueryCompiler compiler = new HibernateInstancesQueryCompiler();

        CommandEvaluator<List, Session> eval = compiler.compile((Query) rootNode);
        List<HProcessInstance> instancesList = (List<HProcessInstance>) eval.evaluate(_session);

        Collection<ProcessInstanceDAO> result = new ArrayList<ProcessInstanceDAO>(instancesList.size());
        for (HProcessInstance instance : instancesList) {
            result.add(getInstance(instance.getId()));
        }
        return result;
    }

}
