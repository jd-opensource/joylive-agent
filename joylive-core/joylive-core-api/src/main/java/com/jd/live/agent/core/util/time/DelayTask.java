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
package com.jd.live.agent.core.util.time;

/**
 * Represents a task that is scheduled to be executed after a certain delay. This interface extends {@link TimeTask},
 * inheriting its methods for retrieving the task's name and its scheduled execution time. The execution time in the context
 * of a {@code DelayTask} is understood to be the time at which the delay period ends and the task is eligible for execution.
 */
public interface DelayTask extends TimeTask {

}

