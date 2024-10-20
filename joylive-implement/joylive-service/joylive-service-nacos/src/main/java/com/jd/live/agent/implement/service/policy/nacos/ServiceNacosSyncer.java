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

import com.alibaba.nacos.api.config.listener.Listener;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.EventHandler;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.thread.NamedThreadFactory;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.StringUtils;
import com.jd.live.agent.core.util.Waiter;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.PolicyType;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.service.PolicyService;
import com.jd.live.agent.implement.service.policy.nacos.config.NacosSyncConfig;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * ServiceNacosSyncer is responsible for synchronizing live service policies from nacos.
 */
@Injectable
@Extension("ServiceNacosSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_TYPE, value = "nacos")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_SERVICE, matchIfMissing = true)
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class ServiceNacosSyncer extends AbstractNacosSyncer implements PolicyService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceNacosSyncer.class);

    private static final int CONCURRENCY = 5;

    private static final int INTERVALS = 10;

    @Inject(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR)
    private PolicySupervisor policySupervisor;

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(Publisher.POLICY_SUBSCRIBER)
    private Publisher<PolicySubscriber> publisher;

    @Inject(ObjectParser.JSON)
    private ObjectParser jsonParser;

    @Config(SyncConfig.SYNC_LIVE_SPACE)
    private NacosSyncConfig syncConfig = new NacosSyncConfig();

    private ExecutorService executorService;

    private final Queue<PolicySubscriber> subscribers = new ConcurrentLinkedQueue<>();

    private final Waiter.MutexWaiter waiter = new Waiter.MutexWaiter();

    private final EventHandler<PolicySubscriber> handler = this::onEvent;

    private final Map<String, ServiceSyncMeta> versions = new ConcurrentHashMap<>();

    @Override
    public PolicyType getPolicyType() {
        return PolicyType.SERVICE_POLICY;
    }

    @Override
    protected CompletableFuture<Void> doStart() {
        int concurrency = syncConfig.getConcurrency() <= 0 ? CONCURRENCY : syncConfig.getConcurrency();
        executorService = Executors.newFixedThreadPool(concurrency, new NamedThreadFactory(getName(), true));
        publisher.addHandler(handler);
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
        addTasks(policySupervisor.getSubscribers());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        super.doStop();
        publisher.removeHandler(handler);
        waiter.wakeup();
        Close.instance().closeIfExists(executorService, ExecutorService::shutdownNow);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected NacosSyncConfig getSyncConfig() {
        return syncConfig;
    }

    /**
     * Synchronizes and updates the service based on the given subscriber.
     *
     * @param subscriber the policy subscriber to synchronize and update.
     */
    private void syncAndUpdate(PolicySubscriber subscriber) {
        ServiceSyncMeta meta = versions.computeIfAbsent(subscriber.getName(), ServiceSyncMeta::new);
        if (!meta.status.compareAndSet(false, true)) {
            return;
        }
        meta.counter.incrementAndGet();
        try {

            //first: get config
            String configInfo = getConfigService().getConfig(subscriber.getName(),
                    syncConfig.getServiceNacosGroup(),
                    syncConfig.getTimeout());
            if (StringUtils.isNotBlank(configInfo)) {
                onOk(subscriber,parseService(configInfo),meta);
            }

            // then: add listener
            Listener listener = new Listener() {
                @Override
                public Executor getExecutor() {
                    return executorService;
                }
                @Override
                public void receiveConfigInfo(String configInfo) {
                    onOk(subscriber, parseService(configInfo), meta);
                }
            };
            getConfigService().addListener(subscriber.getName(),syncConfig.getServiceNacosGroup(),listener);

        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        meta.status.set(false);
        long delay = syncConfig.getInterval() + (long) (Math.random() * 1000);
        timer.delay(getName() + "-" + subscriber.getName(), delay, () -> addTask(subscriber));
    }

    private Service parseService(String configInfo){
        StringReader reader = new StringReader(configInfo);
        Service service = jsonParser.read(reader, new TypeReference<Service>() {});
        reader.close();
        return service;
    }

    /**
     * Handles successful synchronization with status OK.
     *
     * @param subscriber the policy subscriber.
     * @param service    the service data.
     * @param meta       the service synchronization metadata.
     */
    private void onOk(PolicySubscriber subscriber, Service service, ServiceSyncMeta meta) {
        if (update(subscriber.getName(), service)) {
            meta.version = service.getVersion();
            subscriber.complete(getName());
            logger.info(meta.getSuccessMessage());
        }
    }

    /**
     * Attempts to update the service with retries.
     *
     * @param name    the name of the service.
     * @param service the service data.
     * @return true if the update was successful, false otherwise.
     */
    private boolean update(String name, Service service) {
        for (int i = 0; i < UPDATE_MAX_RETRY; i++) {
            if (updateOnce(name, service)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to update the service once.
     *
     * @param name    the name of the service.
     * @param service the service data.
     * @return true if the update was successful, false otherwise.
     */
    private boolean updateOnce(String name, Service service) {
        return policySupervisor.update(expect -> newPolicy(name, service, expect));
    }

    /**
     * Creates a new policy based on the given service.
     *
     * @param name      the name of the service.
     * @param service   the service data.
     * @param oldPolicy the old policy.
     * @return the new policy.
     */
    private GovernancePolicy newPolicy(String name, Service service, GovernancePolicy oldPolicy) {
        GovernancePolicy result = oldPolicy == null ? new GovernancePolicy() : oldPolicy.copy();
        List<Service> newServices = service == null
                ? result.onDelete(name, syncConfig.getPolicy(), getName())
                : result.onUpdate(service, syncConfig.getPolicy(), getName());
        result.setServices(newServices);
        return result;
    }

    /**
     * Configures the HTTP connection with the specified synchronization configuration.
     *
     * @param config the synchronization configuration.
     * @param conn   the HTTP connection to be configured.
     */
    private void configure(SyncConfig config, HttpURLConnection conn) {
        config.header(conn::setRequestProperty);
        application.sync(conn::setRequestProperty);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout((int) config.getTimeout());
    }

    /**
     * Handles events for policy subscribers.
     *
     * @param events the list of events containing policy subscribers.
     */
    private void onEvent(List<Event<PolicySubscriber>> events) {
        events.forEach(e -> addTask(e.getData(), t -> !versions.containsKey(t.getName())));
    }

    /**
     * Adds a list of policy subscriber tasks to the queue.
     *
     * @param tasks the list of policy subscriber tasks to be added.
     */
    private void addTasks(List<PolicySubscriber> tasks) {
        if (tasks != null) {
            tasks.forEach(task -> addTask(task, t -> !versions.containsKey(t.getName())));
        }
    }

    /**
     * Adds a single policy subscriber task to the queue.
     *
     * @param task the policy subscriber task to be added.
     */
    private void addTask(PolicySubscriber task) {
        addTask(task, null);
    }

    /**
     * Adds a single policy subscriber task to the queue with an optional predicate.
     *
     * @param task      the policy subscriber task to be added.
     * @param predicate an optional predicate to test the task before adding it to the queue.
     */
    private void addTask(PolicySubscriber task, Predicate<PolicySubscriber> predicate) {
        if (task != null
                && task.getType() == PolicyType.SERVICE_POLICY
                && isStarted()
                && (predicate == null || predicate.test(task))) {
            subscribers.add(task);
            waiter.wakeup();
        }
    }

    /**
     * Metadata for synchronizing services.
     */
    private static class ServiceSyncMeta {

        protected final String name;

        protected long version;

        protected final AtomicLong counter = new AtomicLong();

        protected final AtomicBoolean status = new AtomicBoolean(false);

        ServiceSyncMeta(String name) {
            this.name = name;
        }

        /**
         * Generates a success message for the synchronization.
         *
         * @return the success message.
         */
        public String getSuccessMessage() {
            return "Success synchronizing service policy from Nacos. service=" + name
                    + ", counter=" + counter.get();
        }
    }
}
