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
package com.jd.live.agent.plugin.registry.eureka.registry;

import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.EurekaTransportConfig;
import jakarta.annotation.Nullable;
import lombok.Getter;

import java.util.List;

public class EurekaRegistryConfig implements EurekaClientConfig {

    private final EurekaClientConfig delegate;

    @Getter
    private final EurekaRegistryPublisher publisher;

    public EurekaRegistryConfig(EurekaClientConfig delegate, EurekaRegistryPublisher publisher) {
        this.delegate = delegate;
        this.publisher = publisher;
    }

    @Override
    public boolean shouldEnforceRegistrationAtInit() {
        return delegate.shouldEnforceRegistrationAtInit();
    }

    @Override
    public boolean shouldEnforceFetchRegistryAtInit() {
        return delegate.shouldEnforceFetchRegistryAtInit();
    }

    @Override
    public boolean shouldUnregisterOnShutdown() {
        return delegate.shouldUnregisterOnShutdown();
    }

    @Override
    public EurekaTransportConfig getTransportConfig() {
        return delegate.getTransportConfig();
    }

    @Override
    public String getExperimental(String name) {
        return delegate.getExperimental(name);
    }

    @Override
    public String getClientDataAccept() {
        return delegate.getClientDataAccept();
    }

    @Override
    public String getDecoderName() {
        return delegate.getDecoderName();
    }

    @Override
    public String getEncoderName() {
        return delegate.getEncoderName();
    }

    @Override
    public boolean shouldOnDemandUpdateStatusChange() {
        return delegate.shouldOnDemandUpdateStatusChange();
    }

    @Override
    public String getEscapeCharReplacement() {
        return delegate.getEscapeCharReplacement();
    }

    @Override
    public String getDollarReplacement() {
        return delegate.getDollarReplacement();
    }

    @Override
    public int getCacheRefreshExecutorExponentialBackOffBound() {
        return delegate.getCacheRefreshExecutorExponentialBackOffBound();
    }

    @Override
    public int getCacheRefreshExecutorThreadPoolSize() {
        return delegate.getCacheRefreshExecutorThreadPoolSize();
    }

    @Override
    public int getHeartbeatExecutorExponentialBackOffBound() {
        return delegate.getHeartbeatExecutorExponentialBackOffBound();
    }

    @Override
    public int getHeartbeatExecutorThreadPoolSize() {
        return delegate.getHeartbeatExecutorThreadPoolSize();
    }

    @Nullable
    @Override
    public String getRegistryRefreshSingleVipAddress() {
        return delegate.getRegistryRefreshSingleVipAddress();
    }

    @Override
    public boolean shouldFetchRegistry() {
        return delegate.shouldFetchRegistry();
    }

    @Override
    public int getEurekaConnectionIdleTimeoutSeconds() {
        return delegate.getEurekaConnectionIdleTimeoutSeconds();
    }

    @Override
    public boolean shouldFilterOnlyUpInstances() {
        return delegate.shouldFilterOnlyUpInstances();
    }

    @Override
    public List<String> getEurekaServerServiceUrls(String myZone) {
        return delegate.getEurekaServerServiceUrls(myZone);
    }

    @Override
    public String[] getAvailabilityZones(String region) {
        return delegate.getAvailabilityZones(region);
    }

    @Override
    public String getRegion() {
        return delegate.getRegion();
    }

    @Nullable
    @Override
    public String fetchRegistryForRemoteRegions() {
        return delegate.fetchRegistryForRemoteRegions();
    }

    @Override
    public boolean shouldDisableDelta() {
        return delegate.shouldDisableDelta();
    }

    @Override
    public boolean shouldLogDeltaDiff() {
        return delegate.shouldLogDeltaDiff();
    }

    @Override
    public boolean allowRedirects() {
        return delegate.allowRedirects();
    }

    @Override
    public boolean shouldPreferSameZoneEureka() {
        return delegate.shouldPreferSameZoneEureka();
    }

    @Override
    public boolean shouldRegisterWithEureka() {
        return delegate.shouldRegisterWithEureka();
    }

    @Override
    public boolean shouldUseDnsForFetchingServiceUrls() {
        return delegate.shouldUseDnsForFetchingServiceUrls();
    }

    @Override
    public String getEurekaServerDNSName() {
        return delegate.getEurekaServerDNSName();
    }

    @Override
    public String getEurekaServerPort() {
        return delegate.getEurekaServerPort();
    }

    @Override
    public String getEurekaServerURLContext() {
        return delegate.getEurekaServerURLContext();
    }

    @Override
    public int getEurekaServerTotalConnectionsPerHost() {
        return delegate.getEurekaServerTotalConnectionsPerHost();
    }

    @Override
    public int getEurekaServerTotalConnections() {
        return delegate.getEurekaServerTotalConnections();
    }

    @Override
    public String getBackupRegistryImpl() {
        return delegate.getBackupRegistryImpl();
    }

    @Override
    public int getEurekaServerConnectTimeoutSeconds() {
        return delegate.getEurekaServerConnectTimeoutSeconds();
    }

    @Override
    public int getEurekaServerReadTimeoutSeconds() {
        return delegate.getEurekaServerReadTimeoutSeconds();
    }

    @Deprecated
    @Override
    public boolean shouldGZipContent() {
        return delegate.shouldGZipContent();
    }

    @Override
    public String getProxyPassword() {
        return delegate.getProxyPassword();
    }

    @Override
    public String getProxyUserName() {
        return delegate.getProxyUserName();
    }

    @Override
    public String getProxyPort() {
        return delegate.getProxyPort();
    }

    @Override
    public String getProxyHost() {
        return delegate.getProxyHost();
    }

    @Override
    public int getEurekaServiceUrlPollIntervalSeconds() {
        return delegate.getEurekaServiceUrlPollIntervalSeconds();
    }

    @Override
    public int getInitialInstanceInfoReplicationIntervalSeconds() {
        return delegate.getInitialInstanceInfoReplicationIntervalSeconds();
    }

    @Override
    public int getInstanceInfoReplicationIntervalSeconds() {
        return delegate.getInstanceInfoReplicationIntervalSeconds();
    }

    @Override
    public int getRegistryFetchIntervalSeconds() {
        return delegate.getRegistryFetchIntervalSeconds();
    }
}
