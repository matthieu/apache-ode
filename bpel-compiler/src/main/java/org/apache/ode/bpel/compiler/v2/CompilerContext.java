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
package org.apache.ode.bpel.compiler.v2;

import java.util.List;

import javax.wsdl.Operation;
import javax.xml.namespace.QName;

import org.apache.ode.bpel.compiler.bom.Activity;
import org.apache.ode.bpel.compiler.bom.Expression;
import org.apache.ode.bpel.compiler.bom.ScopeLikeActivity;
import org.apache.ode.bpel.compiler.SourceLocation;
import org.apache.ode.bpel.compiler.api.CompilationException;
import org.apache.ode.bpel.rapi.ExtensionValidator;
import org.apache.ode.bpel.rtrep.v2.OActivity;
import org.apache.ode.bpel.rtrep.v2.OExpression;
import org.apache.ode.bpel.rtrep.v2.OLValueExpression;
import org.apache.ode.bpel.rtrep.v2.OLink;
import org.apache.ode.bpel.rtrep.v2.OMessageVarType;
import org.apache.ode.bpel.rtrep.v2.OPartnerLink;
import org.apache.ode.bpel.rtrep.v2.OProcess;
import org.apache.ode.bpel.rtrep.v2.OScope;
import org.apache.ode.bpel.rtrep.v2.OXsdTypeVarType;
import org.apache.ode.bpel.rtrep.v2.OXslSheet;
import org.apache.ode.bpel.rtrep.v2.OScope.Variable;
import org.apache.ode.utils.NSContext;

/**
 * Interface providing access to the compiler.
 */
public interface CompilerContext {

    OExpression constantExpr(boolean value);

    OExpression compileJoinCondition(Expression expr)
            throws CompilationException;

    OExpression compileExpr(Expression expr)
            throws CompilationException;

    OLValueExpression compileLValueExpr(Expression expr)
            throws CompilationException;

    /**
     * BPEL 1.1 legacy. 
     * @param locationstr
     * @param nsContext
     * @return
     * @throws CompilationException
     */
    OExpression compileExpr(String locationstr, NSContext nsContext)
            throws CompilationException;

    OXslSheet compileXslt(String docStrUri)
            throws CompilationException;

    OXsdTypeVarType resolveXsdType(QName typeName)
            throws CompilationException;

    OProcess.OProperty resolveProperty(QName name)
            throws CompilationException;

    OScope.Variable resolveVariable(String name)
            throws CompilationException;

    List<OScope.Variable> getAccessibleVariables();

    OScope.Variable resolveMessageVariable(String inputVar)
            throws CompilationException;

    OScope.Variable resolveMessageVariable(String inputVar, QName messageType)
            throws CompilationException;

    OMessageVarType.Part resolvePart(OScope.Variable variable, String partname)
            throws CompilationException;

    OMessageVarType.Part resolveHeaderPart(OScope.Variable variable, String partname)
            throws CompilationException;

    OActivity compile(Activity child)
            throws CompilationException;

    OScope compileSLC(ScopeLikeActivity child, Variable[] variables);

    OPartnerLink resolvePartnerLink(String name)
            throws CompilationException;

    Operation resolvePartnerRoleOperation(OPartnerLink partnerLink, String operationName)
            throws CompilationException;

    Operation resolveMyRoleOperation(OPartnerLink partnerLink, String operationName)
            throws CompilationException;

    OProcess.OPropertyAlias resolvePropertyAlias(OScope.Variable variable, QName property)
            throws CompilationException;

    void recoveredFromError(SourceLocation where, CompilationException bce)
            throws CompilationException;

    OLink resolveLink(String linkName)
            throws CompilationException;

    OScope resolveCompensatableScope(String scopeToCompensate)
            throws CompilationException;

    OProcess getOProcess()
            throws CompilationException;

    OScope.CorrelationSet resolveCorrelationSet(String csetName)
            throws CompilationException;

    String getSourceLocation();

    boolean isPartnerLinkAssigned(String plink);

    List<OActivity> getActivityStack();

    OActivity getCurrent();

    boolean isExtensionDeclared(String namespace);
    
    //void setExtensionValidators(Map<QName, ExtensionValidator> extensionValidators);
    
    ExtensionValidator getExtensionValidator(QName extensionElementName);
}
