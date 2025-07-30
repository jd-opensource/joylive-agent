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
package com.jd.live.agent.plugin.application.springboot.v2.util;

import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.StringUtils;
import com.jd.live.agent.governance.bootstrap.ConfigurableAppContext;
import com.jd.live.agent.governance.doc.Document;
import com.jd.live.agent.governance.doc.ServiceAnchor;
import org.springframework.beans.BeansException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * Utility class for registering Spring MVC request mappings as service anchors.
 */
public class WebDoc {

    private static final Class<?> type = loadClass("org.springframework.web.bind.annotation.RequestMapping", ConfigurableAppContext.class.getClassLoader());

    private final Application application;

    private final ConfigurableApplicationContext context;

    public WebDoc(Application application, ConfigurableApplicationContext context) {
        this.application = application;
        this.context = context;
    }

    /**
     * Builds a document containing service anchors from Spring MVC controllers.
     * Scans for @Controller and @RestController beans and converts their request mappings to service anchors.
     *
     * @return document containing collected service anchors, or null if RequestMapping class not available
     */
    public Document build() {
        if (type == null) {
            return null;
        }
        return () -> {
            List<ServiceAnchor> anchors = new ArrayList<>(128);
            try {
                Map<String, Object> beans = context.getBeansWithAnnotation(Controller.class);
                beans.putAll(context.getBeansWithAnnotation(RestController.class));
                for (Map.Entry<String, Object> entry : beans.entrySet()) {
                    addAnchor(entry.getValue(), anchors, application);
                }
            } catch (BeansException ignored) {
            }
            return anchors;
        };
    }

    /**
     * Processes a controller bean and extracts its request mappings as service anchors.
     *
     * @param bean        Controller instance
     * @param anchors     List to store generated anchors
     * @param application Current application context
     */
    private void addAnchor(Object bean, List<ServiceAnchor> anchors, Application application) {
        Class<?> type = bean.getClass();
        RequestMapping cm = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);
        Map<MethodSignature, RequestMapping> mms = new HashMap<>();
        ReflectionUtils.doWithMethods(type, method -> {
            MethodSignature signature = new MethodSignature(method.getName(), method.getParameterTypes());
            if (!mms.containsKey(signature)) {
                RequestMapping mm = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mm != null) {
                    mms.put(signature, mm);
                }
            }
        }, ReflectionUtils.USER_DECLARED_METHODS);
        String[] cmPaths = cm == null ? new String[0] : cm.path();
        RequestMethod[] cmMethods = cm == null ? new RequestMethod[0] : cm.method();
        String service = application.getService().getName();
        String group = application.getService().getGroup();
        for (Map.Entry<MethodSignature, RequestMapping> entry : mms.entrySet()) {
            RequestMapping mm = entry.getValue();
            RequestMethod[] methods = mergeMethod(cmMethods, mm.method());
            String[] paths = mergePath(cmPaths, mm.path());
            paths = paths == null || paths.length == 0 ? new String[]{"/"} : paths;
            for (String path : paths) {
                if (methods == null || methods.length == 0) {
                    anchors.add(new ServiceAnchor(service, group, path, null));
                } else {
                    for (RequestMethod method : methods) {
                        anchors.add(new ServiceAnchor(service, group, path, method.name()));
                    }
                }
            }
        }
    }

    /**
     * Merges HTTP methods from class and method level annotations.
     * Method-level methods take precedence.
     *
     * @param sources Class-level methods
     * @param targets Method-level methods
     * @return Merged methods
     */
    private RequestMethod[] mergeMethod(RequestMethod[] sources, RequestMethod[] targets) {
        if (targets == null || targets.length == 0) {
            return sources;
        }
        return targets;
    }

    /**
     * Merges URL paths from class and method level annotations.
     * Combines them using URL path joining rules.
     *
     * @param sources Class-level paths
     * @param targets Method-level paths
     * @return Combined paths
     */
    private String[] mergePath(String[] sources, String[] targets) {
        if (sources == null || sources.length == 0) {
            return targets;
        } else if (targets == null || targets.length == 0) {
            return sources;
        }
        List<String> targetList = new ArrayList<>(sources.length + targets.length);
        for (String target : targets) {
            for (String source : sources) {
                targetList.add(StringUtils.url(source, target));
            }
        }
        return targetList.toArray(new String[0]);
    }

    /**
     * Represents a unique method signature (name + parameter types).
     * Used to avoid duplicate method processing.
     */
    private static class MethodSignature {

        private final String name;

        private final Class<?>[] parameterTypes;

        MethodSignature(String name, Class<?>[] parameterTypes) {
            this.name = name;
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MethodSignature)) return false;
            MethodSignature that = (MethodSignature) o;
            return Objects.equals(name, that.name) && Objects.deepEquals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, Arrays.hashCode(parameterTypes));
        }
    }
}
