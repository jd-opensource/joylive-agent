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
package com.jd.live.agent.plugin.transmission.jdkhttp.definition;

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
import com.jd.live.agent.plugin.transmission.jdkhttp.interceptor.JavaHttpClientInterceptor;

/**
 * Defines the instrumentation for the Java HTTP Client's HttpRequestBuilderImpl class.
 * This class specifies the conditions under which the {@link JavaHttpClientInterceptor}
 * is applied to modify or monitor HTTP requests during their construction.
 */
@Injectable
@Extension(value = "JavaHttpClientDefinition", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnTransmissionEnabled
@ConditionalOnClass(JavaHttpClientDefinition.TYPE_HTTP_REQUEST_BUILDER_IMPL)
public class JavaHttpClientDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_HTTP_REQUEST_BUILDER_IMPL = "jdk.internal.net.http.HttpRequestBuilderImpl";

    private static final String METHOD_BUILD = "build";

    private static final String METHOD_BUILD_FOR_WEBSOCKET = "buildForWebSocket";

    @Inject(value = Propagation.COMPONENT_PROPAGATION, component = true)
    private Propagation propagation;

    public JavaHttpClientDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_HTTP_REQUEST_BUILDER_IMPL);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.in(METHOD_BUILD, METHOD_BUILD_FOR_WEBSOCKET),
                        () -> new JavaHttpClientInterceptor(propagation))};
    }
}
