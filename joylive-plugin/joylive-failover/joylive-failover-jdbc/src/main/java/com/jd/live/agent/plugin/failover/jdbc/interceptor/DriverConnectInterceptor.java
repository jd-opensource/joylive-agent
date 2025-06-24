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
package com.jd.live.agent.plugin.failover.jdbc.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.db.DbUrl;
import com.jd.live.agent.governance.db.jdbc.connection.DriverConnection;
import com.jd.live.agent.governance.db.jdbc.context.DriverContext;
import com.jd.live.agent.governance.db.jdbc.datasource.LiveDataSource;
import com.jd.live.agent.governance.interceptor.AbstractDbConnectionInterceptor;
import com.jd.live.agent.governance.interceptor.AbstractJdbcConnectionInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;

import static com.jd.live.agent.governance.util.network.ClusterRedirect.getRedirect;

/**
 * DriverInterceptor
 */
public class DriverConnectInterceptor extends AbstractDbConnectionInterceptor<DbConnection> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractJdbcConnectionInterceptor.class);

    public DriverConnectInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        LiveDataSource ds = DriverContext.get();
        if (ds == null) {
            // not druid & hikari
            return;
        }
        // none tcp address, such as jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
        DbUrl dbUrl = ds.getUrl();
        AccessMode accessMode = getAccessMode(ds.getPoolName(), dbUrl, ctx.getArgument(1));
        DbCandidate candidate = getCandidate(dbUrl.getType(), dbUrl.getAddress(), accessMode, PRIMARY_ADDRESS_RESOLVER);
        String newAddress = candidate.getNewAddress();
        // redirect new address
        if (candidate.isRedirected()) {
            dbUrl = dbUrl.address(newAddress);
            ctx.setArgument(0, dbUrl.toString());
            // log once.
            ClusterAddress target = getRedirect(new ClusterAddress(candidate.getType(), candidate.getOldAddress()));
            if (target == null || !target.getAddress().equals(candidate.getNewAddress())) {
                logger.info("Try reconnecting to {} {}", dbUrl.getType(), candidate.getNewAddress());
            }
        }
        ctx.setAttribute(ATTR_OLD_ADDRESS, candidate);
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        DbCandidate candidate = ctx.getAttribute(ATTR_OLD_ADDRESS);
        if (candidate == null) {
            return;
        }
        ClusterRedirect redirect = toClusterRedirect(candidate);
        ClusterRedirect.redirect(redirect, candidate.isRedirected() ? consumer : null);
        mc.setResult(new DriverConnection(mc.getResult(), redirect, DriverContext.get()));
    }

    @Override
    protected void redirectTo(DbConnection connection, ClusterAddress address) {

    }
}
