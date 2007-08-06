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
package org.apache.ode.ra;

import javax.resource.cci.Connection;

/**
 * Interface providing access to a ODE domain from "external" entities such as
 * administrative consoles, protocol stacks, and client applications.
 */
public interface OdeConnection extends Connection {

  /**
   * Obtain access to a service provider belonging to this ODE domain through
   * its client session interface.
   *
   * @param serviceProviderUri URI of the Service Provider
   * @return possibly remote proxy to the Service Provider's client session interface.
   */
  public ServiceProviderSession createServiceProviderSession(String serviceProviderUri, Class sessionInterface)
          throws OdeConnectionException;

}
