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
package com.jd.live.agent.plugin.registry.springcloud.v1.util;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import org.springframework.context.ApplicationContextAware;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

public class CloudUtils {

    private static final Class<?> FEIGN_CLIENT_TYPE = loadClass("org.springframework.cloud.netflix.feign.FeignClientFactoryBean",
            ApplicationContextAware.class.getClassLoader());

    private static final FieldAccessor feignName = getAccessor(FEIGN_CLIENT_TYPE, "name");

    private static final FieldAccessor feignUrl = getAccessor(FEIGN_CLIENT_TYPE, "url");

    public static String getFeignName(Object bean) {
        return feignName.get(bean, String.class);
    }

    public static String getFeignUrl(Object bean) {
        return feignUrl.get(bean, String.class);
    }
}
