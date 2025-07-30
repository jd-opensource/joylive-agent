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
package com.jd.live.agent.implement.event.opentelemetry.subscription;

import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.event.Subscription;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.doc.ServiceAnchor;
import com.jd.live.agent.governance.event.DocEvent;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

import java.util.List;

public class DocMetric implements Subscription<DocEvent> {

    private static final String KEY_APPLICATION = "application";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_SERVICE = "service";
    private static final String KEY_GROUP = "group";
    private static final String KEY_PATH = "path";
    private static final String KEY_METHOD = "method";

    private static final String COUNTER_ANCHOR_TOTAL = "joylive_anchor_total";

    private static final String ANCHORS = "anchors";
    private static final AttributeKey<String> ATTRIBUTE_APPLICATION = AttributeKey.stringKey(KEY_APPLICATION);
    private static final AttributeKey<Long> ATTRIBUTE_TIMESTAMP = AttributeKey.longKey(KEY_TIMESTAMP);
    private static final AttributeKey<String> ATTRIBUTE_SERVICE = AttributeKey.stringKey(KEY_SERVICE);
    private static final AttributeKey<String> ATTRIBUTE_GROUP = AttributeKey.stringKey(KEY_GROUP);
    private static final AttributeKey<String> ATTRIBUTE_PATH = AttributeKey.stringKey(KEY_PATH);
    private static final AttributeKey<String> ATTRIBUTE_METHOD = AttributeKey.stringKey(KEY_METHOD);

    private final Application application;

    private final LongCounter docs;

    public DocMetric(Application application, Meter meter) {
        this.application = application;
        this.docs = meter.counterBuilder(COUNTER_ANCHOR_TOTAL).setUnit(ANCHORS).build();
    }

    @Override
    public void handle(List<Event<DocEvent>> events) {
        if (events != null) {
            for (Event<DocEvent> event : events) {
                DocEvent docEvent = event.getData();
                for (ServiceAnchor anchor : docEvent.getAnchors()) {
                    docs.add(1, attributes(anchor));
                }
            }
        }
    }

    @Override
    public String getTopic() {
        return Publisher.DOC;
    }

    private Attributes attributes(ServiceAnchor anchor) {
        AttributesBuilder builder = Attributes.builder()
                .put(ATTRIBUTE_APPLICATION, application.getName())
                .put(ATTRIBUTE_TIMESTAMP, application.getTimestamp())
                .put(ATTRIBUTE_SERVICE, anchor.getService())
                .put(ATTRIBUTE_GROUP, anchor.getGroup())
                .put(ATTRIBUTE_PATH, anchor.getPath())
                .put(ATTRIBUTE_METHOD, anchor.getMethod());
        return builder.build();
    }
}
