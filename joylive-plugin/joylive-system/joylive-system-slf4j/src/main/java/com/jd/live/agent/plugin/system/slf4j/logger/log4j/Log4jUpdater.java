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
package com.jd.live.agent.plugin.system.slf4j.logger.log4j;

import com.jd.live.agent.plugin.system.slf4j.logger.AbstractLevelUpdater;
import org.slf4j.Logger;

/**
 * A class that updates the logging level for log4j loggers.
 */
public class Log4jUpdater extends AbstractLevelUpdater {

    @Override
    public boolean support(Logger logger) {
        return logger.getClass().getName().startsWith("org.apache.log4j");
    }

}
