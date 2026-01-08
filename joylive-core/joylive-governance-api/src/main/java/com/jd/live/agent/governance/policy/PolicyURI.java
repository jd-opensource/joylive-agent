/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.governance.policy;

import com.jd.live.agent.core.util.URI;

import java.util.Map;

import static com.jd.live.agent.core.util.StringUtils.EMPTY_STRING;
import static com.jd.live.agent.governance.policy.PolicyId.KEY_SERVICE_GROUP;
import static com.jd.live.agent.governance.policy.PolicyId.KEY_SERVICE_METHOD;

public class PolicyURI extends URI {

    private String serviceGroup;

    private String serviceMethod;

    public PolicyURI() {
    }

    public PolicyURI(String scheme, String host, Integer port, String path, Map<String, String> parameters) {
        super(scheme, host, port, path, parameters);
    }

    public PolicyURI(String scheme, String user, String password, String host, Integer port, String path, Map<String, String> parameters) {
        super(scheme, user, password, host, port, path, parameters);
    }

    protected PolicyURI(String scheme, String user, String password, String host, Integer port, String path, Map<String, String> parameters, String url) {
        super(scheme, user, password, host, port, path, parameters, url);
    }

    public String getServiceGroup() {
        if (serviceGroup == null) {
            String v = getParameter(KEY_SERVICE_GROUP);
            serviceGroup = v == null ? EMPTY_STRING : v;
        }
        return serviceGroup.isEmpty() ? null : serviceGroup;
    }

    public String getServiceMethod() {
        if (serviceMethod == null) {
            String v = getParameter(KEY_SERVICE_METHOD);
            serviceMethod = v == null ? EMPTY_STRING : v;
        }
        return serviceMethod.isEmpty() ? null : serviceMethod;
    }

    @Override
    protected URI create(String scheme, String user, String password, String host, Integer port, String path, Map<String, String> parameters) {
        return new PolicyURI(scheme, user, password, host, port, path, parameters);
    }

    @Override
    protected URI create(String scheme, String user, String password, String host, Integer port, String path, Map<String, String> parameters, String url) {
        return new PolicyURI(scheme, user, password, host, port, path, parameters, url);
    }
}
