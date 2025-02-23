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
package com.jd.live.agent.plugin.application.springboot.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.event.AgentEvent;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import org.springframework.boot.web.embedded.netty.NettyWebServer;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

import java.lang.reflect.Field;

public class SpringCloudHttp3Interceptor extends InterceptorAdaptor {

    private static final String PROTOCOL_HTTP3 = "HTTP3";

    private final Publisher<AgentEvent> publisher;

    public SpringCloudHttp3Interceptor(Publisher<AgentEvent> publisher) {
        this.publisher = publisher;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        NettyWebServer nettyWebServer = (NettyWebServer) ctx.getTarget();
        try {
            Field httpServerField = nettyWebServer.getClass().getDeclaredField("httpServer");
            httpServerField.setAccessible(true);
            HttpServer httpServer = (HttpServer) httpServerField.get(nettyWebServer);
            HttpProtocol[] protocols = httpServer.configuration().protocols();
            for (HttpProtocol protocol : protocols) {
                if (PROTOCOL_HTTP3.equals(protocol.name())) {
                    publisher.offer(AgentEvent.onApplicationStarted("Spring http3 web server started"));
                    break;
                }
            }
        } catch (Exception e) {
            //Not needing to throw exceptions
        }
    }
}
