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
package org.apache.ode.bpel.runtime;

import org.apache.ode.bpel.o.OScope;

/**
 * A scope activity. The scope activity creates a new scope frame and proceeeds
 * using the {@link SCOPE} template. 
 */
public class SCOPEACT extends ACTIVITY {
  private static final long serialVersionUID = -4593029783757994939L;

  public SCOPEACT(ActivityInfo self, ScopeFrame scopeFrame, LinkFrame linkFrame) {
    super(self, scopeFrame, linkFrame);
  }

  public void run() {
    ScopeFrame newFrame = new ScopeFrame(
            (OScope) _self.o,getBpelRuntimeContext().createScopeInstance(_scopeFrame.scopeInstanceId,(OScope) _self.o),
            _scopeFrame,
            null);
    instance(new SCOPE(_self,newFrame, _linkFrame));
  }
}
