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
package com.jd.live.agent.bootstrap.util.type;

import java.lang.reflect.AccessibleObject;

/**
 * A utility class for accessing and modifying the accessibility of accessible objects.
 */
public class Accessible {

    private static final FieldAccessor unsafe = FieldAccessorFactory.getAccessor(AccessibleObject.class, "override");

    /**
     * Sets the accessibility of the specified accessible object.
     *
     * @param target     the accessible object to modify
     * @param accessible true if the accessible object should be accessible, false otherwise
     */
    public static void setAccessible(AccessibleObject target, boolean accessible) {
        if (unsafe != null) {
            unsafe.set(target, accessible);
        } else {
            target.setAccessible(accessible);
        }
    }
}
