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
package com.jd.live.agent.governance.policy;

import com.jd.live.agent.bootstrap.classloader.ResourcerType;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.InjectSourceSupplier;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.InjectLoader;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.AppService;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.LaneConfig;
import com.jd.live.agent.governance.config.LiveConfig;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.invoke.matcher.TagMatcher;
import com.jd.live.agent.governance.invoke.retry.RetrierFactory;
import com.jd.live.agent.governance.policy.variable.UnitFunction;
import com.jd.live.agent.governance.policy.variable.VariableFunction;
import com.jd.live.agent.governance.policy.variable.VariableParser;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PolicyManager
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Injectable
@Extension(value = "PolicyManager", order = InjectSourceSupplier.ORDER_POLICY_MANAGER)
public class PolicyManager implements PolicySupervisor, InjectSourceSupplier, ExtensionInitializer, InvocationContext {
    private static final Logger logger = LoggerFactory.getLogger(PolicyManager.class);
    private final AtomicReference<GovernancePolicy> policy = new AtomicReference<>();
    private final Map<String, PolicySubscriber> subscribers = new ConcurrentHashMap<>();

    @Getter
    @Inject(Publisher.POLICY_SUBSCRIBER)
    private Publisher<PolicySubscriber> publisher;

    @Getter
    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Getter
    @Config(GovernanceConfig.CONFIG_AGENT_GOVERNANCE)
    private GovernanceConfig governanceConfig;

    @Getter
    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private Map<String, UnitFunction> unitFunctions;

    @Getter
    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private Map<String, VariableFunction> variableFunctions;

    @Getter
    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private Map<String, VariableParser<?, ?>> variableParsers;

    @Getter
    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private Map<String, RetrierFactory> retrierFactories;

    @Getter
    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private RetrierFactory retrierFactory;

    @Getter
    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private Map<String, TagMatcher> tagMatchers;

    @Getter
    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private Map<String, LoadBalancer> loadBalancers;

    @Getter
    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private LoadBalancer loadBalancer;

    @Getter
    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private Map<String, ClusterInvoker> clusterInvokers;

    @Getter
    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private ClusterInvoker clusterInvoker;

    @Getter
    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private List<InboundFilter> inboundFilters;

    @Getter
    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private List<OutboundFilter> outboundFilters;

    @Getter
    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private List<RouteFilter> routeFilters;

    private final AtomicInteger counter = new AtomicInteger(0);

    private final CompletableFuture<Void> future = new CompletableFuture<>();

    @Override
    public PolicySupplier getPolicySupplier() {
        return this;
    }

    @Override
    public GovernancePolicy getPolicy() {
        return policy.get();
    }

    @Override
    public UnitFunction getUnitFunction(String name) {
        return name == null || unitFunctions == null ? null : unitFunctions.get(name);
    }

    @Override
    public VariableFunction getVariableFunction(String name) {
        return name == null || variableFunctions == null ? null : variableFunctions.get(name);
    }

    @Override
    public VariableParser<?, ?> getVariableParser(String name) {
        return name == null || variableParsers == null ? null : variableParsers.get(name);
    }

    @Override
    public RetrierFactory getOrDefaultRetrierFactory(String name) {
        RetrierFactory result = retrierFactories == null || name == null ? null : retrierFactories.get(name);
        return result == null ? retrierFactory : result;
    }

    @Override
    public LoadBalancer getOrDefaultLoadBalancer(String name) {
        LoadBalancer result = loadBalancers == null || name == null ? null : loadBalancers.get(name);
        return result == null ? loadBalancer : result;
    }

    @Override
    public ClusterInvoker getOrDefaultClusterInvoker(String name) {
        ClusterInvoker result = clusterInvokers == null || name == null ? null : clusterInvokers.get(name);
        return result == null ? clusterInvoker : result;
    }

    @Override
    public boolean update(GovernancePolicy expect, GovernancePolicy update) {
        // live policy is updated by a few services.
        return policy.compareAndSet(expect, update);
    }

    @Override
    public void apply(InjectSource source) {
        if (source != null) {
            source.add(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR, this);
            source.add(PolicySupervisor.COMPONENT_POLICY_SUPPLIER, this);
            source.add(InvocationContext.COMPONENT_INVOCATION_CONTEXT, this);
            source.add(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG, governanceConfig);
            source.add(LiveConfig.COMPONENT_LIVE_CONFIG, governanceConfig == null ? null : governanceConfig.getLiveConfig());
            source.add(ServiceConfig.COMPONENT_SERVICE_CONFIG, governanceConfig == null ? null : governanceConfig.getServiceConfig());
            source.add(LaneConfig.COMPONENT_LANE_CONFIG, governanceConfig == null ? null : governanceConfig.getLaneConfig());
        }
    }

    @Override
    public CompletableFuture<Void> subscribe(String name, PolicyType type) {
        if (name == null || name.isEmpty() || type == null) {
            return CompletableFuture.completedFuture(null);
        }
        String namespace = application.getService() == null ? null : application.getService().getNamespace();
        PolicySubscriber subscriber = new PolicySubscriber(name, namespace, type);
        subscribe(subscriber);
        return subscriber.getFuture();
    }

    @Override
    public List<PolicySubscriber> getSubscribers() {
        return Collections.unmodifiableList(new ArrayList<>(subscribers.values()));
    }

    @Override
    public void initialize() {
        ServiceConfig serviceConfig = governanceConfig == null ? null : governanceConfig.getServiceConfig();
        Set<String> warmups = serviceConfig == null ? null : serviceConfig.getWarmups();
        warmups = warmups == null ? new HashSet<>() : warmups;
        AppService service = application.getService();
        String name = service == null || service.getName() == null ? null : service.getName();
        String namespace = service == null ? null : service.getNamespace();
        if (name != null) {
            warmups.add(name);
        }
        if (!warmups.isEmpty()) {
            counter.set(warmups.size());
            warmups.forEach(o -> subscribe(new PolicySubscriber(o, namespace, PolicyType.SERVICE_POLICY)));
        } else {
            future.complete(null);
        }
    }

    protected void subscribe(PolicySubscriber subscriber) {
        PolicySubscriber exist = subscribers.putIfAbsent(subscriber.getName(), subscriber);
        if (exist == null) {
            subscriber.trigger((v, t) -> {
                if (t == null) {
                    logger.info("success syncing service policy " + subscriber.getName());
                } else {
                    logger.error("failed to sync service policy " + subscriber.getName());
                }
                if (!future.isDone() && counter.decrementAndGet() == 0) {
                    future.complete(null);
                }
            });
            publisher.offer(new Event<>(subscriber));
        } else {
            exist.trigger(subscriber.getFuture());
        }
    }
}
