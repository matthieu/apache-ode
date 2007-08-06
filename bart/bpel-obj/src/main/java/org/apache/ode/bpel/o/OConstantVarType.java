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
package org.apache.ode.bpel.o;

import org.apache.ode.utils.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Matthieu Riou <mriou at apache dot org>
 */
public class OConstantVarType extends OVarType {
    private String value;
    private transient Node nodeValue;

    public OConstantVarType(OProcess owner, Node value) {
        super(owner);
        this.value = DOMUtils.domToString(value);
    }

    public Node newInstance(Document doc) {
        return getValue();
    }

    public Node getValue() {
        if (nodeValue == null)
            try {
                nodeValue = DOMUtils.stringToDOM(value);
            } catch (Exception e) {
                // Highly unexpected
                throw new RuntimeException(e);
            }
        return nodeValue;
    }
}
