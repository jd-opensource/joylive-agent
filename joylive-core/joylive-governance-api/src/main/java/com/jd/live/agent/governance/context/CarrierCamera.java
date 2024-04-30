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
package com.jd.live.agent.governance.context;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.thread.Camera;
import com.jd.live.agent.governance.context.bag.Carrier;

/**
 * CarrierCamera is an implementation of the Camera interface that provides functionality
 * to snapshot, restore, and remove the current request context, specifically for
 * a carrier-related context which might include information relevant to a carrier service.
 */
@Extension("CarrierCamera")
public class CarrierCamera implements Camera {

    @Override
    public Object snapshot() {
        return RequestContext.get();
    }

    @Override
    public void restore(Object snapshot) {
        RequestContext.set((Carrier) snapshot);
    }

    @Override
    public void remove() {
        RequestContext.remove();
    }
}
