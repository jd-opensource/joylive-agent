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
package com.jd.live.agent.core.mcp.spec.v1;

import com.jd.live.agent.core.parser.annotation.JsonField;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * The response to a tasks/cancel request.
 *
 * @category `tasks/cancel`
 */
@Getter
@Setter
public class CancelTaskResult extends Task implements Result {
    /**
     * See specification for notes on _meta usage
     */
    @JsonField("_meta")
    private Map<String, Object> meta;

    public CancelTaskResult() {
    }

    public CancelTaskResult(String taskId,
                            TaskStatus status,
                            String statusMessage,
                            String createdAt,
                            String lastUpdatedAt,
                            Long ttl,
                            Long pollInterval,
                            Map<String, Object> meta) {
        super(taskId, status, statusMessage, createdAt, lastUpdatedAt, ttl, pollInterval);
        this.meta = meta;
    }

    public static class CancelTaskResultBuilder extends AbstractTaskBuilder<CancelTaskResult, CancelTaskResultBuilder> {

        private Map<String, Object> meta;

        public CancelTaskResultBuilder meta(Map<String, Object> meta) {
            this.meta = meta;
            return self();
        }

        @Override
        protected CancelTaskResultBuilder self() {
            return this;
        }

        @Override
        protected CancelTaskResult createInstance() {
            return new CancelTaskResult();
        }

        @Override
        public CancelTaskResult build() {
            CancelTaskResult instance = super.build();
            instance.setMeta(meta);
            return instance;
        }

        public static CancelTaskResultBuilder builder() {
            return new CancelTaskResultBuilder();
        }
    }
}
