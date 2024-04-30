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
package com.jd.live.agent.plugin.router.dubbo.v2_6.instance;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.request.ServiceRequest;

import static com.alibaba.dubbo.common.Constants.REMOTE_TIMESTAMP_KEY;

public class DubboEndpoint<T> implements Endpoint {

    private final Invoker<T> invoker;

    private final URL url;

    public DubboEndpoint(Invoker<T> invoker) {
        this.invoker = invoker;
        this.url = invoker.getUrl();
    }

    public Invoker<T> getInvoker() {
        return invoker;
    }

    @Override
    public String getHost() {
        return url.getHost();
    }

    @Override
    public int getPort() {
        return url.getPort();
    }

    @Override
    public Long getTimestamp() {
        try {
            return Long.parseLong(url.getParameter(REMOTE_TIMESTAMP_KEY));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Integer getWeight(ServiceRequest request) {
        int result = url.getMethodParameter(request.getMethod(), KEY_WEIGHT, DEFAULT_WEIGHT);
        if (result > 0) {
            long timestamp = url.getParameter(REMOTE_TIMESTAMP_KEY, 0L);
            if (timestamp > 0L) {
                long uptime = System.currentTimeMillis() - timestamp;
                if (uptime < 0) {
                    result = 1;
                } else {
                    int warmup = url.getParameter(KEY_WARMUP, DEFAULT_WARMUP);
                    if (uptime > 0 && uptime < warmup) {
                        int ww = (int) (uptime / ((float) warmup / result));
                        result = ww < 1 ? 1 : Math.min(ww, result);
                    }
                }
            }
        }
        return Math.max(result, 0);
    }

    @Override
    public String getLabel(String key) {
        return url.getParameter(key);
    }

    @Override
    public EndpointState getState() {
        return invoker.isAvailable() ? EndpointState.HEALTHY : EndpointState.DISABLE;
    }
}
