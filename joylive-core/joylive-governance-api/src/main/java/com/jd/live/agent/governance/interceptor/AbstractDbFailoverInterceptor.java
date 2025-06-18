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
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.db.DbUrl;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.*;

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

    public AbstractDbFailoverInterceptor(PolicySupplier policySupplier,
                                         Application application,
                                         GovernanceConfig governanceConfig) {
        this.policySupplier = policySupplier;
        this.application = application;
        this.location = application.getLocation();
        this.governanceConfig = governanceConfig;
    }

    /**
     * Selects an appropriate database node (master or replica) based on requested access mode.
     *
     * @param type            database type/category identifier
     * @param address         cluster address (normalized to lowercase)
     * @param accessMode      READ or WRITE operation mode
     * @param addressResolver function to resolve node address from LiveDatabase
     * @return configured DbCandidate instance, or null if no matching node available
     */
    protected DbCandidate getCandidate(String type, String address, AccessMode accessMode, Function<LiveDatabase, String> addressResolver) {
        address = address == null ? null : address.toLowerCase();
        String[] nodes = toList(toList(splitList(address), URI::parse), URI::getAddress).toArray(new String[0]);
        Location location = application.getLocation();
        GovernancePolicy policy = policySupplier.getPolicy();
        LiveDatabase database = accessMode.isWriteable()
                ? policy.getWriteDatabase(nodes)
                : policy.getReadDatabase(location.getUnit(), location.getCell(), nodes);
        return new DbCandidate(type, accessMode, address, nodes, database, addressResolver);
    }

    /**
     * Checks if master database configuration has changed between two states.
     *
     * @param oldResult previous database state (may be null)
     * @param newResult current database state (may be null)
     * @return true if master addresses differ or state changed from/to null
     */
    protected boolean isChanged(DbCandidate oldResult, DbCandidate newResult) {
        if (oldResult == null) {
            return newResult != null;
        } else if (newResult == null) {
            return true;
        }
        return oldResult.isChanged(newResult);
    }

    /**
     * Converts a DbCandidate into a ClusterRedirect instance.
     *
     * @param candidate contains database redirection details including:
     * @return configured ClusterRedirect with address transformation
     */
    protected ClusterRedirect toClusterRedirect(DbCandidate candidate) {
        return new ClusterRedirect(
                candidate.getType(),
                candidate.getAccessMode(),
                candidate.getOldAddress(),
                candidate.getNewAddress(),
                database -> new ClusterAddress(candidate.getType(), candidate.getAddressResolver().apply(database)));
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
        } else if (READ.equalsIgnoreCase(url.getParameter(ACCESS_MODE))) {
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

    /**
     * Represents a potential database connection candidate with redirection support.
     * Tracks original address/nodes and actual database instance, with optional redirection address.
     */
    @Getter
    protected static class DbCandidate {

        private final String type;

        private final AccessMode accessMode;

        private final String oldAddress;

        private final String[] oldNodes;

        private final LiveDatabase database;

        private final Function<LiveDatabase, String> addressResolver;

        private final String newAddress;

        private final boolean redirected;

        public DbCandidate(String type,
                           AccessMode accessMode,
                           String oldAddress,
                           String[] oldNodes,
                           LiveDatabase database,
                           Function<LiveDatabase, String> addressResolver) {
            this.type = type;
            this.accessMode = accessMode;
            this.oldAddress = oldAddress;
            this.oldNodes = oldNodes;
            this.database = database;
            this.addressResolver = addressResolver;
            this.newAddress = database == null ? oldAddress : (addressResolver == null ? database.getPrimaryAddress() : addressResolver.apply(database));
            this.redirected = database != null && !database.contains(oldNodes);
        }

        /**
         * Determines if database address has changed by comparing with target candidate.
         *
         * @param target Candidate to compare against (nullable)
         * @return true if either candidate is null, instances differ, or addresses don't match
         */
        public boolean isChanged(DbCandidate target) {
            if (target == null) {
                return true;
            }
            LiveDatabase targetDatabase = target.database;
            if (targetDatabase == database) {
                return false;
            }
            List<String> targetAddresses = targetDatabase == null ? null : targetDatabase.getAddresses();
            List<String> Addresses = database == null ? null : database.getAddresses();
            return !Objects.equals(targetAddresses, Addresses);
        }

    }
}
