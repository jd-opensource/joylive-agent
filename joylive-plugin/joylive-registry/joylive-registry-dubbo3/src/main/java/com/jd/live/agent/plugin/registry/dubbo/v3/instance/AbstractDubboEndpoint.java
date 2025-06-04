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

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.ServiceRequest;

import java.util.Map;

import static com.jd.live.agent.core.Constants.LABEL_GROUP;
import static com.jd.live.agent.core.Constants.LABEL_SERVICE_GROUP;

public abstract class AbstractDubboEndpoint extends AbstractEndpoint implements ServiceEndpoint {

    protected String service;

    protected String group;

    protected Integer weight;

    @Override
    public String getService() {
        return service;
    }

    @Override
    public String getGroup() {
        if (group == null) {
            Map<String, String> metadata = getMetadata();
            if (metadata != null) {
                group = metadata.getOrDefault(LABEL_GROUP, "");
                if (group.isEmpty()) {
                    group = metadata.getOrDefault(LABEL_SERVICE_GROUP, "");
                }
            } else {
                group = "";
            }
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

    @Override
    public Integer getWeight(ServiceRequest request) {
        if (weight != null) {
            Double value = Converts.getDouble(Constants.LABEL_WEIGHT);
            if (value == null || value < 0) {
                weight = DEFAULT_WEIGHT;
            } else if (value < 1) {
                weight = (int) (value * 100);
            } else {
                weight = value.intValue();
            }
        }
        return weight;
    }
}
