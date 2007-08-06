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
package org.apache.ode.test;

import org.junit.Ignore;
import org.junit.Test;

public class MessageRouting20Test extends BPELTestAbstract {

	@Ignore("fix test bed for handling ASYNC mex") @Test public void testCorrelation() throws Throwable {
		go("/bpel/2.0/TestCorrelation");
	}

	@Ignore("fix test bed for handling ASYNC mex") @Test public void testCorrelation1() throws Throwable {
		go("/bpel/2.0/TestCorrelation1");
	}

	@Ignore("fix test bed for handling ASYNC mex") @Test public void testCorrelationOpaque() throws Throwable {
		//TODO Fix me, we need to capture the session id to send it in the second test message
		go("/bpel/2.0/TestCorrelationOpaque");
	}

	@Ignore("fix test bed for handling ASYNC mex") @Test public void testDynamicPick() throws Throwable {
		go("/bpel/2.0/TestDynamicPick");
	}

	@Ignore("fix test bed for handling ASYNC mex") @Test public void testInstPick() throws Throwable {
		go("/bpel/2.0/TestInstantiatingPick");
	}

	@Ignore("fix test bed for handling ASYNC mex") @Test public void testStaticOnMessage() throws Throwable {
		go("/bpel/2.0/TestStaticOnMessage");
	}

	@Ignore("fix test bed for handling ASYNC mex") @Test public void testStaticPick() throws Throwable {
		go("/bpel/2.0/TestStaticPick");
	}

	@Ignore("fix test bed for handling ASYNC mex") @Test public void testNegativeCorrelation() throws Throwable {
		/**
		 * This test contains invalid BPEL. There is an instantiating
		 * <receive> and a subsequent <pick> that does not define a correlation
		 * key. The BPEL compiler should throw an exception indicating
		 * the BPEL code error ( verify with spec ).
		 * 
		 * See JIRA ODE-64
		 * 
		 */
		// TODO fix the bug first
		negative("target/test-classes/bpel/2.0/NegativeCorrelationTest");
	}

	@Test @Ignore("fix test bed for handling ASYNC mex") public void testNegativeInitialization() throws Throwable {
		/**
		 * This test contains invalid BPEL. There is an instantiating
		 * <receive> within a <scope>. The <scope> contains eventhandlers
		 * that reference the correlation set found on the receive. The BPEL
		 * compiler should throw an exception indicating
		 * the BPEL error ( verify with spec ) or at runtime
		 * a clear initialization exception should be thrown.
		 * 
		 * See JIRA ODE-61.
		 * 
		 * The message exchange should return with a Fault/Failure.
		 * 
		 */
		// TODO fix the bug first
		negative("target/test-classes/bpel/2.0/NegativeInitializationTest");
	}

}
