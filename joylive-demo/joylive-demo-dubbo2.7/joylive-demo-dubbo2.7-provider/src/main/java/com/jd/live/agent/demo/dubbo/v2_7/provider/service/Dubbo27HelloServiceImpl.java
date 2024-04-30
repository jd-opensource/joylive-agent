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
package com.jd.live.agent.demo.dubbo.v2_7.provider.service;

import com.jd.live.agent.demo.service.HelloService;
import com.jd.live.agent.demo.service.impl.HelloServiceImpl;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcContext;

@DubboService(group = "live-demo", interfaceClass = HelloService.class)
public class Dubbo27HelloServiceImpl extends HelloServiceImpl {

    @Override
    protected StringBuilder attachTag(StringBuilder builder) {
        RpcContext context = RpcContext.getContext();
        return builder.append(", header:{").
                append("x-live-space-id=").append(context.getAttachment("x-live-space-id")).
                append(", x-live-rule-id=").append(context.getAttachment("x-live-rule-id")).
                append(", x-live-uid=").append(context.getAttachment("x-live-uid")).
                append(", x-lane-space-id=").append(context.getAttachment("x-lane-space-id")).
                append(", x-lane-code=").append(context.getAttachment("x-lane-code")).
                append("}, location:{").
                append("liveSpaceId=").append(System.getProperty("x-live-space-id")).
                append(", unit=").append(System.getProperty("x-live-unit")).
                append(", cell=").append(System.getProperty("x-live-cell")).
                append(", laneSpaceId=").append(System.getProperty("x-lane-space-id")).
                append(", lane=").append(System.getProperty("x-lane-code")).
                append("}");
    }

}
