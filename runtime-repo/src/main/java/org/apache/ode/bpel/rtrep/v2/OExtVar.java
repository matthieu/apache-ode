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


/**
 * Compiled representation of an external variable declaration.
 * 
 * @author Maciej Szefler <mszefler at gmail dot com>
 *
 */
public class OExtVar extends OBase {
    private static final long serialVersionUID = 1L;

    /** 
     * Unique identifier for the external variable. Will be referenced in the deployment descriptor.
     */
    public String externalVariableId;
    
    /** Related variable containing the identifying information. */
    public OScope.Variable related; 
      
    public OExtVar(OProcess owner) {
        super(owner);
    }

}
