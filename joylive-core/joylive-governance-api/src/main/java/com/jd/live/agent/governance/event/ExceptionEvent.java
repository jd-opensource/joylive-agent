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
package com.jd.live.agent.governance.event;

import lombok.Getter;

/**
 * Represents an exception event containing class and method information
 * where the exception occurred.
 */
@Getter
public class ExceptionEvent {

    public static final String KEY_APPLICATION = "application";

    public static final String KEY_CLASS_NAME = "class_name";

    public static final String KEY_METHOD = "method";

    public static final String COUNTER_EXCEPTIONS_TOTAL = "joylive_exception_total";

    private final String className;

    private final String method;

    public ExceptionEvent(String className, String method) {
        this.className = className;
        this.method = method;
    }

}
