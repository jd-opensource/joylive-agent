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
package com.jd.live.agent.core.context;

import com.jd.live.agent.core.config.AgentConfig;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.option.Option;
import lombok.Getter;

import java.lang.instrument.Instrumentation;

/**
 * The AgentContext class defines the basic properties and construction methods of the agent context.
 *
 * <p>
 * It contains various information required for agent operation, such as Instrumentation instance,
 * whether it is a dynamic agent, class loader, agent path, configuration options, agent configuration,
 * version and application instance.
 *
 * @since 1.0.0
 */
@Getter
public class AgentContext {

    /**
     * Instrumentation instance, used to provide various instrumentation-related services
     */
    protected Instrumentation instrumentation;

    /**
     * Mark whether the proxy is a dynamic proxy
     */
    protected boolean dynamic;

    /**
     * Class loader, used to load classes required by the agent
     */
    protected ClassLoader classLoader;

    /**
     * The proxy path encapsulates the path information where the proxy file is located
     */
    protected AgentPath agentPath;

    /**
     * Configuration options, encapsulates the configuration options when starting the agent
     */
    protected Option option;

    /**
     * Agent configuration, encapsulates the configuration information of the agent
     */
    protected AgentConfig agentConfig;

    /**
     * The version number of the agent
     */
    protected String version;

    /**
     * Application instance encapsulates application-related information
     */
    protected Application application;

    /**
     * Constructor method to initialize AgentContext instance.
     *
     * @param instrumentation Instrumentation instance
     * @param dynamic         Whether it is a dynamic proxy
     * @param classLoader     class loader
     * @param agentPath       agent path
     * @param option          configuration option
     * @param agentConfig     agent configuration
     * @param version         agent version number
     * @param application     application example
     */
    public AgentContext(Instrumentation instrumentation, boolean dynamic, ClassLoader classLoader, AgentPath agentPath,
                        Option option, AgentConfig agentConfig, String version, Application application) {
        this.instrumentation = instrumentation;
        this.dynamic = dynamic;
        this.classLoader = classLoader;
        this.agentPath = agentPath;
        this.option = option;
        this.agentConfig = agentConfig;
        this.version = version;
        this.application = application;
    }

}
