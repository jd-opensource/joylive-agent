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
import com.jd.live.agent.governance.db.DbUrl;
import com.jd.live.agent.governance.db.DbUrlParser;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * DriverConnectInterceptor
 */
public class DriverInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(DriverInterceptor.class);

    private static final BiConsumer<ClusterAddress, ClusterAddress> consumer = (oldAddress, newAddress) ->
            logger.info("{} connection is redirected from {} to {} ",
                    oldAddress.getType() == null ? "jdbc" : oldAddress.getType(), oldAddress, newAddress);

    private final PolicySupplier policySupplier;

    private final Map<String, DbUrlParser> parser;

    public DriverInterceptor(PolicySupplier policySupplier, Map<String, DbUrlParser> parser) {
        this.policySupplier = policySupplier;
        this.parser = parser;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        DbUrl dbUrl = DbUrlParser.parse((String) arguments[0], parser::get);
        // none tcp address, such as jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
        if (dbUrl.hasAddress()) {
            // lowercase address
            String oldAddress = dbUrl.getAddress().toLowerCase();
            LiveDatabase master = policySupplier.getPolicy().getMaster(oldAddress);
            String newAddress = master == null ? oldAddress : master.getPrimaryAddress();
            newAddress = newAddress == null || newAddress.isEmpty() ? oldAddress : newAddress;
            // redirect new address
            ClusterRedirect redirect;
            if (!oldAddress.equals(newAddress)) {
                dbUrl = dbUrl.address(newAddress);
                arguments[0] = dbUrl.toString();
            }
            redirect = new ClusterRedirect(dbUrl.getType(), oldAddress, newAddress);
            ClusterRedirect.redirect(redirect, consumer);
            // put it in thread local for datasource interceptor
            ClusterRedirect.setAddress(redirect);
        }
    }

}
