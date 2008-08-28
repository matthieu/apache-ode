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

import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.wsdl.Operation;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.ode.bpel.compiler.api.CompilationException;
import org.apache.ode.bpel.compiler.v2.CompilerContext;
import org.apache.ode.bpel.compiler.v2.ExpressionCompiler;
import org.apache.ode.bpel.rapi.ExtensionValidator;
import org.apache.ode.bpel.compiler.bom.*;
import org.apache.ode.bpel.compiler.v2.xpath10.XPath10ExpressionCompilerBPEL11;
import org.apache.ode.bpel.compiler.v2.xpath10.XPath10ExpressionCompilerBPEL20;
import org.apache.ode.bpel.compiler.v2.xpath10.XPath10ExpressionCompilerBPEL20Draft;
import org.apache.ode.bpel.compiler.v2.xpath20.XPath20ExpressionCompilerBPEL20;
import org.apache.ode.bpel.compiler.v2.xpath20.XPath20ExpressionCompilerBPEL20Draft;
import org.apache.ode.bpel.rtrep.v2.*;
import org.apache.ode.utils.DOMUtils;
import org.apache.ode.utils.NSContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class XPathTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testEmptyXPath10StringBPEL11() throws Exception {
		XPath10ExpressionCompilerBPEL11 compiler = new XPath10ExpressionCompilerBPEL11();
		assertCompilationExceptionForEmptyXPath(compiler);
	}

	public void testEmptyXPath10StringBPEL20Draft() throws Exception {
		XPath10ExpressionCompilerBPEL20Draft compiler = new XPath10ExpressionCompilerBPEL20Draft();
		assertCompilationExceptionForEmptyXPath(compiler);
	}

	public void testEmptyXPath10StringBPEL20() throws Exception {
		XPath10ExpressionCompilerBPEL20 compiler = new XPath10ExpressionCompilerBPEL20();
		assertCompilationExceptionForEmptyXPath(compiler);
	}

	public void testEmptyXPath20StringBPEL20Draft() throws Exception {
		XPath20ExpressionCompilerBPEL20Draft compiler = new XPath20ExpressionCompilerBPEL20Draft();
		assertCompilationExceptionForEmptyXPath(compiler);
	}

	public void testEmptyXPath20StringBPEL20() throws Exception {
		XPath20ExpressionCompilerBPEL20 compiler = new XPath20ExpressionCompilerBPEL20();
		assertCompilationExceptionForEmptyXPath(compiler);
	}

	private void assertCompilationExceptionForEmptyXPath(
			ExpressionCompiler compiler) throws SAXException, IOException {
		compiler.setCompilerContext(new MockCompilerContext());
		Element element = DOMUtils.stringToDOM("<condition> </condition>");
		Expression xpath = new Expression(element);
		try {
			compiler.compile(xpath);
			throw new Exception("Empty string is invalid XPath");
		} catch (Exception except) {
			// System.out.println("Empty XPath caused: " + except.getClass());
			// except.printStackTrace();
			assertTrue("Expected a CompilationException",
					except instanceof CompilationException);
		}
	}
}

class MockCompilerContext implements CompilerContext {
	private OProcess _oprocess = new OProcess("20");

	private Map<String, OScope.Variable> _vars = new HashMap<String, OScope.Variable>();

	public OExpression constantExpr(boolean value) {
		return null;
	}

	public OExpression compileJoinCondition(Expression expr)
			throws CompilationException {
		return null;
	}

	public OExpression compileExpr(Expression expr) throws CompilationException {
		return null;
	}

	public OLValueExpression compileLValueExpr(Expression expr)
			throws CompilationException {
		return null;
	}

	public OXslSheet compileXslt(String docStrUri) throws CompilationException {
		return null;
	}

	public OXsdTypeVarType resolveXsdType(QName typeName)
			throws CompilationException {
		return null;
	}

	public OProcess.OProperty resolveProperty(QName name) throws CompilationException {
		return null;
	}

	public OScope.Variable resolveVariable(String name) throws CompilationException {
		return _vars.get(name);
	}

	public List<OScope.Variable> getAccessibleVariables() {
		return new ArrayList<OScope.Variable>(_vars.values());
	}

	public OScope.Variable resolveMessageVariable(String inputVar)
			throws CompilationException {
		return _vars.get(inputVar);
	}

	public OScope.Variable resolveMessageVariable(String inputVar, QName messageType)
			throws CompilationException {
		return _vars.get(inputVar);
	}

	public OMessageVarType.Part resolvePart(OScope.Variable variable, String partname)
			throws CompilationException {
		return ((OMessageVarType) variable.type).parts.get(partname);
	}

	public OActivity compile(Activity child) throws CompilationException {
		// TODO Auto-generated method stub
		return null;
	}

	public OActivity compileSLC(Activity source) throws CompilationException {
		// TODO Auto-generated method stub
		return null;
	}

	public OPartnerLink resolvePartnerLink(String name)
			throws CompilationException {
		// TODO Auto-generated method stub
		return null;
	}

	public Operation resolvePartnerRoleOperation(OPartnerLink partnerLink,
			String operationName) throws CompilationException {
		// TODO Auto-generated method stub
		return null;
	}

	public Operation resolveMyRoleOperation(OPartnerLink partnerLink,
			String operationName) throws CompilationException {
		// TODO Auto-generated method stub
		return null;
	}

	public OProcess.OPropertyAlias resolvePropertyAlias(OScope.Variable variable, QName property)
			throws CompilationException {
		// TODO Auto-generated method stub
		return null;
	}

	public void recoveredFromError(Object where, CompilationException bce)
			throws CompilationException {
	}

	public OLink resolveLink(String linkName) throws CompilationException {
		return null;
	}

	public OScope resolveCompensatableScope(String scopeToCompensate)
			throws CompilationException {
		return null;
	}

	public OProcess getOProcess() throws CompilationException {
		return _oprocess;
	}

	public OScope.CorrelationSet resolveCorrelationSet(String csetName)
			throws CompilationException {
		return null;
	}

	public String getSourceLocation() {
		return null;
	}

	public void compile(OActivity context, BpelObject activity, Runnable run) {
	}

	public boolean isPartnerLinkAssigned(String plink) {
		return false;
	}

	public List<OActivity> getActivityStack() {
		return null;
	}

	public void registerElementVar(String name, QName type) {
		OElementVarType varType = new OElementVarType(getOProcess(), type);
		OScope.Variable var = new OScope.Variable(getOProcess(), varType);
		var.name = name;
		_vars.put(name, var);
	}

	public OExpression compileExpr(String locationstr, NSContext nsContext) {
		return null;
	}

	public OActivity getCurrent() {
		return null;
	}

	public OScope compileSLC(ScopeLikeActivity child, OScope.Variable[] variables) {
		return null;
	}

	public void recoveredFromError(SourceLocation location,
			CompilationException error) {
	}

	public boolean isExtensionDeclared(String namespace) {
		return false;
	}

	public ExtensionValidator getExtensionValidator(QName extensionElementName) {
		return null;
	}

    public OMessageVarType.Part resolveHeaderPart(OScope.Variable variable, String partname) throws CompilationException {
        return null;
    }
}
