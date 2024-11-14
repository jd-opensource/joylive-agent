package com.jd.live.agent.core.service;

import com.jd.live.agent.core.config.ConfigEvent;
import com.jd.live.agent.core.config.ConfigListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AbstractConfigService is responsible for add/remove config listener.
 */
public abstract class AbstractConfigService extends AbstractService implements ConfigService {

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

    /**
     * Publishes a ConfigEvent to all registered listeners.
     *
     * @param event The ConfigEvent to publish.
     */
    protected void publish(ConfigEvent event) {
        listeners.forEach(o -> o.onUpdate(event));
    }

}
