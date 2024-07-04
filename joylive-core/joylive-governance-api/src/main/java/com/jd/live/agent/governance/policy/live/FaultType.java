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

public enum FaultType {

    UNREADY {
        @Override
        public RejectException reject(String reason) {
            return new RejectException.RejectCellException();
        }

        @Override
        public RejectException failover(String reason) {
            return new RejectException.RejectCellException();
        }
    },

    LIMIT {
        @Override
        public RejectException reject(String reason) {
            return new RejectException.RejectLimitException(reason);
        }

        @Override
        public RejectException failover(String reason) {
            return new RejectException.RejectLimitException(reason);
        }
    },

    CIRCUIT_BREAK {
        @Override
        public RejectException reject(String reason) {
            return new RejectException.RejectCircuitBreakException(reason);
        }

        @Override
        public RejectException failover(String reason) {
            return new RejectException.RejectCircuitBreakException(reason);
        }

        @Override
        public RejectException degrade(String reason, Object degradeConfig) {
            return new RejectException.RejectCircuitBreakException(reason, degradeConfig);
        }
    },

    UNIT {
        @Override
        public RejectException reject(String reason) {
            return new RejectException.RejectUnitException(reason);
        }

        @Override
        public RejectException failover(String reason) {
            return new RejectException.RejectEscapeException(reason);
        }
    },

    CELL {
        @Override
        public RejectException reject(String reason) {
            return new RejectException.RejectCellException();
        }

        @Override
        public RejectException failover(String reason) {
            return new RejectException.RejectCellException();
        }
    };

    public abstract RejectException reject(String reason);

    public abstract RejectException failover(String reason);

    public RejectException degrade(String reason, Object degradeConfig) {
        return null;
    }

}
