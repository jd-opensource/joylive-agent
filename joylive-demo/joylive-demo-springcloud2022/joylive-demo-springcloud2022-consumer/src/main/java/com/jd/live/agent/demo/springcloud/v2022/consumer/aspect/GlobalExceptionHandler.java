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
package com.jd.live.agent.demo.springcloud.v2022.consumer.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.live.agent.demo.response.LiveLocation;
import com.jd.live.agent.demo.response.LiveResponse;
import com.jd.live.agent.demo.response.LiveTrace;
import com.jd.live.agent.demo.response.LiveTransmission;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private ObjectMapper objectMapper;

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public LiveResponse handleException(Exception e, HttpServletRequest request) {
        LiveResponse response = getResponse(e);
        if (response == null) {
            response = new LiveResponse(500, "Internal Server Error: " + e.getMessage());
        }
        response.addFirst(new LiveTrace(applicationName, LiveLocation.build(),
                LiveTransmission.build("header", request::getHeader)));
        return response;
    }

    private LiveResponse getResponse(Exception e) {
        LiveResponse response = null;
        byte[] body = null;
        if (e instanceof RestClientResponseException) {
            RestClientResponseException exception = (RestClientResponseException) e;
            body = exception.getResponseBodyAsByteArray();
        } else if (e instanceof FeignException) {
            FeignException exception = (FeignException) e;
            body = exception.content();
        } else if (e instanceof WebClientResponseException) {
            WebClientResponseException responseException = (WebClientResponseException) e;
            body = responseException.getResponseBodyAsByteArray();
        }
        if (body != null) {
            try {
                response = objectMapper.readValue(body, LiveResponse.class);
            } catch (Throwable ignore) {
            }
        }
        return response;
    }

}