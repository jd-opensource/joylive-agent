/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

/**
 * SubTypeMatcher
 *
 * @param <T> types to be matched
 * @since 1.0.0
 */
public class SubTypeMatcher<T extends TypeDesc> extends AbstractJunction<T> {

    private static final Map<String, Set<String>> PARENT_TYPES = new ConcurrentHashMap<>(8192);

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

    /**
     * The entry point for safely getting parent types.
     * It now requires the target TypeDesc to access its TypePool for lookups.
     */
    private static <T extends TypeDesc> Set<String> getParentTypes(T target, BiPredicate<String, ClassLoader> exclusion) {
        // Fix deadlock issue by AI suggestion
        Set<String> types = PARENT_TYPES.get(target.getActualName());
        if (types != null) {
            return types;
        }
        types = loadParentTypes(target, exclusion);
        Set<String> olds = PARENT_TYPES.putIfAbsent(target.getActualName(), types);
        return olds != null ? olds : types;
    }

    /**
     * Loads all the types that are part of the inheritance hierarchy of the given type definition.
     *
     * @param typeDef The type definition to start from.
     * @return A set of strings representing the names of all the types in the inheritance hierarchy.
     */
    private static Set<String> loadParentTypes(TypeDesc typeDef, BiPredicate<String, ClassLoader> exclusion) {
        Set<String> result = new HashSet<>();
        Queue<TypeDesc> queue = new ArrayDeque<>();
        result.add(typeDef.getActualName());
        queue.add(typeDef);
        TypeDef current;
        TypeDesc desc;
        String name;
        while (!queue.isEmpty()) {
            current = queue.poll();
            for (TypeDesc.Generic generic : current.getInterfaces()) {
                desc = generic.asErasure();
                name = desc.getActualName();
                if ((exclusion == null || !exclusion.test(name, null)) && result.add(name)) {
                    queue.add(desc);
                }
            }
            TypeDesc.Generic parent = current.getSuperClass();
            if (parent != null) {
                desc = parent.asErasure();
                name = desc.getActualName();
                if ((exclusion == null || !exclusion.test(name, null)) && result.add(name)) {
                    queue.add(desc);
                }
            }
        }
        return result;
    }

    public static class SubNameMatcher<T extends TypeDesc> extends AbstractJunction<T> {

        private final String type;

        private BiPredicate<String, ClassLoader> exclusion;

        private final boolean implement;

        public SubNameMatcher(String type, BiPredicate<String, ClassLoader> exclusion) {
            this(type, exclusion, false);
        }

        public SubNameMatcher(String type, BiPredicate<String, ClassLoader> exclusion, boolean implement) {
            this.type = type;
            this.exclusion = exclusion;
            this.implement = implement;
        }

        @Override
        public boolean match(T target) {
            if (target == null || type == null || type.isEmpty() || implement && target.isInterface()) {
                return false;
            }
            return getParentTypes(target, exclusion).contains(type);
        }

    }

    public static class SubNamesMatcher<T extends TypeDesc> extends AbstractJunction<T> {

        private final Set<String> types;

        private BiPredicate<String, ClassLoader> exclusion;

        private final boolean implement;

        public SubNamesMatcher(Set<String> types, BiPredicate<String, ClassLoader> exclusion) {
            this(types, exclusion, false);
        }

        public SubNamesMatcher(Set<String> types, BiPredicate<String, ClassLoader> exclusion, boolean implement) {
            this.types = types;
            this.exclusion = exclusion;
            this.implement = implement;
        }

        @Override
        public boolean match(T target) {
            if (target == null || types == null || types.isEmpty() || implement && target.isInterface()) {
                return false;
            }
            Set<String> parentTypes = getParentTypes(target, exclusion);
            for (String type : types) {
                if (parentTypes.contains(type)) {
                    return true;
                }
            }
            return false;
        }

    }

}
