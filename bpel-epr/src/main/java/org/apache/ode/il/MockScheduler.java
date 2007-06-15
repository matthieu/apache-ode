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

package org.apache.ode.il;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.iapi.ContextException;
import org.apache.ode.bpel.iapi.Scheduler;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Matthieu Riou <mriou at apache dot org>
 */
public class MockScheduler implements Scheduler {

    private static final Log __log = LogFactory.getLog(MockScheduler.class);

    private JobProcessor _processor;
    private ExecutorService _executorSvc = Executors.newCachedThreadPool();
    private ThreadLocal<Boolean> _transacted = new ThreadLocal<Boolean>();
    private TransactionManager _txm;

    public MockScheduler() {
        _transacted.set(false);
    }

    public MockScheduler(TransactionManager txm) {
        _txm = txm;
        _transacted.set(false);
    }

    ThreadLocal<List<Synchronizer>> _synchros = new ThreadLocal<List<Scheduler.Synchronizer>>() {
        @Override
        protected List<Synchronizer> initialValue() {
            return new ArrayList<Synchronizer>();
        }
    };

    public String schedulePersistedJob(Map<String, Object> detail, Date date) throws ContextException {
        if (date != null) {
            try {
                while(new Date().before(date)) { Thread.sleep(100); }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return scheduleVolatileJob(true, detail);
    }

    public String scheduleVolatileJob(final boolean transacted, final Map<String, Object> detail) throws ContextException {
        registerSynchronizer(new Synchronizer() {
            public void afterCompletion(boolean success) {
                try {
                    if (transacted) {
                        execIsolatedTransaction(new Callable() {
                            public Object call() throws Exception {
                                JobInfo ji = new JobInfo("volatileJob", detail, 0);
                                doExecute(ji);
                                return null;
                            }
                        });
                    } else {
                        JobInfo ji = new JobInfo("volatileJob", detail, 0);
                        doExecute(ji);
                    }
                } catch (Exception e) {
                    throw new ContextException("Failure when starting a new volatile job.", e);
                }
            }
            public void beforeCompletion() { }
        });
        return null;
    }

    public void cancelJob(String arg0) throws ContextException {

    }

    public <T> T execTransaction(Callable<T> transaction) throws Exception, ContextException {
        begin();
        try {
            T retval = transaction.call();
            commit();
            return retval;
        } catch (Throwable t) {
            __log.error("Caught an exception during transaction", t);
            rollback();
            throw new ContextException("Error in tx", t);
        }
    }

    public <T> Future<T> execIsolatedTransaction(final Callable<T> transaction) throws Exception, ContextException {
        return _executorSvc.submit(new Callable<T>() {
            public T call() throws Exception {
                return execTransaction(transaction);
            }
        });
    }

    public boolean isTransacted() {
        if (_txm != null) {
            try {
                return _txm.getTransaction() != null;
            } catch (SystemException e) {
                __log.error("Exception in mock scheduler isTransacted.", e);
                throw new RuntimeException(e);
            }
        }
        else return _transacted.get();
    }

    public void start() {
    }

    public void stop() {
    }

    public void shutdown() {
    }

    public void registerSynchronizer(final Synchronizer synch) throws ContextException {
        if (_txm != null) {
            try {
                _txm.getTransaction().registerSynchronization(new Synchronization() {
                    public void beforeCompletion() {
                        synch.beforeCompletion();
                    }
                    public void afterCompletion(int status) {
                        synch.afterCompletion(status == Status.STATUS_COMMITTED);
                    }
                });
            } catch (Exception e) {
                __log.error("Exception in mock scheduler sync registration.", e);
                throw new RuntimeException(e);
            }
        } else {
            _synchros.get().add(synch);
        }
    }

    public void begin() {
        if (_txm != null) {
            try {
                _txm.begin();
            } catch (Exception e) {
                __log.error("Exception in mock scheduler begin.", e);
                throw new RuntimeException(e);
            }
        } else {
            _synchros.get().clear();
        }
        _transacted.set(Boolean.TRUE);
    }

    public void commit() {
        if (_txm != null) {
            try {
                _txm.commit();
            } catch (Exception e) {
                __log.error("Exception in mock scheduler commit.", e);
                throw new RuntimeException(e);
            }
        } else {
            for (Synchronizer s : _synchros.get())
                try {
                    s.beforeCompletion();
                } catch (Throwable t) {
                }
            for (Synchronizer s : _synchros.get())
                try {
                    s.afterCompletion(true);
                } catch (Throwable t) {
                }

            _synchros.get().clear();
        }
        _transacted.set(Boolean.FALSE);
    }

    public void rollback() {
        if (_txm != null) {
            try {
                _txm.rollback();
            } catch (Exception e) {
                __log.error("Exception in mock scheduler rollback.", e);
                throw new RuntimeException(e);
            }
        } else {
            for (Synchronizer s : _synchros.get())
                try {
                    s.beforeCompletion();
                } catch (Throwable t) {
                }
            for (Synchronizer s : _synchros.get())
                try {
                    s.afterCompletion(false);
                } catch (Throwable t) {
                }
            _synchros.get().clear();
        }
        _transacted.set(Boolean.FALSE);
    }

    private void doExecute(JobInfo ji) {
        JobProcessor processor = _processor;
        if (processor == null)
            throw new RuntimeException("No processor.");
        try {
            processor.onScheduledJob(ji);
        } catch (Exception jpe) {
            throw new RuntimeException("Scheduled transaction failed unexpectedly: transaction will not be retried!.", jpe);
        }
    }

    public void setJobProcessor(JobProcessor processor) throws ContextException {
        _processor = processor;
    }

    public void setExecutorSvc(ExecutorService executorSvc) {
        _executorSvc = executorSvc;
    }
}
