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
package com.jd.live.agent.governance.interceptor;

import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.db.DbConnectionSupervisor;
import com.jd.live.agent.governance.db.DbUrl;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;

import java.util.Properties;
import java.util.function.Function;

import static com.jd.live.agent.core.util.StringUtils.CHAR_SEMICOLON;
import static com.jd.live.agent.core.util.StringUtils.join;

/**
 * Base interceptor for database failover scenarios.
 * Handles connection pooling and automatic redirection during topology changes.
 *
 * @implSpec Subclasses must implement connection wrapping logic
 */
public abstract class AbstractDbFailoverInterceptor extends InterceptorAdaptor {

    protected static final String ACCESS_MODE = "accessMode";

    protected static final String READ = "read";

    protected static final String ATTR_OLD_ADDRESS = "oldAddress";

    protected static final Function<LiveDatabase, String> MULTI_ADDRESS_SEMICOLON_RESOLVER = database -> join(database.getAddresses(), CHAR_SEMICOLON);

    protected static final Function<LiveDatabase, String> PRIMARY_ADDRESS_RESOLVER = LiveDatabase::getPrimaryAddress;

    protected final PolicySupplier policySupplier;

    protected final Application application;

    protected final Location location;

    protected final GovernanceConfig governanceConfig;

    protected final DbConnectionSupervisor connectionSupervisor;

    public AbstractDbFailoverInterceptor(PolicySupplier policySupplier,
                                         Application application,
                                         GovernanceConfig governanceConfig,
                                         DbConnectionSupervisor connectionSupervisor) {
        this.policySupplier = policySupplier;
        this.application = application;
        this.location = application.getLocation();
        this.governanceConfig = governanceConfig;
        this.connectionSupervisor = connectionSupervisor;
    }

    public AbstractDbFailoverInterceptor(InvocationContext context) {
        this(context.getPolicySupplier(), context.getApplication(), context.getGovernanceConfig(), context.getDbConnectionSupervisor());
    }

    /**
     * Determines database access mode from data source name.
     *
     * @param name Data source name (checked for READ suffix)
     * @return AccessMode.READ if detected in any source, otherwise READ_WRITE
     */
    protected AccessMode getAccessMode(String name) {
        return getAccessMode(name, null, null);
    }

    /**
     * Determines database access mode from multiple configuration sources.
     * Checks in order: name suffix, URL parameter, connection properties, then global config.
     * Defaults to READ_WRITE if no read mode is specified.
     *
     * @param name       Data source name (checked for READ suffix)
     * @param url        Database URL containing parameters
     * @param properties Connection properties
     * @return AccessMode.READ if detected in any source, otherwise READ_WRITE
     */
    protected AccessMode getAccessMode(String name, DbUrl url, Properties properties) {
        if (name != null && name.toLowerCase().endsWith(READ)) {
            return AccessMode.READ;
        } else if (url != null && READ.equalsIgnoreCase(url.getParameter(ACCESS_MODE))) {
            return AccessMode.READ;
        } else if (properties != null && READ.equalsIgnoreCase(properties.getProperty(ACCESS_MODE))) {
            return AccessMode.READ;
        } else {
            AccessMode accessMode = governanceConfig.getDbConfig().getAccessMode();
            // Global access mode configuration
            switch (accessMode) {
                case READ:
                    return AccessMode.READ;
                case READ_WRITE:
                default:
                    return AccessMode.READ_WRITE;
            }
        }
    }

}
