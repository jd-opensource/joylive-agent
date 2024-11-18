package com.jd.live.agent.governance.service.sync;

import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.service.AbstractConfigService;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.template.Template;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbstractNacosSyncer is responsible for create/close Nacos Service.
 */
public abstract class AbstractSyncer<K extends SyncKey, T> extends AbstractConfigService {

    @Inject(Application.COMPONENT_APPLICATION)
    protected Application application;

    @Inject(ObjectParser.JSON)
    protected ObjectParser parser;

    protected Syncer<K, T> syncer;

    protected Template template;

    protected final Map<String, Subscription<K, T>> subscriptions = new ConcurrentHashMap<>();

    @Override
    protected CompletableFuture<Void> doStart() {
        try {
            syncer = createSyncer();
            template = createTemplate();
            startSync();
            return CompletableFuture.completedFuture(null);
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        stopSync();
        Close.instance().closeIfExists(syncer, v -> {
            subscriptions.clear();
            v.close();
        });
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns the synchronization configuration for this service syncer.
     *
     * @return The synchronization configuration.
     */
    protected abstract SyncConfig getSyncConfig();

    /**
     * Creates a new instance of the SubscriptionSyncer class.
     *
     * @return A new instance of the SubscriptionSyncer class.
     */
    protected abstract Syncer<K, T> createSyncer();

    /**
     * Starts the synchronization process for the LanSpaces.
     *
     * @throws Exception If an error occurs during the synchronization process.
     */
    protected abstract void startSync() throws Exception;

    /**
     * Stops the synchronization process for the LanSpaces.
     */
    protected void stopSync() {
    }

    /**
     * Creates a new instance of the Template class.
     *
     * @return A new instance of the Template class.
     */
    protected Template createTemplate() {
        return null;
    }

}
