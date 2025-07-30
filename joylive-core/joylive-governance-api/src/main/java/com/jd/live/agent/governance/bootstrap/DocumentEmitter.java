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
package com.jd.live.agent.governance.bootstrap;

import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.bootstrap.AppListener.AppListenerAdapter;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.doc.Document;
import com.jd.live.agent.governance.doc.DocumentRegistry;
import com.jd.live.agent.governance.doc.ServiceAnchor;
import com.jd.live.agent.governance.event.DocEvent;

import java.util.ArrayList;
import java.util.List;

@Injectable
@Extension(value = "DocEmitter", order = AppListener.ORDER_DOC)
public class DocumentEmitter extends AppListenerAdapter {

    @Inject(DocumentRegistry.COMPONENT_SERVICE_DOC_REGISTRY)
    private DocumentRegistry registry;

    @Inject(Publisher.DOC)
    private Publisher<DocEvent> publisher;

    @Override
    public void onReady(AppContext context) {
        List<ServiceAnchor> anchors = new ArrayList<>();
        for (Document doc : registry.getDocuments()) {
            anchors.addAll(doc.getAnchors());
        }
        if (!anchors.isEmpty()) {
            publisher.offer(new DocEvent(anchors));
        }
    }
}
