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
 * @author: yuanjinzhong
 * @date: 2024/8/29 20:09
 * @description:
 */
@SofaService(interfaceType = HelloService.class, bindings = { @SofaServiceBinding(bindingType = "bolt") })
@Service
public class SofaRpcProviderService  implements HelloService {

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
