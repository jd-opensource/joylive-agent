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
package com.jd.live.agent.plugin.router.springgateway.v2.request;

import com.jd.live.agent.governance.request.header.HeaderUpdater;
import org.springframework.http.HttpHeaders;

/**
 * A class that implements the {@link HeaderUpdater} interface to update HTTP headers.
 * This class provides methods to add and set headers in the given {@link HttpHeaders} instance.
 */
public class HttpHeadersUpdater implements HeaderUpdater {

    private final HttpHeaders headers;

    public HttpHeadersUpdater(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    public void addHeader(String key, String value) {
        headers.add(key, value);
    }

    @Override
    public void setHeader(String key, String value) {
        headers.set(key, value);
    }
}
