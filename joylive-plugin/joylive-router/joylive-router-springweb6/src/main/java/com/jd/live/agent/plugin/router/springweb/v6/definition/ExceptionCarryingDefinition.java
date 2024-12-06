package com.jd.live.agent.plugin.router.springweb.v6.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.router.springweb.v6.interceptor.ExceptionCarryingInterceptor;

/**
 * @author Axkea
 */
@Injectable
@Extension(value = "ExceptionCarryingJakartaDefinition_v6")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_SPRING_ENABLED, matchIfMissing = true)
@ConditionalOnClass(ExceptionCarryingDefinition.TYPE_DISPATCHER_SERVLET)
@ConditionalOnClass(DispatcherHandlerDefinition.TYPE_ERROR_RESPONSE)
public class ExceptionCarryingDefinition extends PluginDefinitionAdapter {
    protected static final String TYPE_DISPATCHER_SERVLET = "org.springframework.web.servlet.DispatcherServlet";

    protected static final String METHOD_PROCESS_HANDLER_EXCEPTION = "processHandlerException";


    public ExceptionCarryingDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_DISPATCHER_SERVLET);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_PROCESS_HANDLER_EXCEPTION), ExceptionCarryingInterceptor::new
                )
        };
    }
}
