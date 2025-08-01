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
package com.jd.live.agent.plugin.application.springboot.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.doc.DocumentRegistry;
import com.jd.live.agent.plugin.application.springboot.v2.context.SpringAppContext;
import com.jd.live.agent.plugin.application.springboot.v2.listener.InnerListener;
import com.jd.live.agent.plugin.application.springboot.v2.util.AppLifecycle;
import com.jd.live.agent.plugin.application.springboot.v2.util.WebDoc;

public class ApplicationOnStartedInterceptor extends InterceptorAdaptor {

    private final AppListener listener;
    private final DocumentRegistry docRegistry;
    private final Application application;

    public ApplicationOnStartedInterceptor(AppListener listener, DocumentRegistry docRegistry, Application application) {
        this.listener = listener;
        this.docRegistry = docRegistry;
        this.application = application;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        SpringAppContext context = new SpringAppContext(ctx.getArgument(0));
        WebDoc webDoc = new WebDoc(application, context.getContext());
        docRegistry.register(webDoc.build());
        // fix for spring boot 2.1, it will trigger twice.
        AppLifecycle.started(() -> {
            InnerListener.foreach(l -> l.onStarted(context));
            listener.onStarted(context);
        });
    }

}
