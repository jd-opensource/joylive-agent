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
package com.jd.live.agent.implement.logger.slf4j;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerBridge;
import com.jd.live.agent.core.config.AgentPath;
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.implement.logger.slf4j.ansi.AnsiOutput;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Injectable
@Extension("slf4j")
public class SLF4JBridge implements LoggerBridge, ExtensionInitializer {

    private static final String KEY_LIVE_LOG_DIR = "LIVE_LOG_DIR";

    private static final String KEY_LIVE_APP_NAME = "LIVE_APP_NAME";

    private static final String KEY_PID = "PID";

    @Inject
    private AgentPath agentPath;

    @Inject
    private Application application;

    @Override
    public Logger getLogger(Class<?> clazz) {
        return new SLF4JLogger(LoggerFactory.getLogger(clazz));
    }

    @Override
    public void initialize() {
        boolean console = System.console() != null;
        if (!console) {
            Map<String, String> env = System.getenv();
            String term = env.get("TERM");
            if (term != null) {
                console = term.startsWith("xterm");
            } else {
                console = "1".equals(env.get("CLICOLOR"));
            }
        }
        if (console) {
            AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
        }
        checkAndSet(KEY_LIVE_LOG_DIR, agentPath.getLogPath().getPath());
        checkAndSet(KEY_LIVE_APP_NAME, application.getName());
        checkAndSet(KEY_PID, String.valueOf(application.getPid()));
    }

    private void checkAndSet(String key, String value) {
        String config = System.getProperty(key, System.getenv(key));
        if ((config == null || config.isEmpty()) && (value != null && !value.isEmpty())) {
            System.setProperty(key, value);
        }
    }
}
