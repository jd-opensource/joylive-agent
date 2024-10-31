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
package com.jd.live.agent.governance.invoke.filter.inbound;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.LoadLimiterConfig;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.limit.LoadLimitPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;
import com.sun.management.OperatingSystemMXBean;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A load limiting filter for inbound requests.
 * <p>
 * This filter checks the system load and rejects requests if the load exceeds a configured threshold.
 * It uses the JMX API to retrieve system metrics and supports application running in container environments.
 * </p>
 */
@Injectable
@Extension(value = "LoadLimitFilter", order = InboundFilter.ORDER_LOAD_LIMITER)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class LoadLimitFilter implements InboundFilter {
    private static final Logger logger = LoggerFactory.getLogger(LoadLimitFilter.class);
    public static final String LOAD_LIMITER_TIMER = "load-limiter";

    private static volatile SystemLoad load;

    private static volatile long processCpuTime = 0;

    private static volatile long processUpTime = 0;

    private static OperatingSystemMXBean osBean;

    private static RuntimeMXBean runtimeBean;

    private static final AtomicBoolean scheduled = new AtomicBoolean(false);

    @Inject
    private Timer timer;

    @Override
    public <T extends InboundRequest> CompletionStage<Object> filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        ServiceConfig serviceConfig = invocation.getContext().getGovernanceConfig().getServiceConfig();
        LoadLimiterConfig limiterConfig = serviceConfig == null ? null : serviceConfig.getLoadLimiter();
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        List<LoadLimitPolicy> loadLimitPolicies = servicePolicy == null ? null : servicePolicy.getLoadLimitPolicies();
        if (limiterConfig != null) {
            schedule();
            pass(invocation, new SystemLoad(limiterConfig.getCpuUsage(), limiterConfig.getLoadUsage()), load);
        } else if (null != loadLimitPolicies && !loadLimitPolicies.isEmpty()) {
            schedule();
            for (LoadLimitPolicy policy : loadLimitPolicies) {
                if (policy.match(invocation)) {
                    pass(invocation, new SystemLoad(policy.getCpuUsage(), policy.getLoadUsage()), load);
                }
            }
        }

        return chain.filter(invocation);
    }

    /**
     * Schedules the computation of system load metrics.
     */
    private void schedule() {
        if (scheduled.compareAndSet(false, true)) {
            addTask(1000);
        }
    }

    /**
     * Checks if the system load exceeds the configured threshold and rejects the request if necessary.
     *
     * @param invocation the inbound request to process
     * @param threshold  the configured threshold for CPU usage and load average
     * @param load       the current system load metrics
     */
    private void pass(InboundInvocation<?> invocation, SystemLoad threshold, SystemLoad load) {
        if (threshold == null || load == null) {
            return;
        }
        Integer cpuUsage = threshold.getCpuUsage();
        Integer loadUsage = threshold.getLoadUsage();
        if (cpuUsage != null && cpuUsage > 0 && cpuUsage <= load.getCpuUsage()
                || loadUsage != null && loadUsage > 0 && loadUsage <= load.getLoadUsage()) {
            invocation.reject(FaultType.LIMIT, "The request is rejected by load limiter. "
                    + "load(cpu:" + load.getCpuUsage() + ", load:" + load.getLoadUsage() + "), "
                    + "threshold(cpu:" + (cpuUsage == null ? "" : cpuUsage) + ", load:" + (loadUsage == null ? "" : loadUsage) + ")");
        }
    }

    /**
     * Schedules a task to compute the system load metrics after a specified delay.
     *
     * @param time the delay in milliseconds before the task is executed
     */
    private void addTask(long time) {
        timer.add(LOAD_LIMITER_TIMER, time, this::compute);
    }

    /**
     * Computes the system load metrics.
     */
    private void compute() {
        try {
            osBean = osBean == null ? ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class) : osBean;
            runtimeBean = runtimeBean == null ? ManagementFactory.getPlatformMXBean(RuntimeMXBean.class) : runtimeBean;

            // calculate process cpu usage to support application running in container environment
            double systemCpuUsage = osBean.getSystemCpuLoad();
            long newProcessCpuTime = osBean.getProcessCpuTime();
            long newProcessUpTime = runtimeBean.getUptime();
            int cpuCores = osBean.getAvailableProcessors();
            long processCpuTimeDiffInMs = TimeUnit.NANOSECONDS.toMillis(newProcessCpuTime - processCpuTime);
            long processUpTimeDiffInMs = newProcessUpTime - processUpTime;
            double processCpuUsage = (double) processCpuTimeDiffInMs / processUpTimeDiffInMs / cpuCores;
            processCpuTime = newProcessCpuTime;
            processUpTime = newProcessUpTime;

            double cpuUsage = Math.max(processCpuUsage, systemCpuUsage);
            double loadAverage = osBean.getSystemLoadAverage();

            load = new SystemLoad((int) cpuUsage, (int) loadAverage);
            addTask(1000);
        } catch (Throwable e) {
            logger.warn("Failed to get system metrics from JMX. caused by " + e.getMessage());
            addTask(10000);
        }
    }

    @Getter
    @AllArgsConstructor
    private static class SystemLoad {

        private Integer cpuUsage;

        private Integer loadUsage;

    }
}
