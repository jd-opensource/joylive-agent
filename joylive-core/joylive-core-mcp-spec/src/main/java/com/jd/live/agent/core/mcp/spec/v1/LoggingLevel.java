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

/**
 * Existing Enums and Base Types (from previous implementation)
 */
@Getter
public enum LoggingLevel {

    @JsonField("debug")
    DEBUG(0),

    @JsonField("info")
    INFO(1),

    @JsonField("notice")
    NOTICE(2),

    @JsonField("warning")
    WARNING(3),

    @JsonField("error")
    ERROR(4),

    @JsonField("critical")
    CRITICAL(5),

    @JsonField("alert")
    ALERT(6),

    @JsonField("emergency")
    EMERGENCY(7);

    private final int level;

    LoggingLevel(int level) {
        this.level = level;
    }

}
