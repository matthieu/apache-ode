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

import java.util.Comparator;

/**
 * Compare jobs, using scheduled date as sort criteria.
 * 
 * @author Maciej Szefler ( m s z e f l e r @ g m a i l . c o m )
 */
class JobComparatorByDate implements Comparator<Task> {

    public int compare(Task o1, Task o2) {
        long diff = o1.schedDate - o2.schedDate;
        if (diff < 0) return -1;
        if (diff > 0) return 1;
        return 0;
    }

}
