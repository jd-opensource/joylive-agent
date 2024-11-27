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

import com.jd.live.agent.core.config.ConfigSupervisor;
import com.jd.live.agent.core.config.ConfigWatcher;
import com.jd.live.agent.core.event.AgentEvent;
import com.jd.live.agent.core.event.AgentEvent.EventType;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.InjectSourceSupplier;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.AppService;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.service.ConfigService;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.RegistryConfig;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.event.TrafficEvent;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.invoke.counter.CounterManager;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.invoke.matcher.TagMatcher;
import com.jd.live.agent.governance.policy.listener.LaneSpaceListener;
import com.jd.live.agent.governance.policy.listener.LiveSpaceListener;
import com.jd.live.agent.governance.policy.listener.ServiceListener;
import com.jd.live.agent.governance.policy.variable.UnitFunction;
import com.jd.live.agent.governance.policy.variable.VariableFunction;
import com.jd.live.agent.governance.policy.variable.VariableParser;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.jd.live.agent.core.config.ConfigWatcher.*;

/**
 * PolicyManager
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Injectable
@Extension(value = "PolicyManager", order = InjectSourceSupplier.ORDER_POLICY_MANAGER)
public class PolicyManager implements PolicySupervisor, InjectSourceSupplier, ExtensionInitializer, InvocationContext {

    private final AtomicReference<GovernancePolicy> policy = new AtomicReference<>();

    private final Map<String, PolicySubscriber> subscribers = new ConcurrentHashMap<>();

    @Getter
    @Inject(Publisher.POLICY_SUBSCRIBER)
    private Publisher<PolicySubscriber> policyPublisher;

    @Getter
    @Inject(Publisher.SYSTEM)
    private Publisher<AgentEvent> systemPublisher;

    @Getter
    @Inject(Publisher.TRAFFIC)
    private Publisher<TrafficEvent> trafficPublisher;

    @Getter
    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(ConfigSupervisor.COMPONENT_CONFIG_SUPERVISOR)
    private ConfigSupervisor configSupervisor;

    @Inject(ObjectParser.JSON)
    private ObjectParser objectParser;

    @Getter
    @Config(GovernanceConfig.CONFIG_LIVE_ENABLED)
    private boolean liveEnabled = true;

    @Getter
    @Config(GovernanceConfig.CONFIG_LANE_ENABLED)
    private boolean laneEnabled = true;

    @Getter
    @Config(GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED)
    private boolean flowControlEnabled;

    @Getter
    @Config(GovernanceConfig.CONFIG_AGENT_GOVERNANCE)
    private GovernanceConfig governanceConfig;

    @Getter
    @Inject
    private Map<String, UnitFunction> unitFunctions;

    @Getter
    @Inject
    private Map<String, VariableFunction> variableFunctions;

    @Getter
    @Inject
    private Map<String, VariableParser<?, ?>> variableParsers;

    @Getter
    @Inject
    private Map<String, TagMatcher> tagMatchers;

    @Getter
    @Inject
    private Map<String, LoadBalancer> loadBalancers;

    @Getter
    @Inject
    private LoadBalancer loadBalancer;

    @Getter
    @Inject
    private Map<String, ClusterInvoker> clusterInvokers;

    @Getter
    @Inject
    private ClusterInvoker clusterInvoker;

    @Getter
    @Inject
    private InboundFilter[] inboundFilters;

    @Getter
    @Inject
    private RouteFilter[] routeFilters;

    @Getter
    @Inject
    private OutboundFilter[] outboundFilters;

    @Getter
    @Inject
    private Timer timer;

    @Getter
    private CounterManager counterManager;

    private List<String> serviceSyncers;

    private final AtomicBoolean warmup = new AtomicBoolean(false);

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
        if (update != null) {
            update.locate(application.getLocation());
        }
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
            source.add(ServiceConfig.COMPONENT_SERVICE_CONFIG, governanceConfig == null ? null : governanceConfig.getServiceConfig());
            source.add(RegistryConfig.COMPONENT_REGISTRY_CONFIG, governanceConfig == null ? null : governanceConfig.getRegistryConfig());
        }
    }

    @Override
    public CompletableFuture<Void> subscribe(String namespace, String service) {
        if (service == null || service.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        namespace = namespace == null || namespace.isEmpty() ? application.getService().getNamespace() : namespace;
        PolicySubscriber subscriber = new PolicySubscriber(service, namespace, TYPE_SERVICE_SPACE, serviceSyncers);
        subscribe(subscriber);
        return subscriber.getFuture();
    }

    @Override
    public boolean isDone(String name) {
        PolicySubscriber subscriber = name == null ? null : subscribers.get(name);
        return subscriber != null && subscriber.isDone();
    }

    @Override
    public List<PolicySubscriber> getSubscribers() {
        return Collections.unmodifiableList(new ArrayList<>(subscribers.values()));
    }

    @Override
    public void waitReady() {
        if (!subscribers.isEmpty()) {
            List<CompletableFuture<Void>> futures = new ArrayList<>(subscribers.size());
            subscribers.forEach((k, v) -> futures.add(v.getFuture()));
            try {
                Futures.allOf(futures).get(governanceConfig.getInitializeTimeout(), TimeUnit.MILLISECONDS);
                systemPublisher.offer(AgentEvent.onServicePolicyReady("Application service policies are ready."));
            } catch (Throwable e) {
                Throwable cause = e instanceof ExecutionException ? e.getCause() : null;
                cause = cause != null ? cause : e;
                String error = cause instanceof TimeoutException ? "It's timeout to fetch governance policy." :
                        "Failed to fetch governance policy. caused by " + cause.getMessage();
                systemPublisher.offer(AgentEvent.onServicePolicyFailure(error, cause));
            }
        }
    }

    @Override
    public void initialize() {
        counterManager = new CounterManager(timer);
        systemPublisher.addHandler(events -> {
            for (Event<AgentEvent> event : events) {
                if (event.getData().getType() == EventType.AGENT_SERVICE_READY) {
                    // subscribe after all services are started.
                    serviceSyncers = getServiceSyncers();
                    warmup();
                }
            }
        });
        configSupervisor.addListener(TYPE_LIVE_SPACE, new LiveSpaceListener(this, objectParser));
        configSupervisor.addListener(TYPE_LANE_SPACE, new LaneSpaceListener(this, objectParser));
        configSupervisor.addListener(TYPE_SERVICE_SPACE, new ServiceListener(this, objectParser, policyPublisher));
    }

    /**
     * Computes a list of policy service names by inspecting the available services from the service supervisor.
     * Only services of type {@link ConfigService} with a policy type of {@link ConfigWatcher#TYPE_SERVICE_SPACE} are included.
     *
     * @return A list of policy service names that match the criteria.
     */
    private List<String> getServiceSyncers() {
        List<String> result = new ArrayList<>();
        List<ConfigWatcher> watchers = configSupervisor.getWatchers();
        if (watchers != null) {
            for (ConfigWatcher service : watchers) {
                if (service instanceof ConfigService) {
                    ConfigService configService = (ConfigService) service;
                    if (TYPE_SERVICE_SPACE.equals(configService.getType())) {
                        result.add(configService.getName());
                    }
                }
            }
        }
        return result;
    }

    /**
     * Initiates the warmup process.
     */
    private void warmup() {
        if (warmup.compareAndSet(false, true)) {
            ServiceConfig serviceConfig = governanceConfig == null ? null : governanceConfig.getServiceConfig();
            Set<String> warmups = serviceConfig == null ? null : serviceConfig.getWarmups();
            warmups = warmups == null ? new HashSet<>() : warmups;
            AppService service = application.getService();
            String namespace = service == null ? null : service.getNamespace();
            String name = service == null || service.getName() == null ? null : service.getName();
            if (name != null) {
                warmups.add(name);
            }
            if (!warmups.isEmpty()) {
                warmups.forEach(o -> subscribe(new PolicySubscriber(o, namespace, TYPE_SERVICE_SPACE, serviceSyncers)));
            }
        }
    }

    /**
     * Subscribes a {@link PolicySubscriber} to the policy publisher.
     *
     * @param subscriber The {@link PolicySubscriber} to be subscribed.
     */
    protected void subscribe(PolicySubscriber subscriber) {
        PolicySubscriber exist = subscribers.putIfAbsent(subscriber.getName(), subscriber);
        if (exist == null) {
            policyPublisher.offer(subscriber);
        } else {
            exist.trigger(subscriber.getFuture());
        }
    }
}
