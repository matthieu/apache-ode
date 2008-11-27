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

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.common.BpelEventFilter;
import org.apache.ode.bpel.common.Filter;
import org.apache.ode.bpel.common.InstanceFilter;
import org.apache.ode.bpel.dao.*;
import org.apache.ode.bpel.evt.BpelEvent;
import org.apache.ode.bpel.evt.ScopeEvent;
import org.apache.ode.utils.ISO8601DateParser;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAQuery;

/**
 * @author Matthieu Riou <mriou at apache dot org>
 */
public class BPELDAOConnectionImpl implements BpelDAOConnection {
	
	static final Log __log = LogFactory.getLog(BPELDAOConnectionImpl.class);
	
	EntityManager _em;

    public BPELDAOConnectionImpl(EntityManager em) {
        _em = em;
    }

    public List<BpelEvent> bpelEventQuery(InstanceFilter ifilter,
                                          BpelEventFilter efilter) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public List<Date> bpelEventTimelineQuery(InstanceFilter ifilter,
                                             BpelEventFilter efilter) {
        // TODO
        throw new UnsupportedOperationException();
    }
	
	public ProcessInstanceDAO getInstance(Long iid) {
        ProcessInstanceDAOImpl instance = _em.find(ProcessInstanceDAOImpl.class, iid);
        return instance;
    }

    public void close() {
        _em = null;
    }

    public MessageExchangeDAO createMessageExchange(String mexId, char dir) {
        MessageExchangeDAOImpl ret = new MessageExchangeDAOImpl(mexId, dir);
        
        _em.persist(ret);
        return ret;
    }

    public ProcessDAO createProcess(QName pid, QName type, String guid, long version) {
        ProcessDAOImpl ret = new ProcessDAOImpl(pid,type,guid,version);
        _em.persist(ret);
        return ret;
    }

    public ProcessDAO getProcess(QName processId) {
        List l = _em.createQuery("select x from ProcessDAOImpl x where x._processId = ?1")
                .setParameter(1, processId.toString()).getResultList();
        if (l.size() == 0) return null;
        ProcessDAOImpl p = (ProcessDAOImpl) l.get(0);
        return p;
    }

    public ScopeDAO getScope(Long siidl) {
        return _em.find(ScopeDAOImpl.class, siidl);
    }

    public void insertBpelEvent(BpelEvent event, ProcessDAO process, ProcessInstanceDAO instance) {
        EventDAOImpl eventDao = new EventDAOImpl();
        eventDao.setTstamp(new Timestamp(System.currentTimeMillis()));
        eventDao.setType(BpelEvent.eventName(event));
        String evtStr = event.toString();
        eventDao.setDetail(evtStr.substring(0, Math.min(254, evtStr.length())));
        if (process != null)
            eventDao.setProcess((ProcessDAOImpl) process);
        if (instance != null)
            eventDao.setInstance((ProcessInstanceDAOImpl) instance);
        if (event instanceof ScopeEvent)
            eventDao.setScopeId(((ScopeEvent) event).getScopeId());
        eventDao.setEvent(event);
        _em.persist(eventDao);
	}
    
