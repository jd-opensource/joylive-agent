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
package com.jd.live.agent.plugin.application.springboot.v2.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.event.AgentEvent;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnGovernanceEnabled;
import com.jd.live.agent.plugin.application.springboot.v2.interceptor.SpringCloudHttp3Interceptor;

@Injectable
@Extension(value = "SpringApplicationDefinition_v5", order = PluginDefinition.ORDER_APPLICATION)
@ConditionalOnGovernanceEnabled
@ConditionalOnClass(SpringNettyWebServerDefinition.TYPE_SPRING_NETTY_WEB_SERVER)
@ConditionalOnClass(SpringNettyWebServerDefinition.TYPE_REACTOR_HTTP_SERVER)
public class SpringNettyWebServerDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_SPRING_NETTY_WEB_SERVER= "org.springframework.boot.web.embedded.netty.NettyWebServer";
    protected static final String TYPE_REACTOR_HTTP_SERVER= "reactor.netty.http.server.HttpServer";

    private static final String METHOD_START= "start";

    @Inject(Publisher.SYSTEM)
    private Publisher<AgentEvent> publisher;

    public SpringNettyWebServerDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_SPRING_NETTY_WEB_SERVER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD_START),
                        () -> new SpringCloudHttp3Interceptor( publisher)),
        };
    }
}
