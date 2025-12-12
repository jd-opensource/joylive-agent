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

/**
 * Data associated with a task.
 *
 * @category `tasks`
 */
@Getter
@Setter
public class Task {

    /**
     * The task identifier.
     */
    private String taskId;

    /**
     * Current task state.
     */
    private TaskStatus status;

    /**
     * Optional human-readable message describing the current task state.
     * This can provide context for any status, including:
     * - Reasons for "cancelled" status
     * - Summaries for "completed" status
     * - Diagnostic information for "failed" status (e.g., error details, what went wrong)
     */
    private String statusMessage;

    /**
     * ISO 8601 timestamp when the task was created.
     */
    private String createdAt;

    /**
     * ISO 8601 timestamp when the task was last updated.
     */
    private String lastUpdatedAt;

    /**
     * Actual retention duration from creation in milliseconds, null for unlimited.
     */
    private Long ttl;

    /**
     * Suggested polling interval in milliseconds.
     */
    private Long pollInterval;

    public Task() {
    }

    public Task(String taskId,
                TaskStatus status,
                String statusMessage,
                String createdAt,
                String lastUpdatedAt,
                Long ttl,
                Long pollInterval) {
        this.taskId = taskId;
        this.status = status;
        this.statusMessage = statusMessage;
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
        this.ttl = ttl;
        this.pollInterval = pollInterval;
    }

    /**
     * The status of a task.
     *
     * @category `tasks`
     */
    public enum TaskStatus {

        @JsonField("working")
        WORKING,

        @JsonField("input_required")
        INPUT_REQUIRED,

        @JsonField("notice")
        NOTICE,

        @JsonField("completed")
        COMPLETED,

        @JsonField("error")
        ERROR,

        @JsonField("failed")
        FAILED,

        @JsonField("cancelled")
        CANCELLED,

    }

    /**
     * A manual builder for {@link Task} that supports inheritance.
     * This builder can be extended by subclasses to provide type-safe builders for Task subclasses.
     *
     * @param <T> The type of Task being built
     * @param <B> The type of Builder (self-type for method chaining)
     */
    protected abstract static class AbstractTaskBuilder<T extends Task, B extends AbstractTaskBuilder<T, B>> {

        protected String taskId;
        protected TaskStatus status;
        protected String statusMessage;
        protected String createdAt;
        protected String lastUpdatedAt;
        protected Long ttl;
        protected Long pollInterval;

        /**
         * Returns this builder for method chaining.
         *
         * @return this builder
         */
        protected abstract B self();

        /**
         * Creates a new instance of the Task being built.
         *
         * @return a new Task instance
         */
        protected abstract T createInstance();

        /**
         * Sets the task identifier.
         *
         * @param taskId the task identifier
         * @return this builder
         */
        public B taskId(String taskId) {
            this.taskId = taskId;
            return self();
        }

        /**
         * Sets the current task state.
         *
         * @param status the current task state
         * @return this builder
         */
        public B status(TaskStatus status) {
            this.status = status;
            return self();
        }

        /**
         * Sets the optional human-readable message describing the current task state.
         *
         * @param statusMessage the status message
         * @return this builder
         */
        public B statusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
            return self();
        }

        /**
         * Sets the ISO 8601 timestamp when the task was created.
         *
         * @param createdAt the creation timestamp
         * @return this builder
         */
        public B createdAt(String createdAt) {
            this.createdAt = createdAt;
            return self();
        }

        /**
         * Sets the ISO 8601 timestamp when the task was last updated.
         *
         * @param lastUpdatedAt the last updated timestamp
         * @return this builder
         */
        public B lastUpdatedAt(String lastUpdatedAt) {
            this.lastUpdatedAt = lastUpdatedAt;
            return self();
        }

        /**
         * Sets the actual retention duration from creation in milliseconds, null for unlimited.
         *
         * @param ttl the time-to-live in milliseconds
         * @return this builder
         */
        public B ttl(Long ttl) {
            this.ttl = ttl;
            return self();
        }

        /**
         * Sets the suggested polling interval in milliseconds.
         *
         * @param pollInterval the polling interval in milliseconds
         * @return this builder
         */
        public B pollInterval(Long pollInterval) {
            this.pollInterval = pollInterval;
            return self();
        }

        /**
         * Builds a new Task instance with the current builder values.
         *
         * @return a new Task instance
         */
        public T build() {
            T instance = createInstance();
            instance.setTaskId(taskId);
            instance.setStatus(status);
            instance.setStatusMessage(statusMessage);
            instance.setCreatedAt(createdAt);
            instance.setLastUpdatedAt(lastUpdatedAt);
            instance.setTtl(ttl);
            instance.setPollInterval(pollInterval);
            return instance;
        }
    }

    /**
     * A concrete implementation of TaskBuilder for the Task class.
     */
    public static class TaskBuilder extends AbstractTaskBuilder<Task, TaskBuilder> {
        @Override
        protected TaskBuilder self() {
            return this;
        }

        @Override
        protected Task createInstance() {
            return new Task();
        }

        /**
         * Creates a new builder for Task.
         *
         * @return a new TaskBuilder
         */
        public static TaskBuilder builder() {
            return new TaskBuilder();
        }
    }


}
