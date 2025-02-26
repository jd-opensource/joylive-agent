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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.bytekit.type.TypeDesc;
import com.jd.live.agent.core.util.cache.UnsafeLazyObject;


/**
 * SubTypeMatcher
 *
 * @param <T> types to be matched
 * @since 1.0.0
 */
public class SubTypeMatcher<T extends TypeDesc> extends AbstractJunction<T> {

    private static final Logger logger = LoggerFactory.getLogger(SubTypeMatcher.class);

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

        private final UnsafeLazyObject<Class<?>> optional = new UnsafeLazyObject<>(this::getType);

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
            Class<?> clazz = optional.get();
            return clazz != null && target.isAssignableTo(clazz);
        }

        private Class<?> getType() {
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                return classLoader != null ? classLoader.loadClass(type) : Class.forName(type);
            } catch (NoClassDefFoundError ignore) {
                return null;
            } catch (Throwable e) {
                logger.error("class is not found in context class loader. " + type);
                return null;
            }
        }
    }

}
