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
package com.jd.live.agent.plugin.transmission.servlet.javax.request.tomcat;

import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.core.util.map.MultiLinkedMap;
import com.jd.live.agent.core.util.map.MultiMap;
import com.jd.live.agent.plugin.transmission.servlet.javax.request.HttpHeaderParser;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.apache.tomcat.util.http.MimeHeaders;

import javax.servlet.http.HttpServletRequest;

/**
 * An implementation of HttpHeaderParser that is compatible with Tomcat's Request and RequestFacade classes.
 */
public class TomcatHeaderParser implements HttpHeaderParser {

    private static final UnsafeFieldAccessor accessor = UnsafeFieldAccessorFactory.getQuietly(RequestFacade.class, "request");

    @Override
    public MultiMap<String, String> parse(Object request) {
        Request req = null;
        if (request instanceof RequestFacade) {
            req = accessor == null ? null : (Request) accessor.get(request);
        } else if (request instanceof Request) {
            req = (Request) request;
        }
        if (req != null) {
            MimeHeaders mimeHeaders = req.getCoyoteRequest().getMimeHeaders();
            int count = mimeHeaders.size();
            if (count == 0) {
                return null;
            }
            MultiMap<String, String> result = MultiLinkedMap.caseInsensitive(count);
            for (int i = 0; i < count; i++) {
                result.add(mimeHeaders.getName(i).toString(), mimeHeaders.getValue(i).toString());
            }
            return result;
        } else {
            HttpServletRequest hsr = (HttpServletRequest) request;
            return HttpUtils.parseHeader(hsr.getHeaderNames(), hsr::getHeaders);
        }
    }
}
