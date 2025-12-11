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
package com.jd.live.agent.implement.bean.mcp.util;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import org.springframework.core.io.ResourceLoader;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

public class SpringUtils {

    private static final ClassLoader CLASS_LOADER = ResourceLoader.class.getClassLoader();

    private static final String TYPE_PARAMETER_NAME_DISCOVERER = "org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory";
    private static final Class<?> CLASS_PARAMETER_NAME_DISCOVERER = loadClass(TYPE_PARAMETER_NAME_DISCOVERER, CLASS_LOADER);
    private static final FieldAccessor ACCESSOR_PARAMETER_NAME_DISCOVERER = getAccessor(CLASS_PARAMETER_NAME_DISCOVERER, "parameterNameDiscoverer");

    @SuppressWarnings("unchecked")
    public static <T> T getParameterNameDiscoverer(Object factory) {
        return (T) ACCESSOR_PARAMETER_NAME_DISCOVERER.get(factory);
    }

}
