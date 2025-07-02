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
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.annotation.ConditionalOnFlowControlEnabled;
import com.jd.live.agent.governance.config.LoadLimiterConfig;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.auth.Permission;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.limit.LoadLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.LoadMetric;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * A load limiting filter for inbound requests.
 * <p>
 * This filter checks the system load and rejects requests if the load exceeds a configured threshold.
 * It uses the JMX API to retrieve system metrics and supports application running in container environments.
 * </p>
 */
@Injectable
@Extension(value = "LoadLimitFilter", order = InboundFilter.ORDER_LOAD_LIMITER)
@ConditionalOnFlowControlEnabled
public class LoadLimitFilter implements InboundFilter {
    private static final Logger logger = LoggerFactory.getLogger(LoadLimitFilter.class);

    private static final String LOAD_LIMITER_TIMER = "load-limiter";

    private static volatile LoadMetric load;

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
        List<LoadLimitPolicy> policies = servicePolicy == null ? null : servicePolicy.getLoadLimitPolicies();

        boolean hasLimitConfig = limiterConfig != null && !limiterConfig.isEmpty();
        boolean hasLimitPolicy = policies != null && !policies.isEmpty();
        if (hasLimitConfig || hasLimitPolicy) {
            if (!scheduled.get()) {
                schedule(limiterConfig == null ? LoadLimiterConfig.DEFAULT_WINDOW : limiterConfig.getCpuWindow());
            }
        }
        Permission permission;
        if (hasLimitConfig) {
            permission = pass(load, limiterConfig::getRatio);
            if (!permission.isSuccess()) {
                return Futures.future(FaultType.LIMIT.reject(permission.getMessage()));
            }
        }
        if (hasLimitPolicy) {
            for (LoadLimitPolicy policy : policies) {
                if (!policy.isEmpty() && policy.match(invocation)) {
                    permission = pass(load, policy::getRatio);
                    if (!permission.isSuccess()) {
                        return Futures.future(FaultType.LIMIT.reject(permission.getMessage()));
                    }
                }
            }
        }

        return chain.filter(invocation);
    }

    /**
     * Determines if the request should be allowed based on current load metrics.
     * Uses a probabilistic rejection strategy when near threshold limits.
     *
     * @param metric current system load metrics (CPU and load average)
     * @param limitFunc function that calculates the rejection ratio (0-100)
     * @return Permission.success() if allowed, Permission.failure() with details if rejected
     */
    private Permission pass(LoadMetric metric, Function<LoadMetric, Integer> limitFunc) {
        int ratio = limitFunc.apply(metric);
        if (ratio <= 0) {
            return Permission.success();
        }
        if (ratio >= 100 || ThreadLocalRandom.current().nextInt(0, 100) < ratio) {
            return Permission.failure("The request is rejected by load limiter"
                    + ", cpu:" + metric.getCpuUsage()
                    + ", load:" + metric.getLoadUsage());
        }
        return Permission.success();
    }

    /**
     * Schedules the computation of system load metrics.
     */
    private void schedule(long interval) {
        if (scheduled.compareAndSet(false, true)) {
            addTask(interval);
        }
    }

    /**
     * Schedules a task to compute the system load metrics after a specified delay.
     *
     * @param time the delay in milliseconds before the task is executed
     */
    private void addTask(long time) {
        timer.delay(LOAD_LIMITER_TIMER, time, () -> compute(time));
    }

    /**
     * Computes the system load metrics.
     */
    private void compute(long interval) {
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

            double cpuUsage = Math.max(processCpuUsage, systemCpuUsage) * 100;
            double loadAverage = osBean.getSystemLoadAverage();

            load = new LoadMetric((int) cpuUsage, (int) loadAverage);
            addTask(interval);
        } catch (Throwable e) {
            logger.warn("Failed to get system metrics from JMX. caused by " + e.getMessage());
            addTask(interval * 5);
        }
    }
}
