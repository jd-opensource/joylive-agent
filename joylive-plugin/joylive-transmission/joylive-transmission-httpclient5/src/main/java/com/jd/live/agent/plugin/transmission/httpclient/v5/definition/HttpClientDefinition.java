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
package com.jd.live.agent.plugin.transmission.httpclient.v5.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnTransmissionEnabled;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transmission.httpclient.v5.interceptor.HttpClientInterceptor;

@Injectable
@Extension(value = "HttpClientDefinition_v5", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnTransmissionEnabled
@ConditionalOnClass(HttpClientDefinition.TYPE)
public class HttpClientDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.apache.hc.client5.http.impl.classic.CloseableHttpClient";

    private static final String TYPE_CLASSIC_TO_ASYNC_ADAPTOR = "org.apache.hc.client5.http.impl.compat.ClassicToAsyncAdaptor";
    private static final String TYPE_INTERNAL_HTTP_CLIENT = "org.apache.hc.client5.http.impl.classic.InternalHttpClient";
    private static final String TYPE_MINIMAL_HTTP_CLIENT = "org.apache.hc.client5.http.impl.classic.MinimalHttpClient";

    private static final String[] TYPE_CLIENTS = {
            TYPE_CLASSIC_TO_ASYNC_ADAPTOR,
            TYPE_INTERNAL_HTTP_CLIENT,
            TYPE_MINIMAL_HTTP_CLIENT
    };

    private static final String METHOD = "doExecute";

    private static final String[] ARGUMENTS = new String[]{
            "org.apache.hc.core5.http.HttpHost",
            "org.apache.hc.core5.http.ClassicHttpRequest",
            "org.apache.hc.core5.http.protocol.HttpContext"
    };

    @Inject(value = Propagation.COMPONENT_PROPAGATION, component = true)
    private Propagation propagation;

    public HttpClientDefinition() {
        this.matcher = () -> MatcherBuilder.in(TYPE_CLIENTS);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENTS)),
                        () -> new HttpClientInterceptor(propagation))};
    }
}
