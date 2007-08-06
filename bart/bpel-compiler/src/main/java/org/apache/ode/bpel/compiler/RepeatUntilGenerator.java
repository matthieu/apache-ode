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
package org.apache.ode.bpel.compiler;

import org.apache.ode.bpel.compiler.bom.Activity;
import org.apache.ode.bpel.compiler.bom.RepeatUntilActivity;
import org.apache.ode.bpel.o.OActivity;
import org.apache.ode.bpel.o.ORepeatUntil;

/**
 * Generates code for <code>&lt;while&gt;</code> activities.
 */
class RepeatUntilGenerator extends DefaultActivityGenerator {
    public OActivity newInstance(Activity src) {
        return new ORepeatUntil(_context.getOProcess(), _context.getCurrent());
    }

    public void compile(OActivity output, Activity srcx)  {
        ORepeatUntil oru = (ORepeatUntil) output;
        RepeatUntilActivity src = (RepeatUntilActivity)srcx;
        oru.untilCondition = _context.compileExpr(src.getCondition());
        oru.activity = _context.compile(src.getActivity());
    }
}

