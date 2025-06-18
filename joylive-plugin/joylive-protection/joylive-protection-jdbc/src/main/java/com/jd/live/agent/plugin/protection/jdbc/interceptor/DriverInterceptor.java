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
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.db.DataSourceDescriptor;
import com.jd.live.agent.governance.db.DataSourceDescriptorFactory;
import com.jd.live.agent.governance.db.DbUrl;
import com.jd.live.agent.governance.db.DbUrlParser;
import com.jd.live.agent.governance.interceptor.AbstractDbFailoverInterceptor;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.protection.jdbc.context.DbContext;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * DriverInterceptor
 */
public class DriverInterceptor extends AbstractDbFailoverInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(DriverInterceptor.class);

    private static final BiConsumer<ClusterAddress, ClusterAddress> consumer = (oldAddress, newAddress) ->
            logger.info("{} connection is redirected from {} to {} ",
                    oldAddress.getType() == null ? "jdbc" : oldAddress.getType(), oldAddress, newAddress);

    private final Map<DataSource, DbUrl> dbUrls = new ConcurrentHashMap<>();

    private final Map<String, DbUrlParser> parsers;

    private final Map<String, DataSourceDescriptorFactory> descriptorFactories;

    public DriverInterceptor(PolicySupplier policySupplier,
                             Application application,
                             GovernanceConfig governanceConfig,
                             Map<String, DbUrlParser> parsers,
                             Map<String, DataSourceDescriptorFactory> descriptorFactories) {
        super(policySupplier, application, governanceConfig);
        this.parsers = parsers;
        this.descriptorFactories = descriptorFactories;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        DataSource dataSource = DbContext.getDataSource();
        if (dataSource != null) {
            DbUrl dbUrl = dbUrls.computeIfAbsent(dataSource, d -> DbUrlParser.parse(ctx.getArgument(0), parsers::get));
            // none tcp address, such as jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
            if (dbUrl.hasAddress()) {
                AccessMode accessMode = getAccessMode(getDatasourceName(dataSource), dbUrl, ctx.getArgument(1));
                DbCandidate candidate = getCandidate(dbUrl.getType(), dbUrl.getAddress(), accessMode);
                String newAddress = candidate.getNewAddress();
                // redirect new address
                if (candidate.isRedirected()) {
                    dbUrl = dbUrl.address(newAddress);
                    ctx.setArgument(0, dbUrl.toString());
                    logger.info("Try reconnecting to {} {}", dbUrl.getType(), candidate.getNewAddress());
                }
                ctx.setAttribute(ATTR_OLD_ADDRESS, candidate);
            }
        }
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        DbCandidate candidate = ctx.getAttribute(ATTR_OLD_ADDRESS);
        if (candidate != null) {
            ClusterRedirect redirect = toClusterRedirect(candidate);
            ClusterRedirect.redirect(redirect, candidate.isRedirected() ? consumer : null);
            DbContext.setClusterRedirect(redirect);
        }
    }

    /**
     * Gets the name of the current data source.
     *
     * @return The name of the current data source, or null if no data source is bound
     * or no descriptor exists for it.
     */
    private String getDatasourceName(DataSource dataSource) {
        DataSourceDescriptorFactory descriptorFactory = dataSource == null ? null : descriptorFactories.get(dataSource.getClass().getSimpleName());
        DataSourceDescriptor descriptor = descriptorFactory == null ? null : descriptorFactory.getDescriptor(dataSource);
        return descriptor == null ? null : descriptor.getName();
    }
}
