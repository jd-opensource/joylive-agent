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
package com.jd.live.agent.plugin.router.springcloud.v2_2.exception.feign;

import com.jd.live.agent.plugin.router.springcloud.v2_2.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v2_2.request.FeignOutboundRequest;
import feign.FeignException;

public class FeignThrower extends SpringOutboundThrower<FeignException, FeignOutboundRequest> {

    public static final FeignThrower INSTANCE = new FeignThrower(new FeignThrowerFactory<>());

    public FeignThrower(FeignThrowerFactory<FeignOutboundRequest> factory) {
        super(factory);
    }
}
