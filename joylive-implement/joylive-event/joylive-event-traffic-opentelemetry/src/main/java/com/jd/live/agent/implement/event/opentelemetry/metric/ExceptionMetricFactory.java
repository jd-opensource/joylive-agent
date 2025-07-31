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
package com.jd.live.agent.implement.event.opentelemetry.metric;

import com.jd.live.agent.bootstrap.bytekit.advice.AdviceHandler;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.core.event.ExceptionEvent;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.event.Subscription;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.config.ExporterConfig;
import com.jd.live.agent.governance.config.ExporterConfig.ExceptionConfig;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.implement.event.opentelemetry.MetricFactory;
import io.opentelemetry.api.metrics.Meter;

import static com.jd.live.agent.core.util.ExceptionUtils.getRootCause;

@Injectable
@Extension("ExceptionMetric")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_EXPORTER_EXCEPTION_ENABLED, matchIfMissing = true)
public class ExceptionMetricFactory implements MetricFactory<ExceptionEvent> {

    private static final String LIVE_AGENT_PREFIX = "com.jd.live.agent.";

    @Inject(Publisher.EXCEPTION)
    private Publisher<ExceptionEvent> publisher;

    @Override
    public Subscription<ExceptionEvent> create(ExporterConfig config, Application application, Meter meter) {
        ExceptionConfig exceptionConfig = config.getExceptionConfig();
        AdviceHandler.onException = e -> onException(e, exceptionConfig);
        return new ExceptionMetric(application, meter);
    }

    @Override
    public void close() {
        AdviceHandler.onException = null;
    }

    /**
     * Processes an exception by analyzing its stack trace and publishing an event
     * if the root cause matches specific criteria.
     * <p>
     * Skips {@link LiveException} instances and only publishes events for:
     * <ul>
     *   <li>Classes not matching the agent's stack trace filter</li>
     *   <li>Classes starting with the agent prefix</li>
     * </ul>
     *
     * @param throwable the exception to process (non-null)
     */
    private void onException(Throwable throwable, ExceptionConfig config) {
        // Get root cause, excluding known LiveException instances
        Throwable rootCause = getRootCause(throwable, e -> !(e instanceof LiveException));
        if (rootCause == null) {
            return;
        }

        // Extract stack trace elements for analysis
        StackTraceElement[] traces = rootCause.getStackTrace();

        // Get maximum traversal depth from configuration (default: 20)
        int maxDepth = config.getMaxDepth();
        int max = maxDepth > 0 ? Math.min(maxDepth, traces.length) : traces.length;

        String className;
        StackTraceElement trace;
        // Traverse stack trace elements
        for (int i = 0; i < max; i++) {
            trace = traces[i];
            className = trace.getClassName();
            if (className.startsWith(LIVE_AGENT_PREFIX)) {
                // Priority handling - Report Agent-related exceptions immediately
                publisher.offer(new ExceptionEvent(className, trace.getMethodName(), trace.getLineNumber()));
                return;
            } else if (!config.withStackTrace(className)) {
                // Filter out business exceptions - ignore if class not in whitelist
                return;
            }
        }
    }
}
