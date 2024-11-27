package com.jd.live.agent.plugin.router.springcloud.v3.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.router.springcloud.v3.interceptor.ExceptionCarryingInterceptor;

/**
 * @author Axkea
 */
@Injectable
@Extension(value = "ExceptionCarryingDefinition_v3")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_SPRING_ENABLED, matchIfMissing = true)
@ConditionalOnClass(ExceptionCarryingDefinition.TYPE_DISPATCHER_SERVLET)
@ConditionalOnMissingClass(BlockingClusterDefinition.TYPE_HTTP_STATUS_CODE)
public class ExceptionCarryingDefinition extends PluginDefinitionAdapter {
    protected static final String TYPE_DISPATCHER_SERVLET = "org.springframework.web.servlet.DispatcherServlet";

    protected static final String METHOD_EXECUTE = "processHandlerException";

    @Inject(ObjectParser.JSON)
    private ObjectParser parser;

    public ExceptionCarryingDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_DISPATCHER_SERVLET);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXECUTE), () -> new ExceptionCarryingInterceptor(parser)
                )
        };
    }
}
