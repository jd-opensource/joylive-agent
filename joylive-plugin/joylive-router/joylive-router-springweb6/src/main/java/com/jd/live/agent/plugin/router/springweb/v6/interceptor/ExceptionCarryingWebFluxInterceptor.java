package com.jd.live.agent.plugin.router.springweb.v6.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jd.live.agent.core.Constants.DEFAULT_HEADER_SIZE_LIMIT;
import static com.jd.live.agent.core.util.ExceptionUtils.*;

/**
 * @author Axkea
 */
public class ExceptionCarryingWebFluxInterceptor extends InterceptorAdaptor {

    @Override
    public void onEnter(ExecutableContext ctx) {
        Throwable t = ctx.getArgument(0);
        ServerWebExchange exchange = ctx.getArgument(3);
        HttpHeaders headers = exchange.getResponse().getHeaders();

        exceptionHeaders(t, headers::set);
    }
}
