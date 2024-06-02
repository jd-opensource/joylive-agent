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
package com.jd.live.agent.demo.dubbo.v2_7.consumer.service;

import com.jd.live.agent.demo.dubbo.v2_7.consumer.config.LiveConfig;
import com.jd.live.agent.demo.service.HelloService;
import com.jd.live.agent.demo.util.EchoResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ConsumerService implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private LiveConfig config;

    @DubboReference(providedBy = "dubbo3-provider", group = "live-demo")
    private HelloService helloService;

    @DubboReference(interfaceClass = HelloService.class, providedBy = "dubbo3-provider", group = "live-demo")
    private GenericService genericService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        invoke();
    }

    protected void invoke() {
        RpcContext context = RpcContext.getContext();
        config.transmit(context::setAttachment);
        String result = helloService.echo("hello");
        EchoResponse response = new EchoResponse("dubbo2.7-consumer", "attachment", context::getAttachment, result);
        System.out.println("invoke result: \n" + response);
        result = (String) genericService.$invoke("echo", new String[]{"java.lang.String"}, new Object[]{"hello"});
        response = new EchoResponse("dubbo2.7-consumer", "attachment", context::getAttachment, result);
        System.out.println("generic invoke result: \n" + response);
    }
}
