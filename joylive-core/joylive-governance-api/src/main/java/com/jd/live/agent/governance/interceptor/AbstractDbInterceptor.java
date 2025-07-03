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

import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.governance.policy.live.db.LiveDatabaseSpec;
import com.jd.live.agent.governance.request.DbRequest;

/**
 * AbstractDbInterceptor is an abstract class that provides a base implementation for
 * database-related interceptors. It encapsulates the common functionality required to
 * interact with a PolicySupplier to determine the access policy for database operations.
 */
public abstract class AbstractDbInterceptor extends InterceptorAdaptor {

    /**
     * A supplier of governance policies that is used to retrieve the current policy
     * for database access control.
     */
    protected final PolicySupplier policySupplier;

    /**
     * Constructs a new AbstractDbInterceptor with the specified policy supplier.
     *
     * @param policySupplier the supplier of governance policies for database access control
     */
    public AbstractDbInterceptor(PolicySupplier policySupplier) {
        this.policySupplier = policySupplier;
    }

    /**
     * Protects the database operation by checking the access policy for the given request.
     * If the policy does not allow the operation, an exception is thrown and the interceptor
     * chain is skipped.
     *
     * @param context the MethodContext in which the database operation is being performed
     * @param request the DbRequest representing the database operation to be protected
     */
    protected void protect(MethodContext context, DbRequest request) {
        GovernancePolicy policy = policySupplier.getPolicy();
        LiveDatabaseSpec databaseSpec = policy == null ? null : policy.getLocalDatabaseSpec();
        LiveDatabase db = databaseSpec == null ? null : databaseSpec.getDatabase(request.getAddress());
        if (db != null) {
            // Retrieve the database policy and determine the access mode
            AccessMode dbMode = db.getAccessMode();
            AccessMode requestMode = request.getAccessMode();
            // Check if the operation is allowed based on the access mode
            if (!dbMode.isReadable() && requestMode.isReadable()) {
                onReject(context, request, request.getType() + " is not readable, address=" + request.getAddress());
            } else if (!dbMode.isWriteable() && requestMode.isWriteable()) {
                onReject(context, request, request.getType() + " is not writeable, address=" + request.getAddress());
            }
        }
    }

    /**
     * Handles request rejection by skipping further processing and throwing an exception.
     *
     * @param context the method execution context
     * @param request the database request being processed
     * @param message rejection reason message
     */
    protected void onReject(MethodContext context, DbRequest request, String message) {
        context.skipWithThrowable(request.reject(message));
    }
}

