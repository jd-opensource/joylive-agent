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
package com.jd.live.agent.core.bytekit.matcher;

import com.jd.live.agent.core.bytekit.type.TypeDesc;

import java.net.URL;
import java.util.*;

/**
 * ExistMatcher
 *
 * @since 1.0.0
 */
public class ExistMatcher<T extends TypeDesc> extends AbstractJunction<T> {

    private Map<String, Set<String>> conditions;

    public ExistMatcher(Map<String, Set<String>> conditions) {
        this.conditions = conditions;
    }

    public ExistMatcher(String type, String... types) {
        if (type != null && types != null && types.length > 0) {
            this.conditions = new HashMap<>();
            conditions.put(type, new HashSet<>(Arrays.asList(types)));
        }
    }

    @Override
    public boolean match(T target) {
        Set<String> types = conditions == null ? null : conditions.get(target.getActualName());
        if (types != null) {
            for (String type : types) {
                if (type != null && !type.isEmpty()) {
                    String name = type.replace(".", "/") + ".class";
                    URL url = Thread.currentThread().getContextClassLoader().getResource(name);
                    if (url == null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
