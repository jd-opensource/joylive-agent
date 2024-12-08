package com.jd.live.agent.plugin.router.springweb.v5.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.router.springweb.v5.interceptor.ExceptionCarryingWebFluxInterceptor;

/**
 * @author Axkea
 */
@Injectable
@Extension(value = "ExceptionCarryingWebFluxDefinition_v5")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_SPRING_ENABLED, matchIfMissing = true)
@ConditionalOnClass(ExceptionCarryingWebFluxDefinition.TYPE_REQUEST_MAPPING_HANDLER_ADAPTER)
@ConditionalOnMissingClass(DispatcherHandlerDefinition.TYPE_ERROR_RESPONSE)
public class ExceptionCarryingWebFluxDefinition extends PluginDefinitionAdapter {
    protected static final String TYPE_REQUEST_MAPPING_HANDLER_ADAPTER = "org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter";

    private static final String METHOD_HANDLE_EXCEPTION = "handleException";


    public ExceptionCarryingWebFluxDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_REQUEST_MAPPING_HANDLER_ADAPTER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_HANDLE_EXCEPTION), ExceptionCarryingWebFluxInterceptor::new
                )
        };
    }
}
