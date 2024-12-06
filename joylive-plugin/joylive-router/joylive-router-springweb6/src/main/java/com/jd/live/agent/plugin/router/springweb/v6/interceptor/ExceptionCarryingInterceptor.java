package com.jd.live.agent.plugin.router.springweb.v6.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import jakarta.servlet.http.HttpServletResponse;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.jd.live.agent.core.Constants.DEFAULT_HEADER_SIZE_LIMIT;
import static com.jd.live.agent.core.Constants.EXCEPTION_MESSAGE_LABEL;
import static com.jd.live.agent.core.Constants.EXCEPTION_NAMES_LABEL;
import static com.jd.live.agent.core.util.ExceptionUtils.asString;
import static com.jd.live.agent.core.util.ExceptionUtils.getExceptions;

/**
 * @author Axkea
 */
public class ExceptionCarryingInterceptor extends InterceptorAdaptor {

    private static final Set<String> exclude = new HashSet<>(Arrays.asList(
            "java.util.concurrent.ExecutionException",
            "java.lang.reflect.InvocationTargetException"
    ));
    @Override
    public void onEnter(ExecutableContext ctx) {
        HttpServletResponse response = ctx.getArgument(1);
        Exception ex = ctx.getArgument(3);
        String exceptionNames = asString(getExceptions(ex, e -> !exclude.contains(e.getClass().getName())), ',', DEFAULT_HEADER_SIZE_LIMIT);
        String message = ex.getMessage();
        if (exceptionNames != null && !exceptionNames.isEmpty()) {
            response.setHeader(EXCEPTION_NAMES_LABEL, exceptionNames);
        }
        if (message != null && !message.isEmpty()) {
            try {
                String encodeMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.name());
                response.setHeader(EXCEPTION_MESSAGE_LABEL, encodeMessage);
            } catch (UnsupportedEncodingException ignore) {
            }
        }
    }
}
