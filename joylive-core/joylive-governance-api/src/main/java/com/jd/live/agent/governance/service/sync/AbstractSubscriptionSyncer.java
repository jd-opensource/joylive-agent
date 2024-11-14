package com.jd.live.agent.governance.service.sync;

import com.jd.live.agent.core.config.ConfigEvent;
import com.jd.live.agent.core.config.ConfigListener;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.service.ConfigService;
import com.jd.live.agent.core.service.sync.AbstractService;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.template.Template;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AbstractNacosSyncer is responsible for create/close Nacos Service.
 */
public abstract class AbstractSubscriptionSyncer<K extends SyncKey, T> extends AbstractService implements ConfigService {

    @Inject(Application.COMPONENT_APPLICATION)
    protected Application application;

    @Inject(ObjectParser.JSON)
    protected ObjectParser parser;

    protected Syncer<K, T> syncer;

    protected Template template;

    protected final Map<String, Subscription<K, T>> subscriptions = new ConcurrentHashMap<>();

    protected final List<ConfigListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addListener(String type, ConfigListener listener) {
        if (getType().equals(type) && listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(String type, ConfigListener listener) {
        if (getType().equals(type) && listener != null) {
            listeners.remove(listener);
        }
    }

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

    /**
     * Publishes a ConfigEvent to all registered listeners.
     *
     * @param event The ConfigEvent to publish.
     */
    protected void publish(ConfigEvent event) {
        listeners.forEach(o -> o.onUpdate(event));
    }

}
