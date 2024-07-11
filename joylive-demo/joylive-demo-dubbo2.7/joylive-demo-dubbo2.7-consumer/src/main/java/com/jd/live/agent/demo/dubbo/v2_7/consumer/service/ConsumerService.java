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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.live.agent.demo.dubbo.v2_7.consumer.config.LiveConfig;
import com.jd.live.agent.demo.response.LiveLocation;
import com.jd.live.agent.demo.response.LiveResponse;
import com.jd.live.agent.demo.response.LiveTrace;
import com.jd.live.agent.demo.response.LiveTransmission;
import com.jd.live.agent.demo.service.HelloService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.service.GenericService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class ConsumerService implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerService.class);

    @Resource
    private LiveConfig config;

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private ObjectMapper objectMapper;

    @DubboReference(group = "live-demo", providedBy = "dubbo2.7-provider")
    private HelloService helloService;

    @DubboReference(interfaceName = "com.jd.live.agent.demo.service.HelloService", providedBy = "dubbo2.7-provider", group = "live-demo", generic = true)
    private GenericService genericService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Thread thread = new Thread(this::invoke);
        thread.setDaemon(true);
        thread.start();
    }

    private void invoke() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        RpcContext context = RpcContext.getContext();
        config.transmit(context::setAttachment);
        long counter = 0;
        long status = 0;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                int remain = (int) (counter++ % 3);
                switch (remain) {
                    case 0:
                        doEcho(context);
                        break;
                    case 1:
                        doGenericEcho(context);
                        break;
                    default:
                        doStatus(context, (status++ % 20) == 0 ? 200 : 500);
                        break;
                }
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
            try {
                countDownLatch.await(1000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void doStatus(RpcContext context, int code) {
        LiveResponse result = helloService.status(code);
        addTrace(context, result);
        output("Invoke status: \n{}", result);
    }

    private void doEcho(RpcContext context) {
        LiveResponse result = helloService.echo("hello");
        addTrace(context, result);
        output("Invoke result: \n{}", result);
    }

    @SuppressWarnings("unchecked")
    private void doGenericEcho(RpcContext context) {
        Map<String, Object> result = (Map<String, Object>) genericService.$invoke("echo",
                new String[]{"java.lang.String"},
                new Object[]{"hello"});
        LiveResponse response = objectMapper.convertValue(result, LiveResponse.class);
        addTrace(context, response);
        output("Generic invoke result: \n{}", response);
    }

    private void addTrace(RpcContext context, LiveResponse result) {
        result.addFirst(new LiveTrace(applicationName, LiveLocation.build(),
                LiveTransmission.build("attachment", context::getAttachment)));
    }

    private void output(String format, LiveResponse result) {
        try {
            logger.info(format, objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException ignore) {
        }
    }
}
