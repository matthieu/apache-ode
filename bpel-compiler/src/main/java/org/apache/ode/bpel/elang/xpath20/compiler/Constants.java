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
package org.apache.ode.bpel.elang.xpath20.compiler;

import org.apache.ode.utils.Namespaces;

/**
 * XPath-4-BPEL related constants.
 */
public class Constants {
    /**
     * Extension function bpws:getVariableData('variableName', 'partName'?,
     * 'locationPath'?)
     */
    public static final String EXT_FUNCTION_GETVARIABLEDATA = "getVariableData";

    /**
     * Extension function
     * bpws:getVariableProperty('variableName','propertyName')
     */
    public static final String EXT_FUNCTION_GETVARIABLEPROPRTY = "getVariableProperty";

    /**
     * Extension function bpws:getLinkStatus('getLinkName')
     */
    public static final String EXT_FUNCTION_GETLINKSTATUS = "getLinkStatus";

    /**
     * Extension function bpws:getLinkStatus('getLinkName')
     */
    public static final String EXT_FUNCTION_DOXSLTRANSFORM = "doXslTransform";

    /**
     * Non standard extension function ode:splitToElements(sourceText, 'separator' 'targetLocalName', 'targetNS'?)
     */
    public static final String NON_STDRD_FUNCTION_SPLITTOELEMENTS = "splitToElements";

    public static boolean isBpelNamespace(String uri){
        return Namespaces.WS_BPEL_20_NS.equals(uri) || Namespaces.WSBPEL2_0_FINAL_EXEC.equals(uri)
                || Namespaces.BPEL11_NS.equals(uri);
    }

}
