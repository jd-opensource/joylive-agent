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
package com.jd.live.agent.governance.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A class representing an error name with its corresponding code.
 */
@Getter
@AllArgsConstructor
public class ErrorName {

    /**
     * The name of the error.
     */
    private String name;

    /**
     * The code associated with the error.
     */
    private String code;

}
