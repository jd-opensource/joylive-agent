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
package com.jd.live.agent.demo.dubbo.v3.provider.service;

import com.jd.live.agent.demo.service.HelloService;
import com.jd.live.agent.demo.util.EchoResponse;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContextAttachment;

@DubboService(group = "live-demo", interfaceClass = HelloService.class)
public class Dubbo3ProviderService implements HelloService {

    @Override
    public String echo(String str) {
        RpcContextAttachment attachment = RpcContext.getServerAttachment();
        return new EchoResponse("dubbo3-provider", "attachment", attachment::getAttachment, str).toString();
    }

}
