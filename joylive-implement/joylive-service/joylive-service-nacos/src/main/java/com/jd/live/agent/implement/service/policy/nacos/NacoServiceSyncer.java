package com.jd.live.agent.implement.service.policy.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.jd.live.agent.governance.service.PolicyService;
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.service.AbstractService;
import com.jd.live.agent.governance.policy.PolicyType;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class NacoServiceSyncer extends AbstractService implements PolicyService, ExtensionInitializer {

    @Override
    public PolicyType getPolicyType() {
        return null;
    }

    @Override
    public void initialize() {
        String serverAddr = "{serverAddr}";
        String dataId = "{dataId}";
        String group = "{group}";
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        try {
            ConfigService configService = NacosFactory.createConfigService(properties);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected CompletableFuture<Void> doStart() {
        return null;
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        return null;
    }
}
