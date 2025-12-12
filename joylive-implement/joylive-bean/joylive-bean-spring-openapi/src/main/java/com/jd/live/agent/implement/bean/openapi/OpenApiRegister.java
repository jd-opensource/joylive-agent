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
package com.jd.live.agent.implement.bean.openapi;

import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.openapi.spec.v3.OpenApiFactory;
import com.jd.live.agent.implement.bean.openapi.util.SpringUtils;

@Extension(value = "OpenApiRegister", order = AppListener.ORDER_OPEN_API)
@ConditionalOnClass("org.springframework.context.ConfigurableApplicationContext")
public class OpenApiRegister extends AppListener.AppListenerAdapter {

    @Override
    public void onStarted(AppContext context) {

        OpenApiFactory.INSTANCE_REF.set(SpringUtils.getApiFactory(context));
    }
}
