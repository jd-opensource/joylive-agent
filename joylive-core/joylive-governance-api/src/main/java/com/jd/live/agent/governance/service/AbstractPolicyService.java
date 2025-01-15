package com.jd.live.agent.governance.service;

import com.jd.live.agent.governance.subscription.policy.PolicyEvent;
import com.jd.live.agent.governance.subscription.policy.PolicyListener;
import com.jd.live.agent.core.service.AbstractService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AbstractConfigService is responsible for add/remove config listener.
 */
public abstract class AbstractPolicyService extends AbstractService implements PolicyService {

    protected final List<PolicyListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addListener(String type, PolicyListener listener) {
        if (getType().equals(type) && listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(String type, PolicyListener listener) {
        if (getType().equals(type) && listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Publishes a ConfigEvent to all registered listeners.
     *
     * @param event The ConfigEvent to publish.
     */
    protected void publish(PolicyEvent event) {
        listeners.forEach(o -> o.onUpdate(event));
    }

}
