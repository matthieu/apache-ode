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

package org.apache.ode.scheduler.simple;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.utils.stl.CollectionsX;
import org.apache.ode.utils.stl.MemberOfFunction;

/**
 * Implements the "todo" queue and prioritized scheduling mechanism. 
 * 
 * @author mszefler
 * @author Maciej Szefler ( m s z e f l e r @ g m a i l . c o m )
 * 
 */
class SchedulerThread implements Runnable {

    private static final Log __log = LogFactory.getLog(SchedulerThread.class);

    private static final int TODO_QUEUE_INITIAL_CAPACITY = 200;

    /** Jobs ready for immediate execution. */
    private PriorityBlockingQueue<Task> _todo;

    /** Lock for managing the queue */
    private ReentrantLock _lock = new ReentrantLock();

    private Condition _activity = _lock.newCondition();

    private volatile boolean _done;

    private TaskRunner _taskrunner;

    private Thread _thread;

    SchedulerThread(TaskRunner runner) {
        _todo = new PriorityBlockingQueue<Task>(TODO_QUEUE_INITIAL_CAPACITY,
                new JobComparatorByDate());
        _taskrunner = runner;
    }

    void start() {
        if (_thread != null)
            return;

        _done = false;
        _thread = new Thread(this, "OdeScheduler");
        _thread.start();
    }

    /**
     * Shutdown the thread.
     */
    void stop() {
        if (_thread == null)
            return;

        _done = true;
        _lock.lock();
        try {
            _activity.signal();
        } finally {
            _lock.unlock();

        }

        while (_thread != null)
            try {
                _thread.join();
                _thread = null;
            } catch (InterruptedException e) {
                ;
            }

    }

    /**
     * Add a job to the todo queue.
     * 
     * @param job
     */
    void enqueue(Task task) {
        _lock.lock();
        try {
            _todo.add(task);
            _activity.signal();
        } finally {
            _lock.unlock();
        }
    }

    /**
     * Remove a job to the todo queue.
     *
     * @param job
     */
    void dequeue(Task task) {
        _lock.lock();
        try {
            _todo.remove(task);
            _activity.signal();
        } finally {
            _lock.unlock();
        }
    }

    /**
     * Get the size of the todo queue.
     * 
     * @return
     */
    public int size() {
        return _todo.size();
    }

    /**
     * Pop items off the todo queue, and send them to the task runner for processing.
     */
    public void run() {
        while (!_done) {
            _lock.lock();
            try {
                long nextjob;
                while ((nextjob = nextJobTime()) > 0 && !_done)
                    _activity.await(nextjob, TimeUnit.MILLISECONDS);

                if (!_done && nextjob == 0) {
                    Task task = _todo.take();
                    _taskrunner.runTask(task);

                }
            } catch (InterruptedException ex) {
                ; // ignore
            } finally {
                _lock.unlock();
            }
        }
    }

    /**
     * Calculate the time until the next available job.
     * 
     * @return time until next job, 0 if one is one is scheduled to go, and some
     *         really large number if there are no jobs to speak of
     */
    private long nextJobTime() {
        assert _lock.isLocked();

        Task job = _todo.peek();
        if (job == null)
            return Long.MAX_VALUE;

        return Math.max(0, job.schedDate - System.currentTimeMillis());
    }

    /**
     * Remove the tasks of a given type from the list. 
     * @param tasktype type of task
     */
    public void clearTasks(final Class<? extends Task> tasktype) {
        _lock.lock();
        try {
            CollectionsX.remove_if(_todo, new MemberOfFunction<Task>() {
                @Override
                public boolean isMember(Task o) {
                    return tasktype.isAssignableFrom(o.getClass());
                }

            });
        } finally {
            _lock.unlock();
        }
    }
}
