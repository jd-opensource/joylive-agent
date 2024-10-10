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
package com.jd.live.agent.demo.sofarpc.consumer.service;

import com.alipay.sofa.rpc.api.GenericService;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.runtime.api.annotation.SofaReference;
import com.alipay.sofa.runtime.api.annotation.SofaReferenceBinding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.live.agent.demo.response.LiveLocation;
import com.jd.live.agent.demo.response.LiveResponse;
import com.jd.live.agent.demo.response.LiveTrace;
import com.jd.live.agent.demo.response.LiveTransmission;
import com.jd.live.agent.demo.service.HelloService;
import com.jd.live.agent.demo.sofarpc.consumer.config.LiveConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private ObjectMapper objectMapper;

    @SofaReference(interfaceType = HelloService.class, jvmFirst = false,
            binding = @SofaReferenceBinding(bindingType = "bolt"))
    private HelloService helloService;

    @Resource
    private GenericService genericService;


    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Thread thread = new Thread(this::invoke);
        thread.setDaemon(true);
        thread.start();
    }

    private void invoke() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        RpcInvokeContext context = RpcInvokeContext.getContext();

        config.transmit(context::putRequestBaggage);
        long counter = 0;
        long status = 0;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                int remain = (int) (counter++ % 4);
                switch (remain) {
                    case 0:
                        doEcho(context);
                        break;
                    case 1:
                        doGenericEcho(context);
                        break;
                    case 2:
                        doGenericStatus(context);
                        break;
                    default:
                        doStatus(context, (status++ % 20) == 0 ? 200 : 500);
                        break;
                }
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
            try {
                countDownLatch.await(300L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    private void doStatus(RpcInvokeContext attachment, int code) {
        Object result = genericService.$genericInvoke("status",
                new String[]{"int"},
                new Object[]{code});
        LiveResponse response = objectMapper.convertValue(result, LiveResponse.class);
        addTrace(attachment, response);
        output("Generic invoke status: \n{}", response);
    }

    private void doEcho(RpcInvokeContext attachment) {
        LiveResponse result = helloService.echo("hello");
        addTrace(attachment, result);
        output("Invoke result: \n{}", result);
    }

    private void doGenericEcho(RpcInvokeContext attachment) {
        Object result = genericService.$genericInvoke("echo",
                new String[]{"java.lang.String"},
                new Object[]{"hello"});
        LiveResponse response = objectMapper.convertValue(result, LiveResponse.class);
        addTrace(attachment, response);
        output("Generic invoke result: \n{}", response);
    }

    private void doGenericStatus(RpcInvokeContext attachment) {
        Object result = genericService.$invoke("echo",
                new String[]{"java.lang.String"},
                new Object[]{"hello"});
        LiveResponse response = objectMapper.convertValue(result, LiveResponse.class);
        addTrace(attachment, response);
        output("Generic invoke result: \n{}", response);
    }


    private void addTrace(RpcInvokeContext context, LiveResponse result) {
        result.addFirst(new LiveTrace(applicationName, LiveLocation.build(),
                LiveTransmission.build("attachment", context::getRequestBaggage)));
    }

    private void output(String format, LiveResponse result) {
        try {
            logger.info(format, objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException ignore) {
        }
    }
}
