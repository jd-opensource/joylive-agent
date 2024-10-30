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
package com.jd.live.agent.plugin.transmission.dubbo.v2_7.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.tag.Label;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.CargoRequire;
import com.jd.live.agent.governance.context.bag.CargoRequires;
import com.jd.live.agent.governance.context.bag.Carrier;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;

import java.util.List;

import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;

public class DubboConsumerInterceptor extends InterceptorAdaptor {

    private final CargoRequire require;

    public DubboConsumerInterceptor(List<CargoRequire> requires) {
        this.require = new CargoRequires(requires);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        attachTag((RpcInvocation) ctx.getArguments()[0]);
    }

    private void attachTag(RpcInvocation invocation) {
        Carrier carrier = RequestContext.getOrCreate();
        carrier.addCargo(require, RpcContext.getContext().getObjectAttachments(), Label::parseValue);
        carrier.cargos(tag -> invocation.setAttachment(tag.getKey(), tag.getValue()));
        Invoker<?> invoker = invocation.getInvoker();
        if (invoker != null) {
            URL url = invoker.getUrl();
            if (SERVICE_REGISTRY_TYPE.equals(url.getParameter(REGISTRY_TYPE_KEY))) {
                invocation.setAttachmentIfAbsent(REGISTRY_TYPE_KEY, SERVICE_REGISTRY_TYPE);
            }
        }
    }

}
