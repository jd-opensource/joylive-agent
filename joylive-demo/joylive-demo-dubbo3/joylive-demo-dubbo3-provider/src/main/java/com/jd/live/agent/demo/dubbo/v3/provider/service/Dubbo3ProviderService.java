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

import com.jd.live.agent.demo.exception.BreakableException;
import com.jd.live.agent.demo.exception.RetryableException;
import com.jd.live.agent.demo.response.LiveLocation;
import com.jd.live.agent.demo.response.LiveResponse;
import com.jd.live.agent.demo.response.LiveTrace;
import com.jd.live.agent.demo.response.LiveTransmission;
import com.jd.live.agent.demo.service.HelloService;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContextAttachment;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.ThreadLocalRandom;

@DubboService(group = "live-demo", interfaceClass = HelloService.class)
public class Dubbo3ProviderService implements HelloService {

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public LiveResponse echo(String str) {
        return createResponse(str);
    }

    @Override
    public LiveResponse status(int code) {
        if (code == 600) {
            if (ThreadLocalRandom.current().nextInt(2) == 0) {
                throw new RetryableException("Code:" + code);
            }
        } else if (code >= 500) {
            throw new BreakableException("Code:" + code);
        }
        return createResponse(code);
    }

    private LiveResponse createResponse(Object data) {
        RpcContextAttachment attachment = RpcContext.getServerAttachment();
        return new LiveResponse(data).addFirst(new LiveTrace(applicationName, LiveLocation.build(),
                LiveTransmission.build("attachment", attachment::getAttachment)));
    }
}