    private static String dateFilter(String filter) {
        String date = Filter.getDateWithoutOp(filter);
        String op = filter.substring(0,filter.indexOf(date));
        Date dt = null;
        try {
            dt = ISO8601DateParser.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Timestamp ts = new Timestamp(dt.getTime());
        return op + " '" + ts.toString() + "'";
    }

	@SuppressWarnings("unchecked")
    public Collection<ProcessInstanceDAO> instanceQuery(InstanceFilter criteria) {
        StringBuffer query = new StringBuffer();
        query.append("select pi from ProcessInstanceDAOImpl as pi");

        if (criteria != null) {
            // Building each clause
            ArrayList<String> clauses = new ArrayList<String>();

            // iid filter
            if ( criteria.getIidFilter() != null ) {
            	// Looks to me like the string has been offseted so the operator can be ignored,
            	clauses.add(" pi._instanceId = " + criteria.getIidFilter() );
            }
            
            // pid filter
            if (criteria.getPidFilter() != null)
                clauses.add(" pi._process._processId = '" + criteria.getPidFilter() + "'");
            
            // name filter
            if (criteria.getNameFilter() != null) {
                String val = criteria.getNameFilter();
                if (val.endsWith("*")) {
                    val = val.substring(0, val.length()-1) + "%";
                }
                //process type string begins with name space
                //this could possibly match more than you want
                //because the name space and name are stored together 
                clauses.add(" pi._process._processType like '%" + val + "'");
            }
            
            // name space filter
            if (criteria.getNamespaceFilter() != null) {
                //process type string begins with name space
                //this could possibly match more than you want
                //because the name space and name are stored together
                clauses.add(" pi._process._processType like '{" + 
                        criteria.getNamespaceFilter() + "%'");
            }
            
            // started filter
            if (criteria.getStartedDateFilter() != null) {
                for ( String ds : criteria.getStartedDateFilter() ) {
                    clauses.add(" pi._dateCreated " + dateFilter(ds));
                }
            }
            
            // last-active filter
            if (criteria.getLastActiveDateFilter() != null) {
                for ( String ds : criteria.getLastActiveDateFilter() ) {
                    clauses.add(" pi._lastActive " + dateFilter(ds));
                }
            }
            
            // status filter
            if (criteria.getStatusFilter() != null) {
                StringBuffer filters = new StringBuffer();
                List<Short> states = criteria.convertFilterState();
                for (int m = 0; m < states.size(); m++) {
                    filters.append(" pi._state = ").append(states.get(m));
                    if (m < states.size() - 1) filters.append(" or");
                }
                clauses.add(" (" + filters.toString() + ")");
            }
            
            // $property filter
            if (criteria.getPropertyValuesFilter() != null) {
                Map<String,String> props = criteria.getPropertyValuesFilter();
                // join to correlation sets
                query.append(" inner join pi._rootScope._correlationSets as cs");            
                int i = 0;
                for (String propKey : props.keySet()) {
                    i++;
                    // join to props for each prop
                    query.append(" inner join cs._props as csp"+i);
                    // add clause for prop key and value
                    clauses.add(" csp"+i+".propertyKey = '"+propKey+
                            "' and csp"+i+".propertyValue = '"+
                            // spaces have to be escaped, might be better handled in InstanceFilter
                            props.get(propKey).replaceAll("&#32;", " ")+"'");
                }
            }
            
            // order by
            StringBuffer orderby = new StringBuffer("");
            if (criteria.getOrders() != null) {
                orderby.append(" order by");
                List<String> orders = criteria.getOrders();
                for (int m = 0; m < orders.size(); m++) {
                    String field = orders.get(m);
                    String ord = " asc";
                    if (field.startsWith("-")) {
                        ord = " desc";
                    }
                    String fieldName = " pi._instanceId";
                    if ( field.endsWith("name") || field.endsWith("namespace")) {
                        fieldName = " pi._process._processType";
                    }
                    if ( field.endsWith("version")) {
                        fieldName = " pi._process._version";
                    }
                    if ( field.endsWith("status")) {
                        fieldName = " pi._state";
                    }
                    if ( field.endsWith("started")) {
                        fieldName = " pi._dateCreated";
                    }
                    if ( field.endsWith("last-active")) {
                        fieldName = " pi._lastActive";
                    }
                    orderby.append(fieldName + ord);
                    if (m < orders.size() - 1) orderby.append(", ");
                }

            }

            // Preparing the statement
            if (clauses.size() > 0) {
                query.append(" where");
                for (int m = 0; m < clauses.size(); m++) {
                    query.append(clauses.get(m));
                    if (m < clauses.size() - 1) query.append(" and");
                }
            }
            
            query.append(orderby);
        }
        
        if (__log.isDebugEnabled()) {
        	__log.debug(query.toString());
        }
        
        // criteria limit
        Query pq = _em.createQuery(query.toString());
        OpenJPAQuery kq = OpenJPAPersistence.cast(pq);
        kq.getFetchPlan().setFetchBatchSize(criteria.getLimit());       
        List<ProcessInstanceDAO> ql = pq.getResultList();
       
        Collection<ProcessInstanceDAO> list = new ArrayList<ProcessInstanceDAO>();
        int num = 0;       
        for (Iterator iterator = ql.iterator(); iterator.hasNext();) {
            if(num++ > criteria.getLimit()) break;
            ProcessInstanceDAO processInstanceDAO = (ProcessInstanceDAO) iterator
                    .next();
            list.add(processInstanceDAO);            
        }     
        
        return list;
	}

   
	public Collection<ProcessInstanceDAO> instanceQuery(String expression) {
	    return instanceQuery(new InstanceFilter(expression));
	}

	public void setEntityManger(EntityManager em) {
		_em = em;
	}
	
    public MessageExchangeDAO getMessageExchange(String mexid) {
        List l = _em.createQuery("select x from MessageExchangeDAOImpl x where x._id = ?1")
        .setParameter(1, mexid).getResultList();
        if (l.size() == 0) return null;
        MessageExchangeDAOImpl m = (MessageExchangeDAOImpl) l.get(0);
        return m;
    }

    public ResourceRouteDAO getResourceRoute(String url, String method) {
        List l = _em.createQuery("select r from ResourceRouteDAOImpl r where r._url = ?1 and r._method = ?2")
            .setParameter(1, url).setParameter(2, method).getResultList();
        if (l.size() == 0) return null;
        ResourceRouteDAOImpl m = (ResourceRouteDAOImpl) l.get(0);
        return m;
    }

    public EntityManager getEntityManager() {
        return _em;
    }
}
