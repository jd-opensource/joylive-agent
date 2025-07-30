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
package com.jd.live.agent.plugin.transmission.httpclient.v4_0.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transmission.httpclient.v4_0.contidion.ConditionalOnHttpClient40TransmissionEnabled;
import com.jd.live.agent.plugin.transmission.httpclient.v4_0.interceptor.HttpClientInterceptor;

@Injectable
@Extension(value = "HttpClientDefinition_v4.0", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnHttpClient40TransmissionEnabled
public class HttpClientDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_ABSTRACT_HTTP_CLIENT = "org.apache.http.impl.client.AbstractHttpClient";

    // 4.2+
    private static final String TYPE_AUTO_RETRY_HTTP_CLIENT = "org.apache.http.impl.client.AutoRetryHttpClient";

    // 4.2+
    protected static final String TYPE_DECOMPRESSING_HTTP_CLIENT = "org.apache.http.impl.client.DecompressingHttpClient";

    private static final String METHOD = "execute";

    private static final String[] ARGUMENTS = new String[]{
            "org.apache.http.HttpHost",
            "org.apache.http.HttpRequest",
            "org.apache.http.protocol.HttpContext"
    };

    @Inject(value = Propagation.COMPONENT_PROPAGATION, component = true)
    private Propagation propagation;

    public HttpClientDefinition() {
        this.matcher = () -> MatcherBuilder.in(TYPE_ABSTRACT_HTTP_CLIENT, TYPE_AUTO_RETRY_HTTP_CLIENT, TYPE_DECOMPRESSING_HTTP_CLIENT);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.in(METHOD).and(MatcherBuilder.arguments(ARGUMENTS)),
                        () -> new HttpClientInterceptor(propagation))};
    }
}
