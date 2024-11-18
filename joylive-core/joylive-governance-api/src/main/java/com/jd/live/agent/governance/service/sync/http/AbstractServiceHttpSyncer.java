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
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.util.http.HttpResponse;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.core.util.template.Template;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.service.sync.*;
import com.jd.live.agent.governance.service.sync.SyncKey.ServiceKey;
import com.jd.live.agent.governance.service.sync.api.ApiResponse;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An abstract class that provides a base implementation for synchronizing data with an HTTP service.
 */
public abstract class AbstractServiceHttpSyncer<K extends ServiceKey> extends AbstractServiceSyncer<K> {

    protected static final String APPLICATION_NAME = "application";

    protected static final String SPACE = "space";

    protected static final String SERVICE_NAME = "service_name";

    protected static final String SERVICE_VERSION = "service_version";

    @Inject(Timer.COMPONENT_TIMER)
    protected Timer timer;

    @Inject(ObjectParser.JSON)
    protected ObjectParser jsonParser;

    protected Template template;

    @Override
    protected CompletableFuture<Void> doStart() {
        SyncAddress.ServiceAddress hssc = (SyncAddress.ServiceAddress) getSyncConfig();
        template = new Template(hssc.getServiceUrl());
        return super.doStart();
    }

    @Override
    protected Syncer<K, Service> createSyncer() {
        return subscription -> {
            SyncConfig config = getSyncConfig();
            K key = subscription.getKey();
            try {
                SyncResponse<Service> response = getService(subscription, config);
                subscription.onUpdate(response);
            } catch (IOException e) {
                subscription.onUpdate(new SyncResponse<>(e));
            } finally {
                long delay = config.getInterval() + ThreadLocalRandom.current().nextLong(2000);
                timer.delay(getName() + "-" + key.getName(), delay, () -> addTask(key.getSubscriber()));
            }
        };
    }

    /**
     * Retrieves a service based on the provided service version and sync configuration.
     *
     * @param subscription The subscription for which an error occurred.
     * @param config  The sync configuration used to configure the HTTP connection.
     * @return An {@link ApiResponse} object containing the requested service.
     * @throws IOException If an I/O error occurs during the request.
     */
    protected SyncResponse<Service> getService(Subscription<K, Service> subscription, SyncConfig config) throws IOException {
        ServiceKey key = subscription.getKey();
        Map<String, Object> context = new HashMap<>(4);
        context.put(APPLICATION_NAME, application.getName());
        context.put(SPACE, key.getNamespace());
        context.put(SERVICE_NAME, key.getName());
        context.put(SERVICE_VERSION, String.valueOf(subscription.getVersion()));
        String uri = template.evaluate(context);
        return getResponse(config, uri);
    }

    /**
     * Gets an SyncResponse object from a SyncConfig and a URI.
     *
     * @param config The SyncConfig object used to configure the connection.
     * @param uri    The URI to send the GET request to.
     * @return The SyncResponse object containing the response.
     * @throws IOException If an I/O error occurs while sending the request or parsing the response.
     */
    protected SyncResponse<Service> getResponse(SyncConfig config, String uri) throws IOException {
        HttpResponse<ApiResponse<Service>> response = HttpUtils.get(uri,
                conn -> configure(config, conn),
                reader -> jsonParser.read(reader, new TypeReference<ApiResponse<Service>>() {
                }));
        return ApiResponse.from(response).asSyncResponse();
    }

    /**
     * Configures the HTTP connection with the specified synchronization configuration.
     *
     * @param config the synchronization configuration.
     * @param conn   the HTTP connection to be configured.
     */
    protected void configure(SyncConfig config, HttpURLConnection conn) {
        config.header(conn::setRequestProperty);
        application.labelSync(conn::setRequestProperty);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout((int) config.getTimeout());
    }
}
