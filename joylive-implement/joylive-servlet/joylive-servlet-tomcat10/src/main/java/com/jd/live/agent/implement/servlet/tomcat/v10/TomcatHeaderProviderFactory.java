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
package com.jd.live.agent.implement.servlet.tomcat.v10;

import com.jd.live.agent.core.extension.annotation.ConditionalOnType;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.request.HeaderProvider;
import com.jd.live.agent.governance.request.HeaderProviderFactory;
import jakarta.servlet.http.HttpServletRequest;

@Extension("tomcat_v10")
@ConditionalOnType(TomcatHeaderProviderFactory.TYPE_REQUEST)
@ConditionalOnType(TomcatHeaderProviderFactory.TYPE_JAKARTA_SERVLET_REQUEST)
public class TomcatHeaderProviderFactory implements HeaderProviderFactory {

    protected static final String TYPE_REQUEST_FACADE = "org.apache.catalina.connector.RequestFacade";

    protected static final String TYPE_REQUEST = "org.apache.catalina.connector.Request";

    protected static final String TYPE_JAKARTA_SERVLET_REQUEST = "jakarta.servlet.http.HttpServletRequest";

    @Override
    public HeaderProvider create(Object request) {
        return new TomcatHeaderProvider((HttpServletRequest) request);
    }

    @Override
    public String[] getSupportTypes() {
        return new String[]{TYPE_REQUEST_FACADE, TYPE_REQUEST};
    }
}
