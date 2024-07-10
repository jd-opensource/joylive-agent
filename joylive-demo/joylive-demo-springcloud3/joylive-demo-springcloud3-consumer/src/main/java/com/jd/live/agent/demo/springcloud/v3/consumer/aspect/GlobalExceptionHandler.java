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
package com.jd.live.agent.demo.springcloud.v3.consumer.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.live.agent.demo.response.LiveLocation;
import com.jd.live.agent.demo.response.LiveResponse;
import com.jd.live.agent.demo.response.LiveTrace;
import com.jd.live.agent.demo.response.LiveTransmission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.application.name}")
    private String applicationName;

    @Resource
    private ObjectMapper objectMapper;

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public LiveResponse handleException(Exception e, HttpServletRequest request) {
        LiveResponse source = null;
        byte[] body = e instanceof RestClientResponseException ? ((RestClientResponseException) e).getResponseBodyAsByteArray() : null;
        if (body != null) {
            try {
                source = objectMapper.readValue(body, LiveResponse.class);
            } catch (Throwable ignore) {
            }
        }
        LiveResponse response = new LiveResponse(500, "Internal Server Error: " + e.getMessage());
        if (source != null) {
            for (LiveTrace trace : source.getTraces()) {
                response.addLast(trace);
            }
        }
        response.addFirst(new LiveTrace(applicationName, LiveLocation.build(),
                LiveTransmission.build("header", request::getHeader)));
        return response;
    }

}