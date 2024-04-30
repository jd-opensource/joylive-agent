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

import com.jd.live.agent.core.bytekit.type.TypeDef;
import com.jd.live.agent.core.bytekit.type.TypeDesc;

import java.util.LinkedList;
import java.util.Queue;


/**
 * SubTypeMatcher
 *
 * @since 1.0.0
 */
public class SubTypeMatcher<T extends TypeDesc> extends AbstractJunction<T> {

    private final Class<?> type;

    private final boolean implement;

    public SubTypeMatcher(Class<?> type) {
        this(type, false);
    }

    public SubTypeMatcher(Class<?> type, boolean implement) {
        this.type = type;
        this.implement = implement;
    }

    @Override
    public boolean match(T target) {
        return target != null && type != null && target.isAssignableTo(type) && !(implement && target.isInterface());
    }

    public static class SubNameMatcher<T extends TypeDesc> extends AbstractJunction<T> {

        private final String type;

        private final boolean implement;

        public SubNameMatcher(String type) {
            this(type, false);
        }

        public SubNameMatcher(String type, boolean implement) {
            this.type = type;
            this.implement = implement;
        }

        @Override
        public boolean match(T target) {
            if (target == null || type == null || type.isEmpty() || implement && target.isInterface()) {
                return false;
            }
            final Queue<TypeDef> queue = new LinkedList<>();
            queue.add(target);
            while (!queue.isEmpty()) {
                TypeDef current = queue.poll();
                if (current.getActualName().equals(type)) {
                    return true;
                }
                for (TypeDesc.Generic generic : current.getInterfaces()) {
                    queue.add(generic.asErasure());
                }
                TypeDesc.Generic parent = current.getSuperClass();
                if (parent != null) {
                    queue.add(parent.asErasure());
                }
            }
            return false;
        }

    }

}
