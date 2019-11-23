/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.opensymphony.xwork2.interceptor;

import com.opensymphony.xwork2.*;
import com.opensymphony.xwork2.config.entities.ActionConfig;
import com.opensymphony.xwork2.config.providers.MockConfigurationProvider;
import com.opensymphony.xwork2.config.providers.XmlConfigurationProvider;
import org.apache.struts2.dispatcher.HttpParameters;

import java.util.*;


public class ParametersInterceptorGenericActionTest extends XWorkTestCase {

    public void testParameters() throws Exception {
        Map params = new HashMap(){{
                put("item.foo", "foo");
                put("item.bar", "bar");
        }};

        HashMap<String, Object> extraContext = new HashMap<>();
        extraContext.put(ActionContext.PARAMETERS, HttpParameters.create(params).build());

        ActionProxy proxy = actionProxyFactory.createActionProxy("", MockConfigurationProvider.PARAM_INTERCEPTOR_GENERIC_ACTION_NAME, null, extraContext);
        proxy.execute();
        assertEquals("bar", ((ActionBar) proxy.getAction()).getItem().getBar());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        XmlConfigurationProvider provider = new XmlConfigurationProvider("xwork-test-beans.xml");
        container.inject(provider);
        loadConfigurationProviders(provider, new MockConfigurationProvider());

        ActionConfig config = configuration.getRuntimeConfiguration().getActionConfig("", MockConfigurationProvider.PARAM_INTERCEPTOR_GENERIC_ACTION_NAME);
        container.inject(config.getInterceptors().get(0).getInterceptor());
    }

}

