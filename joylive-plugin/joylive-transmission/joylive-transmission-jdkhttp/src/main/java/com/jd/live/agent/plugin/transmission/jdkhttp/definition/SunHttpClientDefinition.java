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
import com.jd.live.agent.core.plugin.definition.*;
import com.jd.live.agent.governance.annotation.ConditionalOnTransmissionEnabled;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transmission.jdkhttp.interceptor.SunHttpClientInterceptor;

/**
 * Defines the instrumentation for intercepting the {@code writeRequests} method
 * of the {@code sun.net.www.http.HttpClient} class. This class configures the
 * conditions under which the {@link SunHttpClientInterceptor} is applied, aiming
 * to monitor or modify HTTP request writing behavior.
 *
 */
@Injectable
@Extension(value = "JdkHttpClientDefinition", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnTransmissionEnabled
@ConditionalOnClass(SunHttpClientDefinition.TYPE_HTTP_CLIENT)
public class SunHttpClientDefinition extends PluginDefinitionAdapter implements PluginImporter {

    public static final String TYPE_HTTP_CLIENT = "sun.net.www.http.HttpClient";

    private static final String METHOD_WRITE_REQUESTS = "writeRequests";

    private static final String[] ARGUMENT_WRITE_REQUESTS = new String[]{
            "sun.net.www.MessageHeader",
            "sun.net.www.http.PosterOutputStream"
    };

    @Inject(Propagation.COMPONENT_PROPAGATION)
    private Propagation propagation;

    public SunHttpClientDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_HTTP_CLIENT);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_WRITE_REQUESTS).
                                and(MatcherBuilder.arguments(ARGUMENT_WRITE_REQUESTS)),
                        () -> new SunHttpClientInterceptor(propagation))};
    }

    @Override
    public String[] getImports() {
        return new String[]{"sun.net.www.http.HttpClient", "sun.net.www.MessageHeader"};
    }
}
