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
package com.jd.live.agent.governance.service.sync.http;

import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectReader.StringReader;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Daemon;
import com.jd.live.agent.core.util.Waiter;
import com.jd.live.agent.core.util.http.HttpResponse;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.service.sync.SyncKey.HttpSyncKey;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.SyncStatus;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.governance.service.sync.http.HttpWatchEvent.EventType;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * A class that watches for changes to HTTP resources and notifies listeners of those changes.
 */
public class HttpWatcher implements AutoCloseable {

    protected final String name;

    protected final SyncConfig config;

    protected final Application application;

    protected final Map<HttpResource, HttpListener> subscriptions = new ConcurrentHashMap<>();

    protected final Waiter.MutexWaiter waiter = new Waiter.MutexWaiter();

    protected final AtomicBoolean started = new AtomicBoolean(true);

    protected final AtomicLong counter = new AtomicLong();

    protected final Daemon daemon;

    public HttpWatcher(String name, SyncConfig config, Application application) {
        this.name = name;
        this.config = config;
        this.application = application;
        this.daemon = Daemon.builder()
                .name(name)
                .delay(config.getDelay())
                .fault(config.getFault())
                .interval(config.getInterval())
                .waiter(waiter)
                .condition(this::isStarted)
                .runnable(this::run)
                .build();
        this.daemon.start();
    }

    @Override
    public void close() throws Exception {
        if (started.compareAndSet(true, false)) {
            waiter.wakeup();
            Close.instance().close(daemon);
        }
    }

    /**
     * Subscribes the specified listener to changes for the specified resource.
     *
     * @param resource The resource to subscribe to.
     * @param listener The listener to notify of changes.
     */
    public void subscribe(HttpResource resource, HttpListener listener) {
        if (resource != null && listener != null) {
            if (subscriptions.putIfAbsent(resource, listener) == null) {
                request(resource, listener);
            }
        }
    }

    /**
     * Unsubscribes the specified resource from the list of subscribed resources.
     *
     * @param resource The resource to unsubscribe.
     */
    public void unsubscribe(HttpResource resource) {
        if (resource != null) {
            subscriptions.remove(resource);
        }
    }

    /**
     * Checks if the synchronization process has been started.
     *
     * @return True if the synchronization process has been started, false otherwise.
     */
    protected boolean isStarted() {
        return started.get();
    }

    /**
     * Sends an HTTP request to the specified resource and returns the response.
     *
     * @param resource The resource to request.
     * @return The HTTP response.
     * @throws IOException If an I/O error occurs during the request.
     */
    protected HttpResponse<String> request(HttpResource resource) throws IOException {
        return HttpUtils.get(resource.getUrl(), this::configure, new StringReader<>());
    }

    /**
     * Sends an HTTP request to the specified resource and notifies the specified listener of the response.
     *
     * @param resource The resource to request.
     * @param listener The listener to notify of the response.
     */
    protected void request(HttpResource resource, HttpListener listener) {
        try {
            HttpResponse<String> response = request(resource);
            switch (response.getStatus()) {
                case OK:
                    listener.onUpdate(new HttpWatchEvent(EventType.UPDATE, resource.getId(), response.getData()));
                    break;
                case NOT_FOUND:
                    listener.onUpdate(new HttpWatchEvent(resource.getId(), new IOException("Failed to request " + resource + ", caused by it's not found.")));
                case NOT_MODIFIED:
                    break;
                default:
                    listener.onUpdate(new HttpWatchEvent(resource.getId(), new IOException("Failed to request " + resource + ", caused by " + response.getMessage())));
                    break;
            }
        } catch (IOException e) {
            listener.onUpdate(new HttpWatchEvent(resource.getId(), e));
        }
    }

    /**
     * The main run method executed by the daemon thread. It performs synchronization and updates the
     * state based on the result.
     */
    protected void run() {
        while (isStarted()) {
            counter.incrementAndGet();
            try {
                for (Map.Entry<HttpResource, HttpListener> entry : subscriptions.entrySet()) {
                    HttpResource resource = entry.getKey();
                    HttpListener listener = entry.getValue();
                    request(resource, listener);
                }
                waiter.await(config.getInterval(), TimeUnit.MILLISECONDS, null);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Configures the HTTP connection with the necessary headers and timeout settings.
     *
     * @param conn the HTTP connection to configure.
     */
    protected void configure(HttpURLConnection conn) {
        config.header(conn::setRequestProperty);
        application.labelSync(conn::setRequestProperty);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout((int) config.getTimeout());
    }

    /**
     * Creates a new Syncer instance for the specified URL and data transformation function.
     *
     * @param function The function to apply to the response data.
     * @param <K>      The type of the synchronization key.
     * @param <T>      The type of the data to synchronize.
     * @return A new Syncer instance.
     */
    public <K extends HttpSyncKey, T> Syncer<K, T> createSyncer(Function<String, SyncResponse<T>> function) {
        return subscription -> {
            try {
                subscribe(subscription.getKey(), event -> {
                    switch (event.getType()) {
                        case UPDATE:
                            subscription.onUpdate(function.apply(event.getData()));
                            break;
                        case DELETE:
                            subscription.onUpdate(new SyncResponse<>(SyncStatus.NOT_FOUND, null));
                            break;
                        case ERROR:
                            subscription.onUpdate(new SyncResponse<>(event.getThrowable()));
                            break;
                    }
                });
            } catch (Throwable e) {
                subscription.onUpdate(new SyncResponse<>(e));
            }
        };
    }
}
