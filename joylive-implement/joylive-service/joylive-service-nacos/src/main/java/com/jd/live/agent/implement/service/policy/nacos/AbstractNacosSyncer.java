package com.jd.live.agent.implement.service.policy.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.jd.live.agent.bootstrap.exception.InitializeException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.service.AbstractService;
import com.jd.live.agent.implement.service.policy.nacos.config.NacosSyncConfig;

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
                        configService = NacosFactory.createConfigService(getSyncConfig().getServerAddr());
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
