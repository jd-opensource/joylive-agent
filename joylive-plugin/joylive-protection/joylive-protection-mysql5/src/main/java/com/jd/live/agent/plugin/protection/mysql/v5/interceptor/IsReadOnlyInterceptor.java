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
package com.jd.live.agent.plugin.protection.mysql.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.interceptor.AbstractDbInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.request.DbRequest;

/**
 * CreateNewIOInterceptor
 */
public class IsReadOnlyInterceptor extends AbstractDbInterceptor {

    public IsReadOnlyInterceptor(PolicySupplier policySupplier) {
        super(policySupplier);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Boolean systemFlag = RequestContext.getAttribute(DbRequest.SYSTEM_REQUEST);
        if (systemFlag == null || !systemFlag) {
            RequestContext.setAttribute(DbRequest.SYSTEM_REQUEST, Boolean.TRUE);
            ctx.setAttribute(DbRequest.SYSTEM_REQUEST, Boolean.TRUE);
        }
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        Boolean systemFlag = ctx.getAttribute(DbRequest.SYSTEM_REQUEST);
        if (systemFlag != null && systemFlag) {
            RequestContext.removeAttribute(DbRequest.SYSTEM_REQUEST);
        }
    }
}
