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
package com.jd.live.agent.plugin.transmission.sofarpc.interceptor;

import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.context.bag.Propagation;

import static com.jd.live.agent.governance.context.bag.live.LivePropagation.LIVE_PROPAGATION;
import static com.jd.live.agent.governance.request.header.HeaderParser.StringHeaderParser.reader;
import static com.jd.live.agent.governance.request.header.HeaderParser.StringHeaderParser.writer;

public class SofaRpcClientInterceptor extends InterceptorAdaptor {

    private final Propagation propagation;

    public SofaRpcClientInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        SofaRequest request = (SofaRequest) ctx.getArguments()[0];
        attachTag(request);
    }

    private void attachTag(SofaRequest request) {
        Carrier carrier = RequestContext.getOrCreate();
        if (RpcInvokeContext.isBaggageEnable()) {
            LIVE_PROPAGATION.read(carrier, reader(RpcInvokeContext.getContext().getAllRequestBaggage()));
        }
        propagation.write(carrier, writer(request::addRequestProp));
    }
}
