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
package com.jd.live.agent.plugin.application.springboot.v2.util.port.web;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.plugin.application.springboot.v2.context.SpringAppContext;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortDetector;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortDetectorFactory;
import com.jd.live.agent.plugin.application.springboot.v2.util.port.PortInfo;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.core.io.ResourceLoader;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * A factory class that provides a PortDetector instance based on the given AppContext.
 */
public class WebPortDetectorFactory implements PortDetectorFactory {

    /**
     * Returns a PortDetector instance based on the given AppContext.
     *
     * @param context The AppContext.
     * @return A PortDetector instance.
     */
    public PortDetector get(AppContext context) {
        return WebPortDetector.isWebServerEnabled(context) ? new WebPortDetector(context) : null;
    }

    private static class WebPortDetector implements PortDetector {

        private static final String TYPE_WEB_SERVER_APPLICATION_CONTEXT = "org.springframework.boot.web.context.WebServerApplicationContext";
        private static final Class<?> CLASS_WEB_SERVER_APPLICATION_CONTEXT = loadClass(TYPE_WEB_SERVER_APPLICATION_CONTEXT, ResourceLoader.class.getClassLoader());
        private static final Class<?> CLASS_SERVER_PROPERTIES = loadClass("org.springframework.boot.autoconfigure.web.ServerProperties", ResourceLoader.class.getClassLoader());
        private static final FieldAccessor ACCESSOR_SSL = getAccessor(CLASS_SERVER_PROPERTIES, "ssl");

        private final AppContext context;

        WebPortDetector(AppContext context) {
            this.context = context;
        }

        public static boolean isWebServerEnabled(AppContext context) {
            if (context instanceof SpringAppContext) {
                SpringAppContext springContext = (SpringAppContext) context;
                if (CLASS_WEB_SERVER_APPLICATION_CONTEXT != null && CLASS_WEB_SERVER_APPLICATION_CONTEXT.isInstance(springContext.getContext())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public PortInfo getPort() {
            WebServerApplicationContext wsac = (WebServerApplicationContext) ((SpringAppContext) context).getContext();
            WebServer webServer = wsac.getWebServer();
            int port = webServer.getPort();
            boolean secure = false;
            if (ACCESSOR_SSL != null) {
                try {
                    Object bean = wsac.getBean(CLASS_SERVER_PROPERTIES);
                    secure = ACCESSOR_SSL.get(bean) != null;
                } catch (Throwable ignored) {
                }
            }
            return new PortInfo(port, secure);
        }
    }

}
