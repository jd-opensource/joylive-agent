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
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.util.URI;
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
import java.util.Arrays;
import java.util.List;

import static com.jd.live.agent.bootstrap.bytekit.context.MethodContext.invokeOrigin;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.CHAR_SEMICOLON;
import static com.jd.live.agent.core.util.StringUtils.join;

/**
 * MongoClientsInterceptor
 */
public class MongoClientsInterceptor extends AbstractDbConnectionInterceptor<LiveMongoClient> {

    private static final Logger logger = LoggerFactory.getLogger(MongoClientsInterceptor.class);

    private static final String TYPE_MONGODB = "mongodb";

    public MongoClientsInterceptor(PolicySupplier policySupplier, Publisher<DatabaseEvent> publisher, Timer timer) {
        super(policySupplier, publisher, timer);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        // redirect to new address
        Object[] arguments = ctx.getArguments();
        MongoClientSettings settings = (MongoClientSettings) arguments[0];
        ClusterSettings cluster = settings.getClusterSettings();
        String srvHost = cluster.getSrvHost();
        String address = srvHost == null || srvHost.isEmpty() ? join(cluster.getHosts()) : srvHost;
        DbResult result = getMaster(address);
        if (result != null && !result.isMaster()) {
            List<String> masterAddress = result.getMaster().getAddresses();
            result.setNewAddress(join(masterAddress, CHAR_SEMICOLON));
            ctx.setAttribute(ATTR_OLD_ADDRESS, result);
            MongoClientSettings.Builder builder = MongoClientSettings.builder(settings);
            builder.applyToClusterSettings(b -> {
                if (srvHost != null && !srvHost.isEmpty()) {
                    b.srvHost(result.getNewAddress());
                } else {
                    b.hosts(toList(masterAddress, this::toServerAddress));
                }
            });
            arguments[0] = builder.build();
            logger.info("Try reconnecting to mongodb {}", result.getNewAddress());
        }
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        MongoClient client = mc.getResult();
        MongoClientSettings settings = mc.getArgument(0);
        MongoDriverInformation driverInfo = mc.getArgument(1);
        ClusterSettings cluster = settings.getClusterSettings();
        // source address
        String srvHost = cluster.getSrvHost();
        String srcAddress = srvHost == null || srvHost.isEmpty() ? join(cluster.getHosts()) : srvHost;
        // redirected address
        DbResult result = ctx.getAttribute(ATTR_OLD_ADDRESS);
        String oldAddress = result != null ? result.getOldAddress() : srcAddress;
        String newAddress = result != null ? result.getNewAddress() : oldAddress;

        ClusterRedirect redirect = new ClusterRedirect(TYPE_MONGODB, oldAddress, newAddress);
        ClusterRedirect.redirect(redirect, consumer);

        Method method = mc.getMethod();
        mc.setResult(createConnection(() -> new LiveMongoClient(client, redirect, addr -> {
            try {
                MongoClientSettings newSettings = MongoClientSettings.builder(settings)
                        .applyToClusterSettings(builder -> {
                            if (srvHost != null && !srvHost.isEmpty()) {
                                builder.srvHost(addr.getAddress());
                            } else {
                                builder.hosts(toList(Arrays.asList(addr.getNodes()), this::toServerAddress));
                            }
                        }).build();
                return (MongoClient) invokeOrigin(null, method, new Object[]{newSettings, driverInfo});
            } catch (Exception ignore) {
                // Without exception.
                return null;
            }
        }, closer)));
    }

    @Override
    protected ClusterAddress createAddress(String address) {
        return new ClusterAddress(TYPE_MONGODB, address);
    }

    @Override
    protected void redirectTo(LiveMongoClient client, ClusterAddress address) {
        client.reconnect(address);
        ClusterRedirect.redirect(client.getAddress().newAddress(address), consumer);
    }

    protected ServerAddress toServerAddress(String address) {
        URI uri = URI.parse(address);
        return new ServerAddress(uri.getHost(), uri.getPort());
    }
}
