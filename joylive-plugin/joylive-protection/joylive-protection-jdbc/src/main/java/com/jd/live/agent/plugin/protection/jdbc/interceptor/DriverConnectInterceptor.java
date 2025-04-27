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
import com.jd.live.agent.core.util.network.Address;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.policy.live.LiveSpec;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.plugin.protection.jdbc.sql.LiveConnection;
import com.jd.live.agent.plugin.protection.jdbc.util.JdbcUrl;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.jd.live.agent.core.util.network.Address.parse;

/**
 * DriverConnectInterceptor
 */
public class DriverConnectInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(DriverConnectInterceptor.class);

    private static final String ATTRIBUTE_ADDRESS = "uri";

    private final PolicySupplier policySupplier;

    private final Publisher<DatabaseEvent> publisher;

    private final Map<String, List<Connection>> connections = new ConcurrentHashMap<>();

    public DriverConnectInterceptor(PolicySupplier policySupplier, Publisher<DatabaseEvent> publisher) {
        this.policySupplier = policySupplier;
        this.publisher = publisher;
        publisher.addHandler(this::onEvent);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        JdbcUrl uri = JdbcUrl.parse((String) arguments[0]);
        String host = uri.getHost();
        Integer port = uri.getPort();
        // handle ipv6
        Address address = parse(host, false, port);
        String addr = address.getAddress().toLowerCase();
        LiveDatabase master = getMaster(addr);
        String newAddress = master == null ? addr : master.getPrimaryAddress();
        if (newAddress != null && !addr.equals(newAddress)) {
            // handle ipv6
            address = parse(newAddress);
            uri = new JdbcUrl(uri.getScheme(), uri.getUser(), uri.getPassword(), address, uri.getPath(), uri.getQuery());
            ctx.setAttribute(ATTRIBUTE_ADDRESS, newAddress);
            arguments[0] = uri.toString();
            logger.info("Jdbc connection is created. it's redirected from {} to {} ", addr, newAddress);
        }
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Connection oldConnection = mc.getResult();
        String address = ctx.getAttribute(ATTRIBUTE_ADDRESS);
        LiveConnection newConnection = new LiveConnection(address, oldConnection, c -> {
            List<Connection> values = connections.get(c.getAddress());
            if (values != null) {
                values.remove(c);
            }
        });
        connections.computeIfAbsent(address, a -> new CopyOnWriteArrayList<>()).add(newConnection);
        mc.setResult(newConnection);
    }

    private void onEvent(List<Event<DatabaseEvent>> events) {
        connections.forEach((address, cons) -> {
            LiveDatabase master = getMaster(address);
            if (master != null && !master.contains(address)) {
                // Close connection to reconnect to the new master address
                Close close = Close.instance();
                cons.forEach(close::close);
                logger.info("Jdbc connection is closing. it's redirected from {} to {} ", address, master.getPrimaryAddress());
            }
        });
    }

    private LiveDatabase getMaster(String address) {
        LiveSpace liveSpace = policySupplier.getPolicy().getLocalLiveSpace();
        LiveSpec spec = liveSpace == null ? null : liveSpace.getSpec();
        if (spec != null) {
            LiveDatabase database = spec.getDatabase(address);
            if (database != null) {
                return database.getMaster();
            }
        }
        return null;
    }

}
