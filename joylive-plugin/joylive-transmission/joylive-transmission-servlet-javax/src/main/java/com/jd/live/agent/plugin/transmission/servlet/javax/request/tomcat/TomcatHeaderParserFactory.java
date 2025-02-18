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
package com.jd.live.agent.plugin.transmission.servlet.javax.request.tomcat;

import com.jd.live.agent.plugin.transmission.servlet.javax.request.HttpHeaderParser;
import com.jd.live.agent.plugin.transmission.servlet.javax.request.HttpHeaderParserFactory;

/**
 * A factory for creating instances of HttpHeaderParser that are compatible with Tomcat's Request and RequestFacade classes.
 */
public class TomcatHeaderParserFactory implements HttpHeaderParserFactory {

    private static final String TYPE_REQUEST_FACADE = "org.apache.catalina.connector.RequestFacade";
    private static final String TYPE_REQUEST = "org.apache.catalina.connector.Request";

    @Override
    public HttpHeaderParser create() {
        return new TomcatHeaderParser();
    }

    @Override
    public boolean support(Class<?> type) {
        String name = type.getName();
        return name.equals(TYPE_REQUEST_FACADE) || name.equals(TYPE_REQUEST);
    }

    public static boolean isValid() {
        try {
            Class.forName(TYPE_REQUEST);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
