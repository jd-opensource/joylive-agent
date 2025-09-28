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
package com.jd.live.agent.plugin.router.springcloud.v5.exception.feign;

import com.jd.live.agent.plugin.router.springcloud.v5.exception.ThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v5.request.FeignOutboundRequest;
import feign.FeignException;
import org.springframework.http.HttpStatus;

/**
 * A concrete implementation of {@link ThrowerFactory} that creates exceptions specifically
 */
public class FeignThrowerFactory<R extends FeignOutboundRequest> implements ThrowerFactory<FeignException, R> {

    @Override
    public FeignException createException(R request, HttpStatus status, String message, Throwable throwable) {
        if (throwable instanceof FeignException) {
            return (FeignException) throwable;
        }
        feign.Request req = request.getRequest();
        switch (status) {
            case SERVICE_UNAVAILABLE:
                return new FeignException.ServiceUnavailable(message, req, req.body(), req.headers());
            case FORBIDDEN:
                return new FeignException.Forbidden(message, req, req.body(), req.headers());
            default:
                return new FeignException.InternalServerError(message, req, req.body(), req.headers());
        }
    }
}
