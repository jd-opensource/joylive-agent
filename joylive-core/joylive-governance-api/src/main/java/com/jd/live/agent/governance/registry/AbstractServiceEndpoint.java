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
package com.jd.live.agent.governance.registry;

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.governance.instance.AbstractEndpoint;

public abstract class AbstractServiceEndpoint extends AbstractEndpoint implements ServiceEndpoint {

    protected String service;

    protected volatile Boolean secure;

    public AbstractServiceEndpoint() {
    }

    public AbstractServiceEndpoint(String service) {
        this(service, null, null);
    }

    public AbstractServiceEndpoint(String service, String group) {
        this(service, group, null);
    }

    public AbstractServiceEndpoint(String service, String group, Boolean secure) {
        super(group);
        this.service = service;
        this.secure = secure;
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public boolean isSecure() {
        Boolean result = secure;
        if (result == null) {
            result = Boolean.parseBoolean(getLabel(Constants.LABEL_SECURE));
            secure = result;
        }
        return secure;
    }

}
