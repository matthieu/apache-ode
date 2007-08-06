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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

/**
 * Representation of alarm-based event handlers.
 */
public class OnAlarm extends BpelObject {
  public OnAlarm(Element el) {
        super(el);
    }


/**
   * The activity associated with the alarm.
   *
   * @return activity associated with alarm
   */
  public Activity getActivity() {
      return getFirstChild(Activity.class);
  }


  /**
   * Get the duration of the alarm.
   *
   * @return duration of the alarm
   */
  public Expression getFor() {
      return (Expression) getFirstChild(rewriteTargetNS(Bpel20QNames.FOR));
  }


  /**
   * Get the deadline when the alarm goes out of effect.
   *
   * @return deadline when alarm goes out of effect
   */
  public Expression getUntil() {
      return (Expression) getFirstChild(rewriteTargetNS(Bpel20QNames.UNTIL));
      
  }
  
  /**
   * Get the repeatEvery (optional)
   * @return the duration expression that specifies the frequency
   */
  public Expression getRepeatEvery() {
      return (Expression) getFirstChild(rewriteTargetNS(Bpel20QNames.REPEAT_EVERY));
  }
  
  

}
