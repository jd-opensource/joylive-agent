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
package com.jd.live.agent.plugin.protection.jdbc.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.plugin.protection.jdbc.sql.LiveConnection;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import static com.jd.live.agent.governance.util.DatabaseUtils.ADDRESS;
import static com.jd.live.agent.governance.util.DatabaseUtils.redirect;

/**
 * DataSourceInterceptor
 */
public class DataSourceInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceInterceptor.class);

    private static final BiConsumer<String, String> consumer = (oldAddress, newAddress) -> logger.info("Jdbc connection is redirected from {} to {} ", oldAddress, newAddress);

    private final PolicySupplier policySupplier;

    private final Publisher<DatabaseEvent> publisher;

    private final Map<String, List<Connection>> connections = new ConcurrentHashMap<>();

    public DataSourceInterceptor(PolicySupplier policySupplier, Publisher<DatabaseEvent> publisher) {
        this.policySupplier = policySupplier;
        this.publisher = publisher;
        publisher.addHandler(this::onEvent);
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Connection oldConnection = mc.getResult();
        String address = ADDRESS.get();
        if (address != null) {
            LiveConnection newConnection = new LiveConnection(address, oldConnection, c -> {
                List<Connection> values = connections.get(c.getAddress());
                if (values != null) {
                    values.remove(c);
                }
            });
            connections.computeIfAbsent(address, a -> new CopyOnWriteArrayList<>()).add(newConnection);
            mc.setResult(newConnection);
        }
    }

    private void onEvent(List<Event<DatabaseEvent>> events) {
        GovernancePolicy policy = policySupplier.getPolicy();
        Close close = Close.instance();
        connections.forEach((address, cons) -> {
            LiveDatabase master = policy.getMaster(address);
            if (master != null && !master.contains(address)) {
                // Close connection to reconnect to the new master address
                cons.forEach(close::close);
                redirect(address, master.getPrimaryAddress(), consumer);
            }
        });
    }


}
