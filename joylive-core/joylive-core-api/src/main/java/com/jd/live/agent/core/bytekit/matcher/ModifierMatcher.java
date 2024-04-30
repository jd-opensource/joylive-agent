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

import com.jd.live.agent.core.bytekit.type.ModifierDesc;

/**
 * ModifierMatcher
 *
 * @since 1.0.0
 */
public class ModifierMatcher<T extends ModifierDesc> extends AbstractJunction<T> {

    private final Mode mode;

    public ModifierMatcher(Mode mode) {
        this.mode = mode;
    }

    public static <T extends ModifierDesc> Junction<T> of(Mode mode) {
        return new ModifierMatcher<>(mode);
    }

    @Override
    public boolean match(T target) {
        return target != null && (mode.getModifiers() & target.getModifiers()) != 0;
    }

    public enum Mode {
        PUBLIC(0x0001, "isPublic()"),
        PRIVATE(0x0002, "isPrivate()"),
        PROTECTED(0x0004, "isProtected()"),
        STATIC(0x0008, "isStatic()"),
        FINAL(0x0010, "isFinal()"),
        ABSTRACT(0x0008, "isAbstract()"),
        INTERFACE(0x0400, "isInterface()"),
        ANNOTATION(0x2000, "isAnnotation()"),
        ENUMERATION(0x4000, "isEnum()");

        private final int modifiers;
        private final String description;

        Mode(int modifiers, String description) {
            this.modifiers = modifiers;
            this.description = description;
        }

        public int getModifiers() {
            return modifiers;
        }

        public String getDescription() {
            return description;
        }
    }
}
