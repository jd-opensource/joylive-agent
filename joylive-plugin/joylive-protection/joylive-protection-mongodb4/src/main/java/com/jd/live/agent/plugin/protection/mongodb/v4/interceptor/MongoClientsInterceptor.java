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
package com.jd.live.agent.plugin.protection.mongodb.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.interceptor.AbstractDbConnectionInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.protection.mongodb.v4.client.LiveMongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoDriverInformation;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.connection.ClusterSettings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.bootstrap.bytekit.context.MethodContext.invokeOrigin;
import static com.jd.live.agent.core.util.StringUtils.join;

/**
 * MongoClientsInterceptor
 */
public class MongoClientsInterceptor extends AbstractDbConnectionInterceptor<LiveMongoClient> {

    public MongoClientsInterceptor(PolicySupplier policySupplier, Publisher<DatabaseEvent> publisher, Timer timer) {
        super(policySupplier, publisher, timer);
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        MongoClient client = mc.getResult();
        MongoClientSettings settings = mc.getArgument(0);
        MongoDriverInformation driverInfo = mc.getArgument(1);
        ClusterSettings cluster = settings.getClusterSettings();
        String srvHost = cluster.getSrvHost();
        String address = srvHost == null || srvHost.isEmpty() ? join(cluster.getHosts()) : srvHost;
        if (address != null && !address.isEmpty()) {
            ClusterRedirect redirect = new ClusterRedirect(address);
            Method method = mc.getMethod();
            mc.setResult(createConnection(() -> new LiveMongoClient(client, redirect, addr -> {
                try {
                    MongoClientSettings newSettings = MongoClientSettings.builder(settings)
                            .applyToClusterSettings(builder -> {
                                if (srvHost != null && !srvHost.isEmpty()) {
                                    builder.srvHost(addr.getAddress());
                                } else {
                                    builder.hosts(newAddress(addr.getNodes()));
                                }
                            }).build();
                    return (MongoClient) invokeOrigin(null, method, new Object[]{newSettings, driverInfo});
                } catch (Exception ignore) {
                    // Without exception.
                    return null;
                }
            }, closer)));
        }
    }

    @Override
    protected ClusterAddress createAddress(String address) {
        return new ClusterAddress("mongodb", address);
    }

    @Override
    protected void redirectTo(LiveMongoClient client, ClusterAddress address) {
        client.reconnect(address);
        ClusterRedirect.redirect(client.getAddress().newAddress(address), consumer);
    }

    private List<ServerAddress> newAddress(String[] addresses) {
        List<ServerAddress> result = new ArrayList<>(addresses.length);
        for (String address : addresses) {
            // the port will be parsed from the address.
            result.add(new ServerAddress(address));
        }
        return result;
    }

}
