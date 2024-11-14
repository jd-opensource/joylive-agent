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
package com.jd.live.agent.governance.service.sync;

import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.service.ServiceName;
import lombok.Getter;

/**
 * Represents a key used to identify a subscription.
 */
public interface SyncKey {

    String getType();

    @Getter
    class ServiceKey implements SyncKey, ServiceName {

        protected final PolicySubscriber subscriber;

        public ServiceKey(PolicySubscriber subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public String getNamespace() {
            return subscriber.getNamespace();
        }

        @Override
        public String getName() {
            return subscriber.getName();
        }

        @Override
        public String getType() {
            return "service";
        }

        @Override
        public String toString() {
            return subscriber.getUniqueName();
        }
    }

    @Getter
    class LiveSpaceKey implements SyncKey {

        private final String id;

        public LiveSpaceKey(String id) {
            this.id = id;
        }

        @Override
        public String getType() {
            return "lane space";
        }
    }

    @Getter
    class LaneSpaceKey implements SyncKey {

        protected final String id;

        public LaneSpaceKey(String id) {
            this.id = id;
        }

        @Override
        public String getType() {
            return "lane space";
        }
    }
}
