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
package com.jd.live.agent.demo.sofarpc.provider.service;

import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.runtime.api.annotation.SofaService;
import com.alipay.sofa.runtime.api.annotation.SofaServiceBinding;
import com.jd.live.agent.demo.response.LiveLocation;
import com.jd.live.agent.demo.response.LiveResponse;
import com.jd.live.agent.demo.response.LiveTrace;
import com.jd.live.agent.demo.response.LiveTransmission;
import com.jd.live.agent.demo.service.HelloService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SofaRpcProviderService
 *
 * @author yuanjinzhong
 */
@SofaService(interfaceType = HelloService.class, bindings = {@SofaServiceBinding(bindingType = "bolt")})
@Service
public class SofaRpcProviderService implements HelloService {

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public LiveResponse echo(String str) {

        return createResponse(str);
    }

    @Override
    public LiveResponse status(int code) {
        if (code >= 500) {
            throw new RuntimeException("Code:" + code);
        }
        return createResponse(code);
    }

    private LiveResponse createResponse(Object data) {
        //sofa-rcp invoke-chain-pass-data ：https://www.sofastack.tech/projects/sofa-rpc/invoke-chain-pass-data/
        RpcInvokeContext context = RpcInvokeContext.getContext();
        return new LiveResponse(data).addFirst(new LiveTrace(applicationName, LiveLocation.build(),
                LiveTransmission.build("attachment", context::getRequestBaggage)));
    }
}
