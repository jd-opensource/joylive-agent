package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.filter.FilterChain;

public class LiveCluster {

    private final AbstractCluster cluster;

    public LiveCluster(AbstractCluster cluster) {
        this.cluster = cluster;
    }

    public FilterChain getFilterChain() {
        return cluster.getFilterChain();
    }

    public String getStickyId() {
        // 粘滞连接，当前连接可用
        if (cluster.consumerConfig.isSticky()) {
//            if (lastProviderInfo != null) {
//                ProviderInfo providerInfo = lastProviderInfo;
//                ClientTransport lastTransport = connectionHolder.getAvailableClientTransport(providerInfo);
//                if (lastTransport != null && lastTransport.isAvailable()) {
//                    checkAlias(providerInfo, message);
//                    return providerInfo;
//                }
//            }
        }
        return null;
    }
}
