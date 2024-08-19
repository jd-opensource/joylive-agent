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
package com.jd.live.agent.implement.service.policy.microservice;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.EventHandler;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.service.AbstractService;
import com.jd.live.agent.core.thread.NamedThreadFactory;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Waiter;
import com.jd.live.agent.core.util.http.HttpResponse;
import com.jd.live.agent.core.util.http.HttpState;
import com.jd.live.agent.core.util.http.HttpStatus;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.core.util.template.Template;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.PolicyType;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.service.PolicyService;
import com.jd.live.agent.implement.service.policy.microservice.config.MicroServiceSyncConfig;
import com.jd.live.agent.implement.service.policy.microservice.reponse.Error;
import com.jd.live.agent.implement.service.policy.microservice.reponse.Response;

import java.io.IOException;
import java.io.SyncFailedException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * MicroServiceSyncer is responsible for synchronizing microservice policies from a microservice control plane.
 */
@Injectable
@Extension("MicroServiceSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_MICROSERVICE_TYPE, value = "jmsf")
@ConditionalOnProperty(name = SyncConfig.SYNC_MICROSERVICE_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class MicroServiceSyncer extends AbstractService implements PolicyService, ExtensionInitializer {

    private static final Logger logger = LoggerFactory.getLogger(MicroServiceSyncer.class);

    private static final int CONCURRENCY = 5;

    private static final int INTERVALS = 10;

    private static final String SPACE = "space";

    private static final String SERVICE_NAME = "service_name";

    private static final String APPLICATION_NAME = "application";

    private static final String SERVICE_VERSION = "service_version";

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR)
    private PolicySupervisor policySupervisor;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(Publisher.POLICY_SUBSCRIBER)
    private Publisher<PolicySubscriber> publisher;

    @Inject(ObjectParser.JSON)
    private ObjectParser jsonParser;

    @Config(SyncConfig.SYNC_MICROSERVICE)
    private MicroServiceSyncConfig syncConfig = new MicroServiceSyncConfig();

    private ExecutorService executorService;

    private final Queue<PolicySubscriber> subscribers = new ConcurrentLinkedQueue<>();

    private final Waiter.MutexWaiter waiter = new Waiter.MutexWaiter();

    private final EventHandler<PolicySubscriber> handler = this::onEvent;

    private final Map<String, ServiceSyncMeta> versions = new ConcurrentHashMap<>();

    private Template template;

    @Override
    public PolicyType getPolicyType() {
        return PolicyType.SERVICE_POLICY;
    }

    @Override
    public void initialize() {
        template = new Template(syncConfig.getServiceUrl());
    }

    @Override
    protected CompletableFuture<Void> doStart() {
        int concurrency = syncConfig.getConcurrency() <= 0 ? CONCURRENCY : syncConfig.getConcurrency();
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
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        publisher.removeHandler(handler);
        waiter.wakeup();
        Close.instance().closeIfExists(executorService, ExecutorService::shutdownNow);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Synchronizes and updates the service based on the given subscriber.
     *
     * @param subscriber the policy subscriber to synchronize and update.
     */
    private void syncAndUpdate(PolicySubscriber subscriber) {
        ServiceSyncMeta meta = versions.computeIfAbsent(subscriber.getName(),
                k -> new ServiceSyncMeta(subscriber.getNamespace(), subscriber.getName()));
        if (!meta.status.compareAndSet(false, true)) {
            return;
        }
        meta.counter.incrementAndGet();
        try {
            Response<Service> response = getService(subscriber.getName(), meta, syncConfig);
            HttpStatus status = response.getStatus();
            switch (status) {
                case OK:
                    onOk(subscriber, response.getData(), meta);
                    break;
                case NOT_MODIFIED:
                    onNotModified(subscriber, meta);
                    break;
                case NOT_FOUND:
                    onNotFound(subscriber, meta);
                    break;
            }
        } catch (SyncFailedException e) {
            logger.error(e.getMessage());
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        meta.status.set(false);
        long delay = syncConfig.getInterval() + (long) (Math.random() * 1000);
        timer.delay(getName() + "-" + subscriber.getName(), delay, () -> addTask(subscriber));
    }

    /**
     * Handles successful synchronization with status OK.
     *
     * @param subscriber the policy subscriber.
     * @param service    the service data.
     * @param meta       the service synchronization metadata.
     */
    private void onOk(PolicySubscriber subscriber, Service service, ServiceSyncMeta meta) {
        if (service.getName() != null && update(subscriber.getName(), service)) {
            meta.version = service.getVersion();
            subscriber.complete(getName());
            logger.info(meta.getSuccessMessage(HttpStatus.OK));
        }
    }

    /**
     * Handles service is NOT_MODIFIED.
     *
     * @param subscriber the policy subscriber.
     * @param meta       the service synchronization metadata.
     */
    private void onNotModified(PolicySubscriber subscriber, ServiceSyncMeta meta) {
        subscriber.complete(getName());
        if (meta.shouldPrint()) {
            logger.info(meta.getSuccessMessage(HttpStatus.NOT_MODIFIED));
        }
    }

    /**
     * Handles service is NOT_FOUND.
     *
     * @param subscriber the policy subscriber.
     * @param meta       the service synchronization metadata.
     */
    private void onNotFound(PolicySubscriber subscriber, ServiceSyncMeta meta) {
        if (meta.version > 0) {
            if (update(subscriber.getName(), null)) {
                // Retry from version 0 after data is recovered.
                meta.version = 0;
                subscriber.complete(getName());
                logger.info(meta.getSuccessMessage(HttpStatus.NOT_FOUND));
            }
        } else if (subscriber.complete(getName())) {
            logger.info(meta.getSuccessMessage(HttpStatus.NOT_FOUND));
        } else if (meta.shouldPrint()) {
            logger.info(meta.getSuccessMessage(HttpStatus.NOT_FOUND));
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
     * Retrieves the service data from the remote server.
     *
     * @param name   the name of the service.
     * @param meta   the service synchronization metadata.
     * @param config the synchronization configuration.
     * @return the response containing the service data.
     * @throws IOException if an I/O error occurs.
     */
    private Response<Service> getService(String name, ServiceSyncMeta meta, SyncConfig config) throws IOException {
        Map<String, Object> context = new HashMap<>(4);
        // context.put(POLICY_TYPE, name);
        context.put(SPACE, application.getService().getNamespace());
        context.put(APPLICATION_NAME, application.getName());
        context.put(SERVICE_NAME, name);
        context.put(SERVICE_VERSION, String.valueOf(meta.version));
        String uri = template.evaluate(context);
        HttpResponse<Response<Service>> httpResponse = HttpUtils.get(uri,
                conn -> configure(config, conn),
                reader -> jsonParser.read(reader, new TypeReference<Response<Service>>() {
                }));
        if (httpResponse.getStatus() == HttpStatus.OK) {
            Response<Service> response = httpResponse.getData();
            Error error = response.getError();
            HttpStatus status = response.getStatus();
            switch (status) {
                case OK:
                case NOT_MODIFIED:
                case NOT_FOUND:
                    return response;
            }
            throw new SyncFailedException(meta.getErrorMessage(error));
        }
        throw new SyncFailedException(meta.getErrorMessage(httpResponse));
    }

    /**
     * Configures the HTTP connection with the specified synchronization configuration.
     *
     * @param config the synchronization configuration.
     * @param conn   the HTTP connection to be configured.
     */
    private void configure(SyncConfig config, HttpURLConnection conn) {
        config.header(conn::setRequestProperty);
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

        protected final String namespace;

        protected final String name;

        protected long version;

        protected final AtomicLong counter = new AtomicLong();

        protected final AtomicBoolean status = new AtomicBoolean(false);

        ServiceSyncMeta(String namespace, String name) {
            this.namespace = namespace;
            this.name = name;
        }

        /**
         * Determines whether a log message should be printed based on the counter.
         *
         * @return true if a log message should be printed, false otherwise.
         */
        public boolean shouldPrint() {
            return counter.get() % INTERVALS == 1;
        }

        /**
         * Generates a success message for the synchronization.
         *
         * @param status the HTTP status of the synchronization.
         * @return the success message.
         */
        public String getSuccessMessage(HttpStatus status) {
            return "Success synchronizing service policy from jmsf control plane. service=" + name
                    + ", space=" + namespace
                    + ", code=" + status.value()
                    + ", counter=" + counter.get();
        }

        /**
         * Generates an error message for the synchronization.
         *
         * @param reply the HTTP state of the synchronization.
         * @return the error message.
         */
        public String getErrorMessage(HttpState reply) {
            return "Failed to synchronize service policy from jmsf control plane. service=" + name
                    + ", space=" + namespace
                    + ", code=" + reply.getCode()
                    + ", message=" + reply.getMessage()
                    + ", counter=" + counter.get();
        }
    }
}
