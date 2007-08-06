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

package org.apache.ode.bpel.compiler.bom;

import org.w3c.dom.Element;

/**
 * Interface for a <code>&lt;completionCondition&gt;</code> as used in a
 * forEach activity.
 */
public class CompletionCondition extends Expression {

    public CompletionCondition(Element el) {
        super(el);
    }

    /**
     * Gets whether the completion count should include all terminated children
     * or only successfully completed ones.
     * 
     * @return counts completed
     */
    public boolean isSuccessfulBranchesOnly() {
        return getAttribute("successfulBranchesOnly", "no").equals("yes");
    }
}
