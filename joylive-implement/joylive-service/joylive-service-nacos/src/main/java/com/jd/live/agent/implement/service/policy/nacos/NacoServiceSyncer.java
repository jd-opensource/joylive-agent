package com.jd.live.agent.implement.service.policy.nacos;

import com.jd.live.agent.governance.service.PolicyService;
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.service.AbstractService;
import  com.jd.live.agent.governance.policy.PolicyType;

import java.util.concurrent.CompletableFuture;

public class NacoServiceSyncer extends AbstractService implements PolicyService, ExtensionInitializer {

    @Override
    public PolicyType getPolicyType(){
        return null;
    }

    @Override
    public void initialize() {

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
