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
package com.jd.live.agent.demo.v3.consumer.service;

import com.jd.live.agent.demo.service.HelloService;
import com.jd.live.agent.demo.util.EchoResponse;
import com.jd.live.agent.demo.v3.consumer.config.LiveConfig;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContextAttachment;
import org.apache.dubbo.rpc.service.GenericService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class ConsumerService implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerService.class);

    @Resource
    private LiveConfig config;

    @DubboReference(providedBy = "dubbo3-provider", group = "live-demo")
    private HelloService helloService;

    @DubboReference(interfaceClass = HelloService.class, providedBy = "dubbo3-provider", group = "live-demo")
    private GenericService genericService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Thread thread = new Thread(this::invoke);
        thread.setDaemon(true);
        thread.start();
    }

    private void invoke() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        RpcContextAttachment attachment = RpcContext.getClientAttachment();
        config.transmit(attachment::setAttachment);
        long counter = 0;
        while (!Thread.currentThread().isInterrupted()) {
            if (counter++ % 2 == 0) {
                doInvoke(attachment);
            } else {
                doGenericInvoke(attachment);
            }
            try {
                countDownLatch.await(3000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignore) {
            }
        }

    }

    private void doInvoke(RpcContextAttachment attachment) {
        String result = helloService.echo("hello");
        EchoResponse response = new EchoResponse("dubbo3-consumer", "attachment", attachment::getAttachment, result);
        logger.info("Invoke result: \n\n{}", response);
    }

    private void doGenericInvoke(RpcContextAttachment attachment) {
        String result;
        EchoResponse response;
        result = (String) genericService.$invoke("echo", new String[]{"java.lang.String"}, new Object[]{"hello"});
        response = new EchoResponse("dubbo3-consumer", "attachment", attachment::getAttachment, result);
        logger.info("Generic invoke result: \n\n{}", response);
    }
}
