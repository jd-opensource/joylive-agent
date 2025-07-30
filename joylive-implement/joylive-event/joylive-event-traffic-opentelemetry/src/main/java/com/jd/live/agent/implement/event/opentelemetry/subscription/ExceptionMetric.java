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
import com.jd.live.agent.core.event.ExceptionEvent;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.event.Subscription;
import com.jd.live.agent.core.instance.Application;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

import java.util.List;

public class ExceptionMetric implements Subscription<ExceptionEvent> {

    private static final String KEY_APPLICATION = "application";
    private static final String KEY_CLASS_NAME = "class_name";
    private static final String KEY_METHOD_NAME = "method_name";
    private static final String KEY_LINE_NUMBER = "line_number";
    private static final String COUNTER_EXCEPTIONS_TOTAL = "joylive_exception_total";

    private static final String ERRORS = "errors";
    private static final AttributeKey<String> ATTRIBUTE_APPLICATION = AttributeKey.stringKey(KEY_APPLICATION);
    private static final AttributeKey<String> ATTRIBUTE_CLASS_NAME = AttributeKey.stringKey(KEY_CLASS_NAME);
    private static final AttributeKey<String> ATTRIBUTE_METHOD_NAME = AttributeKey.stringKey(KEY_METHOD_NAME);
    private static final AttributeKey<Long> ATTRIBUTE_LINE_NUMBER = AttributeKey.longKey(KEY_LINE_NUMBER);

    private final Application application;

    private final LongCounter exceptions;

    public ExceptionMetric(Application application, Meter meter) {
        this.application = application;
        this.exceptions = meter.counterBuilder(COUNTER_EXCEPTIONS_TOTAL).setUnit(ERRORS).build();
    }

    @Override
    public void handle(List<Event<ExceptionEvent>> events) {
        if (events != null) {
            for (Event<ExceptionEvent> event : events) {
                exceptions.add(1, attributes(event));
            }
        }
    }

    @Override
    public String getTopic() {
        return Publisher.EXCEPTION;
    }

    private Attributes attributes(Event<ExceptionEvent> event) {
        ExceptionEvent exceptionEvent = event.getData();
        AttributesBuilder builder = Attributes.builder()
                .put(ATTRIBUTE_APPLICATION, application.getName())
                .put(ATTRIBUTE_CLASS_NAME, exceptionEvent.getClassName())
                .put(ATTRIBUTE_METHOD_NAME, exceptionEvent.getMethodName())
                .put(ATTRIBUTE_LINE_NUMBER, exceptionEvent.getLineNumber());
        return builder.build();
    }
}
