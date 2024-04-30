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
package com.jd.live.agent.demo.v3.consumer;

import com.jd.live.agent.demo.service.HelloService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContextAttachment;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

@SpringBootApplication
@Service
@EnableDubbo
public class Dubbo3Consumer {

    @DubboReference(providedBy = "dubbo3-provider", group = "live-demo")
    private HelloService helloService;

    @DubboReference(interfaceClass = HelloService.class, providedBy = "dubbo3-provider", group = "live-demo")
    private GenericService genericService;

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Dubbo3Consumer.class, args);
        Dubbo3Consumer application = context.getBean(Dubbo3Consumer.class);
        String world = "world";
        String user = "dev-1";
        int ruleId = 1003;
        application.invoke(user, ruleId, world);
    }

    protected void invoke(String user, int ruleId, String message) {
        RpcContextAttachment attachment = RpcContext.getClientAttachment();
        attachment.setAttachment("x-live-uid", user);
        attachment.setAttachment("x-live-rule-id", ruleId);
        System.out.println("x-live-uid=" + user + ", x-live-rule-id=" + ruleId);
        String result = helloService.echo(message);
        System.out.println("invoke result: " + result);
        result = (String) genericService.$invoke("echo", new String[]{"java.lang.String"}, new Object[]{message});
        System.out.println("generic invoke result: " + result);
    }

}
