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
package com.jd.live.agent.implement.service.policy.multilive;

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
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.implement.service.policy.multilive.config.LiveSyncConfig;
import com.jd.live.agent.implement.service.policy.multilive.reponse.Error;
import com.jd.live.agent.implement.service.policy.multilive.reponse.Response;

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
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Injectable
@Extension("LiveServiceSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_TYPE, value = "multilive")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_SERVICE, matchIfMissing = true)
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class LiveServiceSyncer extends AbstractService implements ExtensionInitializer {

    private static final Logger logger = LoggerFactory.getLogger(LiveServiceSyncer.class);

    private static final int CONCURRENCY = 5;

    private static final int INTERVALS = 10;

    private static final String SERVICE_NAME = "service_name";

    private static final String SERVICE_VERSION = "service_version";

    @Inject(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR)
    private PolicySupervisor policySupervisor;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(Publisher.POLICY_SUBSCRIBER)
    private Publisher<PolicySubscriber> publisher;

    @Inject(ObjectParser.JSON)
    private ObjectParser jsonParser;

    @Config(SyncConfig.SYNC_LIVE_SPACE)
    private LiveSyncConfig syncConfig = new LiveSyncConfig();

    private ExecutorService executorService;

    private final Queue<PolicySubscriber> subscribers = new ConcurrentLinkedQueue<>();

    private final Waiter.MutexWaiter waiter = new Waiter.MutexWaiter();

    private final EventHandler<PolicySubscriber> handler = this::onEvent;

    private final Map<String, ServiceSyncMeta> versions = new ConcurrentHashMap<>();

    private Template template;

    @Override
    public void initialize() {
        template = new Template(syncConfig.getSpaceUrl());
    }

    @Override
    protected String getName() {
        return "live-service-syncer";
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

    private void syncAndUpdate(PolicySubscriber subscriber) {
        ServiceSyncMeta meta = versions.computeIfAbsent(subscriber.getName(), ServiceSyncMeta::new);
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

    private void onOk(PolicySubscriber subscriber, Service service, ServiceSyncMeta meta) {
        if (update(subscriber.getName(), service)) {
            meta.version = service.getVersion();
            subscriber.complete();
            logger.info(meta.getSuccessMessage(HttpStatus.OK));
        }
    }

    private void onNotModified(PolicySubscriber subscriber, ServiceSyncMeta meta) {
        subscriber.complete();
        if (meta.shouldPrint()) {
            logger.info(meta.getSuccessMessage(HttpStatus.NOT_MODIFIED));
        }
    }

    private void onNotFound(PolicySubscriber subscriber, ServiceSyncMeta meta) {
        if (meta.version > 0) {
            if (update(subscriber.getName(), null)) {
                // Retry from version 0 after data is recovered.
                meta.version = 0;
                subscriber.complete();
                logger.info(meta.getSuccessMessage(HttpStatus.NOT_FOUND));
            }
        } else if (subscriber.complete()) {
            logger.info(meta.getSuccessMessage(HttpStatus.NOT_FOUND));
        } else if (meta.shouldPrint()) {
            logger.info(meta.getSuccessMessage(HttpStatus.NOT_FOUND));
        }
    }

    private boolean update(String name, Service service) {
        for (int i = 0; i < UPDATE_MAX_RETRY; i++) {
            if (updateOnce(name, service)) {
                return true;
            }
        }
        return false;
    }

    private boolean updateOnce(String name, Service service) {
        return policySupervisor.update(expect -> newPolicy(name, service, expect));
    }

    private GovernancePolicy newPolicy(String name, Service service, GovernancePolicy oldPolicy) {
        GovernancePolicy result = oldPolicy == null ? new GovernancePolicy() : oldPolicy.copy();
        BiConsumer<ServicePolicy, ServicePolicy> consumer = (o, n) -> o.setLivePolicy(n == null ? null : n.getLivePolicy());
        List<Service> newServices = result.update(name, service, consumer, getName());
        result.setServices(newServices);
        return result;
    }

    private Response<Service> getService(String name, ServiceSyncMeta meta, SyncConfig config) throws IOException {
        Map<String, Object> context = new HashMap<>(2);
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

    private void configure(SyncConfig config, HttpURLConnection conn) {
        config.header(conn::setRequestProperty);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout((int) config.getTimeout());
    }

    private void onEvent(List<Event<PolicySubscriber>> events) {
        events.forEach(e -> addTask(e.getData(), t -> !versions.containsKey(t.getName())));
    }

    private void addTasks(List<PolicySubscriber> tasks) {
        if (tasks != null) {
            tasks.forEach(task -> addTask(task, t -> !versions.containsKey(t.getName())));
        }
    }

    private void addTask(PolicySubscriber task) {
        addTask(task, null);
    }

    private void addTask(PolicySubscriber task, Predicate<PolicySubscriber> predicate) {
        if (task != null
                && task.getType() == PolicyType.SERVICE_POLICY
                && isStarted()
                && (predicate == null || predicate.test(task))) {
            subscribers.add(task);
            waiter.wakeup();
        }
    }

    private static class ServiceSyncMeta {

        protected final String name;

        protected long version;

        protected final AtomicLong counter = new AtomicLong();

        protected final AtomicBoolean status = new AtomicBoolean(false);

        ServiceSyncMeta(String name) {
            this.name = name;
        }

        public boolean shouldPrint() {
            return counter.get() % INTERVALS == 1;
        }

        public String getSuccessMessage(HttpStatus status) {
            return "Success synchronizing service policy from multilive. service=" + name
                    + ", code=" + status.value()
                    + ", counter=" + counter.get();
        }

        public String getErrorMessage(HttpState reply) {
            return "Failed to synchronize service policy from multilive. service=" + name
                    + ", code=" + reply.getCode()
                    + ", message=" + reply.getMessage()
                    + ", counter=" + counter.get();
        }
    }
}
