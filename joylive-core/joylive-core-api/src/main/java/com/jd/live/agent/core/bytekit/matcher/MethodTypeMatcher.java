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

import com.jd.live.agent.core.bytekit.type.MethodDesc;


/**
 * MethodTypeMatcher
 *
 * @param <T> Match target type
 * @since 1.0.0
 */
public class MethodTypeMatcher<T extends MethodDesc> extends AbstractJunction<T> {

    private final MethodType type;

    public MethodTypeMatcher(MethodType type) {
        this.type = type;
    }

    @Override
    public boolean match(T target) {
        return target != null && type.is(target);
    }

    public enum MethodType {
        CONSTRUCTOR("isConstructor()") {
            @Override
            public boolean is(MethodDesc desc) {
                return desc.isConstructor();
            }
        },
        METHOD("isMethod()") {
            @Override
            boolean is(MethodDesc desc) {
                return desc.isMethod();
            }
        },
        DEFAULT_METHOD("isDefaultMethod()") {
            @Override
            boolean is(MethodDesc desc) {
                return desc.isDefaultMethod();
            }
        };

        private final String description;

        MethodType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        abstract boolean is(MethodDesc desc);

    }
}
