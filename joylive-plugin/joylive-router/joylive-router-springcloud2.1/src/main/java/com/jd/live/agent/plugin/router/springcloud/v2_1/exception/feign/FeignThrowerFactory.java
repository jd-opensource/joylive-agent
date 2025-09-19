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
package com.jd.live.agent.plugin.router.springcloud.v2_1.exception.feign;

import com.jd.live.agent.plugin.router.springcloud.v2_1.exception.ThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v2_1.request.FeignOutboundRequest;
import feign.FeignException;
import feign.Request;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Constructor;

import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredConstructor;

/**
 * A concrete implementation of {@link ThrowerFactory} that creates exceptions specifically
 */
public class FeignThrowerFactory<R extends FeignOutboundRequest> implements ThrowerFactory<FeignException, R> {

    // feign 10.12.0+
    private static final Constructor<?> constructor1 = getDeclaredConstructor(FeignException.class, new Class[]{int.class, String.class, Request.class, Throwable.class});
    // feign 10.1.0
    private static final Constructor<?> constructor2 = getDeclaredConstructor(FeignException.class, new Class[]{int.class, String.class, byte[].class});

    @Override
    public FeignException createException(R request, HttpStatus status, String message, Throwable throwable) {
        return createException(request.getRequest(), status.value(), message, throwable);
    }

    public FeignException createException(Request request, int status, String message, Throwable throwable) {
        if (throwable instanceof FeignException) {
            return (FeignException) throwable;
        }
        try {
            if (constructor1 != null) {
                return (FeignException) constructor1.newInstance(status, message, request, throwable);
            }
            return (FeignException) constructor2.newInstance(status, message, request.body());
        } catch (Throwable ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
}
