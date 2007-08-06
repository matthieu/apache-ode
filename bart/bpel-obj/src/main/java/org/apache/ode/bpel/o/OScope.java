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

import java.util.*;

/**
 * Compiled representation of a BPEL scope. Instances of this class
 * are generated by the BPEL compiler.
 */
public class OScope extends OActivity {
  
    static final long serialVersionUID = -1L  ;

    /** Name of the scope. */
    public String name;

    /** ScopeLikeConstructImpl's fault handler. */
    public OFaultHandler faultHandler;

    /** The activity that gets executed within this scope. */
    public OActivity activity;

    /** ScopeLikeConstructImpl's compensation handler. */
    public OCompensationHandler compensationHandler;
    
    /** ScopeLikeConstructImpl's termination handler. */
    public OTerminationHandler terminationHandler;

    /** ScopeLikeConstructImpl's event handler. */
    public OEventHandler eventHandler;

    /** Variables declared within the scope. */
    public final HashMap<String,Variable> variables = new HashMap<String,Variable>();

    /** OCorrelation sets declared within the scope. */
    public final Map<String,CorrelationSet> correlationSets = new HashMap<String, CorrelationSet>();

    public final Map<String, OPartnerLink> partnerLinks = new HashMap<String,OPartnerLink>();

    /** The descendants of this scope that can be compensated from the FH/CH of this scope. */
    public final Set<OScope> compensatable = new HashSet<OScope>();

    public boolean implicitScope;

    public boolean atomicScope;

    public OScope(OProcess owner, OActivity parent) {
        super(owner, parent);
    }

    /**
     * Obtains the correlation set visible in current scope or parent scope.
     *
     * @param corrName correlation set name
     *
     * @return
     */
    public CorrelationSet getCorrelationSet(String corrName) {
        return correlationSets.get(corrName);
    }

    /**
     *
     * Get a localy-defined variable by name.
     * @param varName name of variable
     *
     * @return
     */
    public Variable getLocalVariable(final String varName) {
        return variables.get(varName);
    }

    public void addLocalVariable(Variable variable) {
        variables.put(variable.name, variable);
    }

    public Variable getVisibleVariable(String varName) {
        OActivity current = this;
        Variable variable;
        while (current != null) {
            if (current instanceof OScope) {
                variable = ((OScope)current).getLocalVariable(varName);
                if (variable != null)
                    return variable;
            }
            current = current.getParent();
        }
        return null;
    }

    public OPartnerLink getLocalPartnerLink(String name) {
        return partnerLinks.get(name);
    }

    public OPartnerLink getVisiblePartnerLink(String name) {
        OActivity current = this;
        OPartnerLink plink;
        while (current != null) {
            if (current instanceof OScope) {
                plink = ((OScope)current).getLocalPartnerLink(name);
                if (plink != null)
                    return plink;
            }
            current = current.getParent();
        }
        return null;
    }

    public void addCorrelationSet(CorrelationSet ocset) {
        correlationSets.put(ocset.name, ocset);
    }

    public boolean isInAtomicScope() {
        OActivity current = this;
        while (current != null) {
            if (current instanceof OScope && ((OScope)current).atomicScope)
                return true;
            current = current.getParent();
        }
        return false;
    }

    public String toString() {
        return "{OScope '" + name + "' id=" + getId() + "}";
    }

    public static final class CorrelationSet extends OBase {
      
        static final long serialVersionUID = -1L  ;
        public String name;
        public OScope declaringScope;
        public final List<OProcess.OProperty>properties = new ArrayList<OProcess.OProperty>();


        public CorrelationSet(OProcess owner) {
            super(owner);
        }

        public String toString() {
            return "{CSet " + name + " " + properties + "}";
        }
    }

    public static final class Variable extends OBase {
        static final long serialVersionUID = -1L  ;
        public String name;
        public OScope declaringScope;
        public OVarType type;

        public Variable(OProcess owner, OVarType type) {
            super(owner);
            this.type = type;
        }

        public String toString() {
            return "{Variable " + getDescription() + ":" + type + "}";
        }

        public String getDescription() {
            StringBuffer buf = new StringBuffer(declaringScope.name);
            buf.append('.');
            buf.append(name);
            return buf.toString();
        }
    }

}
