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
package org.apache.ode.bpel.evt;

import javax.xml.namespace.QName;

public class CorrelationEvent extends ProcessEvent {
  private static final long serialVersionUID = 1L;

  private QName _portType;
  private String _operation;
  private String _mexId;

  public CorrelationEvent() {
    super();
  }

  public CorrelationEvent(QName portType, String operation, String mexId) {
    _portType = portType;
    _operation = operation;
    _mexId = mexId;
  }

  /** Message exchange port type*/
    public QName getPortType() {
    return _portType;
  }

    /** Message exchange operation */
    public String getOperation() {
    return _operation;
  }

  /** Message exchange id */
    public String getMessageExchangeId() {
    return _mexId;
  }

  public void setMexId(String mexId) {
    _mexId = mexId;
  }

  public void setOperation(String operation) {
    _operation = operation;
  }

  public void setPortType(QName portType) {
    _portType = portType;
  }

  public TYPE getType() {
    return TYPE.correlation;
  }
}
