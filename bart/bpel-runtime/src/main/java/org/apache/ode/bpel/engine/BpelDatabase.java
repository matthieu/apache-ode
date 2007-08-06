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

import javax.xml.namespace.QName;

import org.apache.ode.bpel.dao.BpelDAOConnection;
import org.apache.ode.bpel.dao.BpelDAOConnectionFactory;
import org.apache.ode.bpel.iapi.Scheduler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Mostly of historical interest. Provides transactional access to the BPEL database, defines a Callable-style
 * interface for transactions. 
 * 
 * <p>Should probably be eliminated. --mszefler 2007-07-26 </p>
 * 
 * @author Maciej Szefler <mszefler at gmail dot com>
 */
class BpelDatabase {
    static Log __log = LogFactory.getLog(BpelDatabase.class);

    protected BpelDAOConnectionFactory _sscf;

    protected Contexts _contexts;

    BpelDatabase(Contexts contexts) {
        if (contexts == null)
            throw new NullPointerException("scheduler is null!");

        _sscf = contexts.dao;
        _contexts = contexts;

    }

    /**
     * Get a connection to the database with the correct store identifier.
     * 
     * @return a state store connection
     * @throws org.apache.ode.utils.dao.DConnectionException
     */
    BpelDAOConnection getConnection() {
        // Note: this will give us a connection associated with the current
        // transaction, so no need to worry about closing it.
        return _sscf.getConnection();
    }

    BpelProcessDatabase getProcessDb(QName pid) {
        return new BpelProcessDatabase(_contexts, pid);
    }

    /**
     * Execute a self-contained database transaction.
     * 
     * @param callable
     *            database transaction
     * @return
     * @throws DConnectionException
     */
    <T> T exec(final Callable<T> callable) throws Exception {
        return _contexts.execTransaction(new java.util.concurrent.Callable<T>() {
            public T call() throws Exception {
                return callable.run(_sscf.getConnection());
            }
        });
    }

    interface Callable<T> {
        public T run(BpelDAOConnection conn) throws Exception;
    }
}
