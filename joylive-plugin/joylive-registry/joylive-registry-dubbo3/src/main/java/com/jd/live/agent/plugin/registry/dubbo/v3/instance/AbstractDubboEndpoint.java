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
package com.jd.live.agent.plugin.registry.dubbo.v3.instance;

import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.AbstractServiceEndpoint;

import static com.jd.live.agent.core.Constants.LABEL_GROUP;
import static com.jd.live.agent.core.Constants.LABEL_SERVICE_GROUP;
import static com.jd.live.agent.core.util.StringUtils.EMPTY_STRING;

public abstract class AbstractDubboEndpoint extends AbstractServiceEndpoint {

    @Override
    public String getGroup() {
        if (group == null) {
            group = getLabel(LABEL_GROUP, LABEL_SERVICE_GROUP, EMPTY_STRING);
        }
        return group;
    }

    @Override
    public String getScheme() {
        return "dubbo";
    }

    @Override
    public EndpointState getState() {
        return EndpointState.HEALTHY;
    }

}
