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
package com.jd.live.agent.bootstrap.logger;

/**
 * A bridge implementation that creates instances of a Logger that delegate to the Java Util Logging (JUL) framework.
 * This class is responsible for creating logger instances that can be used throughout an application to log messages via JUL.
 *
 * @since 1.0.0
 */
public class JULBridge implements LoggerBridge {

    @Override
    public Logger getLogger(Class<?> clazz) {
        return new JULLogger(java.util.logging.Logger.getLogger(clazz.getName()));
    }
}
