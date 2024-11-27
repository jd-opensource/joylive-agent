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
package com.jd.live.agent.plugin.router.springcloud.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.exception.ExceptionMessage;
import org.apache.catalina.connector.ResponseFacade;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Axkea
 */
public class ExceptionCarryingInterceptor extends InterceptorAdaptor {

    //The default buffer size of tomcat's response header is 1024 * 8
    private static final int DEFAULT_HEADER_SIZE_LIMIT = 1024 * 4;

    private static final String EXCEPTION_CLASS_NAME = "java.lang.Exception";
    private static final String THROWABLE_CLASS_NAME = "java.lang.Throwable";

    private final ObjectParser parser;

    public ExceptionCarryingInterceptor(ObjectParser parser) {
        this.parser = parser;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        ResponseFacade response = (ResponseFacade) arguments[1];
        Exception ex = (Exception) arguments[3];
        ExceptionMessage exceptionMessage = ExceptionMessage.build(ex);
        String message = parseToJson(exceptionMessage);
        if (message.getBytes().length > DEFAULT_HEADER_SIZE_LIMIT) {
            Set<String> exclude = new HashSet<>();
            exclude.add(EXCEPTION_CLASS_NAME);
            exclude.add(THROWABLE_CLASS_NAME);
            exceptionMessage.compress(exclude);
            message = parseToJson(exceptionMessage);
        }
        if (message.getBytes().length <= DEFAULT_HEADER_SIZE_LIMIT) {
            response.setHeader(Constants.EXCEPTION_MESSAGE_LABEL, message);
        }
    }

    private String parseToJson(ExceptionMessage exceptionMessage) {
        StringWriter stringWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
        parser.write(bufferedWriter, exceptionMessage);
        return stringWriter.toString();
    }

}
