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
package com.jd.live.agent.governance.policy.live;

import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.*;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;

/**
 * Enumeration representing different types of faults that can occur.
 */
public enum FaultType {

    /**
     * Represents a state where the system is not ready.
     */
    UNREADY {
        @Override
        public RejectException reject(String reason) {
            return new RejectUnreadyException();
        }

    },

    /**
     * Represents a state where the permission has failed.
     */
    PERMISSION_DENIED {
        @Override
        public RejectException reject(String reason) {
            return new RejectPermissionException(reason);
        }

    },

    /**
     * Represents a state where the authentication has failed.
     */
    UNAUTHORIZED {
        @Override
        public RejectException reject(String reason) {
            return new RejectAuthException(reason);
        }

    },

    /**
     * Represents a state where a limit has been reached.
     */
    LIMIT {
        @Override
        public RejectException reject(String reason) {
            return new RejectLimitException(reason);
        }

    },

    /**
     * Represents a state where a circuit break has occurred.
     */
    CIRCUIT_BREAK {
        @Override
        public RejectException reject(String reason) {
            return new RejectCircuitBreakException(reason);
        }

        @Override
        public RejectException degrade(String reason, DegradeConfig config) {
            return new RejectCircuitBreakException(reason, config);
        }
    },

    /**
     * Represents a state where a unit fault has occurred.
     */
    UNIT {
        @Override
        public RejectException reject(String reason) {
            return new RejectUnitException(reason);
        }

        @Override
        public RejectException failover(String reason) {
            return new RejectEscapeException(reason);
        }
    },

    /**
     * Represents a state where a cell fault has occurred.
     */
    CELL {

        @Override
        public RejectException reject(String reason) {
            return new RejectCellException();
        }

        @Override
        public RejectException failover(String reason) {
            return new RejectEscapeException(reason);
        }
    };

    /**
     * Rejects the request with a specific exception.
     *
     * @param reason the reason for rejection
     * @return a {@link RejectException}
     */
    public abstract RejectException reject(String reason);

    /**
     * Fails over the request with a specific exception.
     *
     * @param reason the reason for failover
     * @return a {@link RejectException}
     */
    public RejectException failover(String reason) {
        return null;
    }

    /**
     * Degrades the request with a specific exception.
     *
     * @param reason the reason for degradation
     * @param config the configuration for degradation
     * @return a {@link RejectException}, or {@code null} if not supported
     */
    public RejectException degrade(String reason, DegradeConfig config) {
        return null;
    }
}
