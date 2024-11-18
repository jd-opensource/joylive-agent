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
package com.jd.live.agent.governance.service.sync;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.ConfigEvent.EventType;
import com.jd.live.agent.core.config.ConfigListener;
import com.jd.live.agent.core.config.ConfigWatcher;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.EventHandler;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.exception.SyncException;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.service.ConfigService;
import com.jd.live.agent.core.thread.NamedThreadFactory;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Waiter;
import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.listener.ServiceEvent;
import com.jd.live.agent.governance.policy.service.Service;

import java.io.StringReader;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.jd.live.agent.governance.service.sync.SyncKey.ServiceKey;

/**
 * An abstract class that provides a basic implementation of a service syncer.
 */
public abstract class AbstractServiceSyncer<K extends ServiceKey> extends AbstractSyncer<K, Service> implements ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(AbstractServiceSyncer.class);

    protected static final int CONCURRENCY = 5;

    @Inject(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR)
    protected PolicySupervisor policySupervisor;

    @Inject(Publisher.POLICY_SUBSCRIBER)
    protected Publisher<PolicySubscriber> publisher;

    protected ExecutorService executorService;

    protected final Queue<PolicySubscriber> subscribers = new ConcurrentLinkedQueue<>();

    protected final Waiter.MutexWaiter waiter = new Waiter.MutexWaiter();

    protected final EventHandler<PolicySubscriber> handler = this::onEvent;

    @Override
    public String getType() {
        return ConfigWatcher.TYPE_SERVICE_SPACE;
    }

    @Override
    protected void startSync() throws Exception {
        SyncConfig config = getSyncConfig();
        int concurrency = config.getConcurrency() <= 0 ? CONCURRENCY : config.getConcurrency();
        executorService = Executors.newFixedThreadPool(concurrency, new NamedThreadFactory(getName(), true));
        publisher.addHandler(handler);
        for (int i = 0; i < concurrency; i++) {
            executorService.submit(() -> {
                while (isStarted()) {
                    PolicySubscriber subscriber = subscribers.poll();
                    if (subscriber != null) {
                        syncAndUpdate(subscriber);
                    } else {
                        try {
                            waiter.await(5000, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            });
        }
        addTasks(policySupervisor.getSubscribers());
    }

    @Override
    protected void stopSync() {
        publisher.removeHandler(handler);
        waiter.wakeup();
        Close.instance().closeIfExists(executorService, ExecutorService::shutdownNow);
    }

    /**
     * Creates a new instance of the synchronization engine for the given policy subscribers and service data.
     *
     * @return A new instance of the synchronization engine.
     */
    protected abstract Syncer<K, Service> createSyncer();

    /**
     * Creates a new Subscription object for synchronizing Service objects.
     *
     * @param subscriber The PolicySubscriber for which to create the subscription.
     * @return A new Subscription object for synchronizing Service objects.
     */
    protected Subscription<K, Service> createSubscription(PolicySubscriber subscriber) {
        Subscription<K, Service> result = new Subscription<>(getName(), createServiceKey(subscriber));
        result.setListener(r -> onResponse(result, r));
        return result;
    }

    /**
     * Creates a new ServiceKey object for the given PolicySubscriber.
     *
     * @param subscriber The PolicySubscriber for which to create the ServiceKey object.
     * @return A new ServiceKey object representing the PolicySubscriber.
     */
    protected abstract K createServiceKey(PolicySubscriber subscriber);

    /**
     * Synchronizes and updates the service based on the given subscriber.
     *
     * @param subscriber the policy subscriber to synchronize and update.
     */
    protected void syncAndUpdate(PolicySubscriber subscriber) {
        Subscription<K, Service> subscription = subscriptions.computeIfAbsent(subscriber.getUniqueName(),
                name -> createSubscription(subscriber));
        if (subscription.lock()) {
            try {
                subscription.addCounter();
                syncer.sync(subscription);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            } finally {
                subscription.unlock();
            }
        }
    }

    /**
     * Handles the synchronization response for the given policy subscriber.
     *
     * @param subscription The subscription for which an error occurred.
     * @param response     The synchronization response containing the status and data.
     */
    protected void onResponse(Subscription<K, Service> subscription, SyncResponse<Service> response) {
        if (!isStarted()) {
            return;
        }
        switch (response.getStatus()) {
            case SUCCESS:
                onSuccess(subscription, response.getData());
                break;
            case NOT_FOUND:
                onNotFound(subscription);
                break;
            case NOT_MODIFIED:
                onNotModified(subscription);
                break;
            case ERROR:
                onError(subscription, response.getError(), response.getThrowable());
                break;
        }
    }

    /**
     * Handles a successful synchronization response for the given policy subscriber.
     *
     * @param subscription The subscription for which an error occurred.
     * @param service      The new service data to update the subscriber with.
     */
    protected void onSuccess(Subscription<K, Service> subscription, Service service) {
        if (service == null) {
            onNotFound(subscription);
        } else {
            PolicySubscriber subscriber = subscription.getKey().getSubscriber();
            if (update(subscriber.getName(), service)) {
                subscription.setVersion(service.getVersion());
                subscriber.complete(subscription.getOwner());
                logger.info(subscription.getSuccessMessage(SyncStatus.SUCCESS));
            }
        }
    }

    /**
     * Handles service is NOT_MODIFIED.
     *
     * @param subscription The subscription for which an error occurred.
     */
    protected void onNotModified(Subscription<K, Service> subscription) {
        PolicySubscriber subscriber = subscription.getKey().getSubscriber();
        subscriber.complete(getName());
        if (subscription.shouldPrint()) {
            logger.info(subscription.getSuccessMessage(SyncStatus.NOT_MODIFIED));
        }
    }

    /**
     * Handles service is NOT_FOUND.
     *
     * @param subscription The subscription for which an error occurred.
     */
    protected void onNotFound(Subscription<K, Service> subscription) {
        PolicySubscriber subscriber = subscription.getKey().getSubscriber();
        if (subscription.getVersion() > 0) {
            if (update(subscriber.getName(), null)) {
                // Retry from version 0 after data is recovered.
                subscription.setVersion(0);
                subscriber.complete(subscription.getOwner());
                logger.info(subscription.getSuccessMessage(SyncStatus.NOT_FOUND));
            }
        } else if (subscriber.complete(subscription.getOwner())) {
            logger.info(subscription.getSuccessMessage(SyncStatus.NOT_FOUND));
        } else if (subscription.shouldPrint()) {
            logger.info(subscription.getSuccessMessage(SyncStatus.NOT_FOUND));
        }
    }

    /**
     * Handles errors that occur during the synchronization process for a specific subscription.
     *
     * @param subscription The subscription for which an error occurred.
     * @param error        The error message describing the error that occurred.
     * @param e            The optional Throwable object that caused the error.
     */
    protected void onError(Subscription<K, Service> subscription, String error, Throwable e) {
        e = e != null ? e : new SyncException(error);
        PolicySubscriber subscriber = subscription.getKey().getSubscriber();
        if (subscriber.completeExceptionally(e)) {
            logger.error(subscription.getErrorMessage(SyncStatus.ERROR, error), e);
        } else if (subscription.shouldPrint()) {
            logger.error(subscription.getErrorMessage(SyncStatus.ERROR, error), e);
        }
    }

    /**
     * Attempts to update the service with retries.
     *
     * @param name    the name of the service.
     * @param service the service data.
     * @return true if the update was successful, false otherwise.
     */
    protected boolean update(String name, Service service) {
        ServiceEvent event = ServiceEvent.creator()
                .type(service == null ? EventType.DELETE_ITEM : EventType.UPDATE_ITEM)
                .name(name)
                .value(service)
                .watcher(getName())
                .description("service " + name)
                .build();
        configure(event);
        for (ConfigListener listener : listeners) {
            if (!listener.onUpdate(event)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Configures the synchronization process based on the given service event.
     *
     * @param event The service event that triggered the configuration process.
     */
    protected abstract void configure(ServiceEvent event);

    /**
     * Handles events for policy subscribers.
     *
     * @param events the list of events containing policy subscribers.
     */
    protected void onEvent(List<Event<PolicySubscriber>> events) {
        events.forEach(e -> addTask(e.getData(), t -> !subscriptions.containsKey(t.getName())));
    }

    /**
     * Adds a list of policy subscriber tasks to the queue.
     *
     * @param tasks the list of policy subscriber tasks to be added.
     */
    protected void addTasks(List<PolicySubscriber> tasks) {
        if (tasks != null) {
            tasks.forEach(task -> addTask(task, t -> !subscriptions.containsKey(t.getName())));
        }
    }

    /**
     * Adds a single policy subscriber task to the queue.
     *
     * @param task the policy subscriber task to be added.
     */
    protected void addTask(PolicySubscriber task) {
        addTask(task, null);
    }

    /**
     * Adds a single policy subscriber task to the queue with an optional predicate.
     *
     * @param task      the policy subscriber task to be added.
     * @param predicate an optional predicate to test the task before adding it to the queue.
     */
    protected void addTask(PolicySubscriber task, Predicate<PolicySubscriber> predicate) {
        if (task != null
                && ConfigWatcher.TYPE_SERVICE_SPACE.equals(task.getType())
                && isStarted()
                && (predicate == null || predicate.test(task))) {
            subscribers.add(task);
            waiter.wakeup();
        }
    }

    /**
     * Parses a configuration string into a Service object.
     *
     * @param config The configuration string to parse.
     * @return A Service object representing the parsed configuration, or null if the configuration string is null or empty.
     */
    protected SyncResponse<Service> parse(String config) {
        if (config == null || config.isEmpty()) {
            return new SyncResponse<>(SyncStatus.NOT_FOUND, null);
        }
        Service service = parser.read(new StringReader(config), Service.class);
        return new SyncResponse<>(SyncStatus.SUCCESS, service);
    }

}
