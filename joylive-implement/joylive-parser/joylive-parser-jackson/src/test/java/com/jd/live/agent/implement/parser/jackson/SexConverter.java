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
package com.jd.live.agent.implement.parser.jackson;

import com.jd.live.agent.core.parser.annotation.JsonConverter;

public class SexConverter implements JsonConverter<Object, Sex> {

    @Override
    public Sex convert(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof Integer) {
            return ((Integer) source) == 1 ? Sex.FEMALE : Sex.MALE;
        } else {
            return Sex.valueOf(source.toString());
        }
    }
}
