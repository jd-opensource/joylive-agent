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
package com.jd.live.agent.implement.servlet.tomcat.v9;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.core.util.map.MultiLinkedMap;
import com.jd.live.agent.core.util.map.MultiMap;
import com.jd.live.agent.governance.request.HeaderProvider;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.apache.tomcat.util.http.MimeHeaders;

import javax.servlet.http.HttpServletRequest;


public class TomcatHeaderProvider implements HeaderProvider {

    private final HttpServletRequest request;

    public TomcatHeaderProvider(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public MultiMap<String, String> getHeaders() {
        Request req = null;
        if (request instanceof RequestFacade) {
            req = Accessor.request == null ? null : (Request) Accessor.request.get(request);
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
            return HttpUtils.parseHeader(request.getHeaderNames(), request::getHeaders);
        }
    }

    private static class Accessor {

        private static final FieldAccessor request = FieldAccessorFactory.getAccessor(RequestFacade.class, "request");

    }
}
