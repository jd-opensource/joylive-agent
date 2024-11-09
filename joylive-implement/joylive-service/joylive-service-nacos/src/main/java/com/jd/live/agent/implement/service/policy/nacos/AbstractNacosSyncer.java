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
package com.jd.live.agent.implement.service.policy.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.jd.live.agent.bootstrap.exception.InitializeException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.service.AbstractService;
import com.jd.live.agent.implement.service.policy.nacos.config.NacosSyncConfig;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * AbstractNacosSyncer is responsible for create/close Nacos Service.
 */
public abstract class AbstractNacosSyncer extends AbstractService implements ExtensionInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AbstractNacosSyncer.class);

    private ConfigService configService;

    /**
     * create Nacos Config Service
     */
    @Override
    public void initialize() {
        if (configService == null) {
            synchronized (this) {
                if (configService == null) {
                    try {
                        logger.debug("initialize nacos config service");
                        Properties properties = new Properties();
                        properties.put(PropertyKeyConst.SERVER_ADDR, getSyncConfig().getServerAddr());
                        properties.put(PropertyKeyConst.NAMESPACE, getSyncConfig().getNamespace());
                        properties.put(PropertyKeyConst.USERNAME, getSyncConfig().getUsername());
                        properties.put(PropertyKeyConst.PASSWORD, getSyncConfig().getPassword());
                        configService = NacosFactory.createConfigService(properties);
                    } catch (Throwable t) {
                        throw new InitializeException("initialize nacos ConfigService failed", t);
                    }
                }
            }
        }
    }

    @Override
    protected CompletableFuture<Void> doStart() {
        initialize();
        return CompletableFuture.completedFuture(null);
    }


    /**
     * close Nacos Config Service
     */
    @Override
    protected CompletableFuture<Void> doStop() {
        try {
            logger.debug("shutdown nacos config service");
            if (configService != null) {
                getConfigService().shutDown();
            }
        } catch (Throwable t) {
            throw new RuntimeException("shutdown nacos config service failed", t);
        }
        return CompletableFuture.completedFuture(null);
    }

    protected ConfigService getConfigService() {
        if (configService == null) {
            initialize();
        }
        return configService;
    }

    protected abstract NacosSyncConfig getSyncConfig();
}
