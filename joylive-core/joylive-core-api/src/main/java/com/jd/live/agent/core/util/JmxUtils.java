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
package com.jd.live.agent.core.util;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Utility class for interacting with JMX MBeans.
 */
public class JmxUtils {

    /**
     * Retrieves a list of MBeans that match the given query name and converts them using the provided converter function.
     *
     * @param queryName the query name to match MBeans. It should be a valid ObjectName pattern.
     * @param converter a function to convert the MBean object to the desired type. If null, the MBean object itself is returned.
     * @param <T>       the type of the objects to be returned.
     * @return a list of objects of type T representing the MBeans that match the query name.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getMBeans(String queryName, Function<Object, T> converter) {
        List<T> result = new ArrayList<>();
        if (queryName != null && !queryName.isEmpty()) {
            try {
                MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                Class<?> dynamicMBean2Class = Class.forName("com.sun.jmx.mbeanserver.DynamicMBean2");
                Method getResourceMethod = dynamicMBean2Class.getDeclaredMethod("getResource");
                Class<?> nameamedObjectClass = Class.forName("com.sun.jmx.mbeanserver.NamedObject");
                Method getObjectMethod = nameamedObjectClass.getDeclaredMethod("getObject");

                Field msbInterceptorField = server.getClass().getDeclaredField("mbsInterceptor");
                msbInterceptorField.setAccessible(true);
                Object mbsInterceptor = msbInterceptorField.get(server);

                Field repositoryField = mbsInterceptor.getClass().getDeclaredField("repository");
                repositoryField.setAccessible(true);
                Object repository = repositoryField.get(mbsInterceptor);

                Field domainTbField = repository.getClass().getDeclaredField("domainTb");
                domainTbField.setAccessible(true);
                Map<String, Map<String, ?>> domainTb = (Map<String, Map<String, ?>>) domainTbField.get(repository);

                Set<ObjectInstance> instances = server.queryMBeans(new ObjectName(queryName), null);
                for (ObjectInstance instance : instances) {
                    ObjectName objectName = instance.getObjectName();
                    Object namedObject = domainTb.get(objectName.getDomain()).get(objectName.getCanonicalKeyPropertyListString());
                    Object dynamicMBean = getObjectMethod.invoke(namedObject);
                    Object mbean = getResourceMethod.invoke(dynamicMBean);
                    T target = converter != null ? converter.apply(mbean) : (T) mbean;
                    if (target != null) {
                        result.add(target);
                    }
                }
                return result;
            } catch (Throwable ignore) {
            }
        }
        return result;
    }
}
