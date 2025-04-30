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
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.network.Address;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.protection.jdbc.util.JdbcUrl;

import java.util.function.BiConsumer;

import static com.jd.live.agent.core.util.network.Address.parse;
import static com.jd.live.agent.governance.util.network.ClusterRedirect.redirect;
import static com.jd.live.agent.governance.util.network.ClusterRedirect.setAddress;

/**
 * DriverConnectInterceptor
 */
public class DriverInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(DriverInterceptor.class);

    private static final BiConsumer<ClusterAddress, ClusterAddress> consumer = (oldAddress, newAddress) -> logger.info("Jdbc connection is redirected from {} to {} ", oldAddress, newAddress);

    private final PolicySupplier policySupplier;

    public DriverInterceptor(PolicySupplier policySupplier) {
        this.policySupplier = policySupplier;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        JdbcUrl uri = JdbcUrl.parse((String) arguments[0]);
        String host = uri.getHost();
        Integer port = uri.getPort();
        // handle ipv6
        Address address = parse(host, false, port);
        String oldAddress = address.getAddress().toLowerCase();
        LiveDatabase master = policySupplier.getPolicy().getMaster(oldAddress);
        String newAddress = master == null ? oldAddress : master.getPrimaryAddress();
        ClusterRedirect redirect;
        if (newAddress != null && !oldAddress.equals(newAddress)) {
            redirect = new ClusterRedirect(oldAddress, newAddress);
            setAddress(redirect);
            redirect(redirect, consumer);

            // handle ipv6
            address = parse(newAddress);
            uri = new JdbcUrl(uri.getScheme(), uri.getUser(), uri.getPassword(), address, uri.getPath(), uri.getQuery());
            arguments[0] = uri.toString();
        } else {
            redirect = new ClusterRedirect(oldAddress, oldAddress);
            setAddress(redirect);
            redirect(redirect, consumer);
        }
    }

}
