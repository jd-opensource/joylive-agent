package com.jd.live.agent.plugin.router.springweb.v5.interceptor;

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
import static com.jd.live.agent.core.util.ExceptionUtils.asString;
import static com.jd.live.agent.core.util.ExceptionUtils.getExceptions;

/**
 * @author Axkea
 */
public class ExceptionCarryingWebFluxInterceptor extends InterceptorAdaptor {

    private static final Set<String> exclude = new HashSet<>(Arrays.asList(
            "java.util.concurrent.ExecutionException",
            "java.lang.reflect.InvocationTargetException"
    ));

    @Override
    public void onEnter(ExecutableContext ctx) {
        Throwable t = ctx.getArgument(0);
        String exceptionNames = asString(getExceptions(t, e -> !exclude.contains(e.getClass().getName())), ',', DEFAULT_HEADER_SIZE_LIMIT);
        String message = t.getMessage();
        ServerWebExchange exchange = ctx.getArgument(3);
        HttpHeaders headers = exchange.getResponse().getHeaders();

        if (exceptionNames != null && !exceptionNames.isEmpty()) {
            List<String> header = new ArrayList<>(1);
            header.add(exceptionNames);
            headers.put(Constants.EXCEPTION_NAMES_LABEL, header);
        }
        if (message != null && !message.isEmpty()) {
            try {
                String encodeMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.name());
                List<String> header = new ArrayList<>(1);
                header.add(encodeMessage);
                headers.put(Constants.EXCEPTION_MESSAGE_LABEL, header);
            } catch (UnsupportedEncodingException e) {
            }
        }
    }
}
