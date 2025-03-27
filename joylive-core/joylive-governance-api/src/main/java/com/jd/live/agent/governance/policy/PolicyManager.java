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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
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
import com.jd.live.agent.core.service.ServiceSupervisor;
import com.jd.live.agent.core.service.ServiceSupervisorAware;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.*;
import com.jd.live.agent.governance.context.bag.AutoDetect;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.governance.context.bag.Propagation.AutoPropagation;
import com.jd.live.agent.governance.event.TrafficEvent;
import com.jd.live.agent.governance.event.TrafficEvent.ActionType;
import com.jd.live.agent.governance.instance.counter.CounterManager;
import com.jd.live.agent.governance.instance.counter.internal.InternalCounterManager;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.LiveFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.invoke.matcher.TagMatcher;
import com.jd.live.agent.governance.policy.variable.UnitFunction;
import com.jd.live.agent.governance.policy.variable.VariableFunction;
import com.jd.live.agent.governance.policy.variable.VariableParser;
import com.jd.live.agent.governance.service.PolicyService;
import com.jd.live.agent.governance.subscription.config.ConfigCenter;
import com.jd.live.agent.governance.subscription.policy.PolicyWatcher;
import com.jd.live.agent.governance.subscription.policy.PolicyWatcherManager;
import com.jd.live.agent.governance.subscription.policy.PolicyWatcherSupervisor;
import com.jd.live.agent.governance.subscription.policy.listener.LaneSpaceListener;
import com.jd.live.agent.governance.subscription.policy.listener.LiveSpaceListener;
import com.jd.live.agent.governance.subscription.policy.listener.ServiceListener;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.governance.policy.service.ServiceName.getUniqueName;
import static com.jd.live.agent.governance.subscription.policy.PolicyWatcher.*;

/**
 * PolicyManager
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Injectable
@Extension(value = "PolicyManager", order = InjectSourceSupplier.ORDER_POLICY_MANAGER)
public class PolicyManager implements PolicySupervisor, InjectSourceSupplier, ExtensionInitializer, InvocationContext, ServiceSupervisorAware {

    private static final Logger logger = LoggerFactory.getLogger(PolicyManager.class);

    private final AtomicReference<GovernancePolicy> policy = new AtomicReference<>();

    private final Map<String, PolicySubscription> subscriptions = new ConcurrentHashMap<>();

    @Getter
    @Inject(Publisher.POLICY_SUBSCRIBER)
    private Publisher<PolicySubscription> policyPublisher;

    @Getter
    @Inject(Publisher.SYSTEM)
    private Publisher<AgentEvent> systemPublisher;

    @Getter
    @Inject(Publisher.TRAFFIC)
    private Publisher<TrafficEvent> trafficPublisher;

    @Getter
    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(ObjectParser.JSON)
    private ObjectParser objectParser;

    @Getter
    @Config(GovernanceConfig.CONFIG_LIVE_ENABLED)
    private boolean liveEnabled;

    @Getter
    @Config(GovernanceConfig.CONFIG_LANE_ENABLED)
    private boolean laneEnabled;

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
    private RouteFilter[] liveFilters;

    @Getter
    @Inject
    private Timer timer;

    @Inject
    private Map<String, Propagation> propagations;

    @Inject
    private List<Propagation> propagationList;

    @Getter
    private Propagation propagation;

    @Getter
    private CounterManager counterManager;

    private List<String> serviceSyncers;

    private ConfigCenter configCenter;

    private final PolicyWatcherSupervisor policyWatcherSupervisor = new PolicyWatcherManager();

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
            update.locate(application);
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
            source.add(Propagation.COMPONENT_PROPAGATION, propagation);
            source.add(ConfigCenter.COMPONENT_CONFIG_CENTER, configCenter);
            if (governanceConfig != null) {
                source.add(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG, governanceConfig);
                source.add(ServiceConfig.COMPONENT_SERVICE_CONFIG, governanceConfig.getServiceConfig());
                source.add(RegistryConfig.COMPONENT_REGISTRY_CONFIG, governanceConfig.getRegistryConfig());
            }
        }
    }

    @Override
    public CompletableFuture<Void> subscribe(String namespace, String service) {
        if (service == null || service.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        namespace = namespace == null || namespace.isEmpty() ? application.getService().getNamespace() : namespace;
        return subscribe(new PolicySubscription(service, namespace, TYPE_SERVICE_POLICY, serviceSyncers));
    }

    @Override
    public boolean isReady(String namespace, String service) {
        if (service == null || service.isEmpty()) {
            return false;
        }
        namespace = namespace == null || namespace.isEmpty() ? application.getService().getNamespace() : namespace;
        String fullName = getUniqueName(namespace, service);
        PolicySubscription subscription = subscriptions.get(fullName);
        return subscription != null && subscription.isReady();
    }

    @Override
    public List<PolicySubscription> getSubscriptions() {
        return Collections.unmodifiableList(new ArrayList<>(subscriptions.values()));
    }

    @Override
    public void waitReady() {
        if (!subscriptions.isEmpty()) {
            List<PolicySubscription> subscriptions = new ArrayList<>(this.subscriptions.values());
            List<CompletableFuture<Void>> futures = new ArrayList<>(subscriptions.size());
            subscriptions.forEach(subscription -> futures.add(subscription.watch()));
            try {
                Futures.allOf(futures).get(governanceConfig.getInitializeTimeout(), TimeUnit.MILLISECONDS);
                systemPublisher.offer(AgentEvent.onServicePolicyReady("Application service policies are ready."));
            } catch (Throwable e) {
                Throwable cause = e instanceof ExecutionException ? e.getCause() : null;
                cause = cause != null ? cause : e;
                String error;
                if (cause instanceof TimeoutException) {
                    for (int i = 0; i < futures.size(); i++) {
                        if (!futures.get(i).isDone()) {
                            PolicySubscription subscription = subscriptions.get(i);
                            logger.error("It's timeout to fetch {} {} governance policy by {}.",
                                    subscription.getFullName(), subscription.getType(), subscription.getUnCompletedSyncers());
                        }
                    }
                    error = "It's timeout to fetch governance policy.";
                } else {
                    error = "Failed to fetch governance policy. caused by " + cause.getMessage();
                }
                systemPublisher.offer(AgentEvent.onServicePolicyFailure(error, cause));
            }
        }
    }

    @Override
    public void publish(TrafficEvent event) {
        if (event != null) {
            MonitorConfig monitorConfig = governanceConfig.getServiceConfig().getMonitor();
            if (event.getActionType() == ActionType.FORWARD && monitorConfig.isForward()
                    || event.getActionType() == ActionType.REJECT && monitorConfig.isReject()) {
                trafficPublisher.offer(event);
            }
        }
    }

    @Override
    public void initialize() {
        List<RouteFilter> forwards = toList(routeFilters, filter -> filter instanceof LiveFilter ? filter : null);
        liveFilters = forwards == null ? null : forwards.toArray(new RouteFilter[0]);

        governanceConfig = governanceConfig == null ? new GovernanceConfig() : governanceConfig;
        governanceConfig.initialize(application);
        counterManager = new InternalCounterManager(timer);
        propagation = buildPropagation();
        systemPublisher.addHandler(events -> {
            for (Event<AgentEvent> event : events) {
                if (event.getData().getType() == EventType.AGENT_SERVICE_READY) {
                    // subscribe after all services are started.
                    serviceSyncers = getServiceSyncers();
                    warmup();
                }
            }
        });

        policyWatcherSupervisor.addListener(TYPE_LIVE_POLICY, new LiveSpaceListener(this, objectParser));
        policyWatcherSupervisor.addListener(TYPE_LANE_POLICY, new LaneSpaceListener(this, objectParser));
        policyWatcherSupervisor.addListener(TYPE_SERVICE_POLICY, new ServiceListener(this, objectParser, policyPublisher));

        if (!(flowControlEnabled || laneEnabled || liveEnabled)) {
            logger.warn("No governance is enabled, please check the configuration.");
        }

    }

    @Override
    public void setup(ServiceSupervisor serviceSupervisor) {
        serviceSupervisor.service(service -> {
            if (service instanceof PolicyService) {
                policyWatcherSupervisor.addWatcher((PolicyService) service);
            } else if (service instanceof ConfigCenter) {
                configCenter = (ConfigCenter) service;
            }
        });
    }

    /**
     * Builds a {@link Propagation} instance based on the configuration settings.
     * It retrieves the {@link TransmitConfig} from the governance configuration and selects a {@link Propagation}
     * instance from the available propagations. If auto-detection is enabled, it constructs an {@link AutoPropagation}
     * instance.
     *
     * @return A {@link Propagation} instance configured based on the settings.
     */
    private Propagation buildPropagation() {
        TransmitConfig config = governanceConfig.getTransmitConfig();
        Propagation defaultPropagation = propagationList.get(0);
        Propagation propagation = propagations.getOrDefault(config.getType(), defaultPropagation);
        AutoDetect autoDetect = config.getAutoDetect();
        if (autoDetect == null || autoDetect == AutoDetect.NONE) {
            return propagation;
        } else if (autoDetect == AutoDetect.ALL) {
            return new AutoPropagation(propagationList, propagation, autoDetect);
        } else if (propagation == defaultPropagation) {
            return new AutoPropagation(propagationList, propagation, autoDetect);
        } else {
            // Priority processing of the current configuration
            List<Propagation> ordered = new ArrayList<>(propagationList.size());
            ordered.add(propagation);
            for (Propagation p : propagationList) {
                if (p != propagation) {
                    ordered.add(propagation);
                }
            }
            return new AutoPropagation(ordered, propagation, autoDetect);
        }
    }

    /**
     * Computes a list of policy service names by inspecting the available services from the service supervisor.
     * Only services of type {@link PolicyService} with a policy type of {@link PolicyWatcher#TYPE_SERVICE_POLICY} are included.
     *
     * @return A list of policy service names that match the criteria.
     */
    private List<String> getServiceSyncers() {
        List<String> result = new ArrayList<>();
        List<PolicyWatcher> watchers = policyWatcherSupervisor.getWatchers();
        if (watchers != null) {
            for (PolicyWatcher service : watchers) {
                if (service instanceof PolicyService) {
                    PolicyService configService = (PolicyService) service;
                    if (TYPE_SERVICE_POLICY.equals(configService.getType())) {
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
            ServiceConfig serviceConfig = governanceConfig.getServiceConfig();
            Set<String> warmups = serviceConfig == null ? null : serviceConfig.getWarmups();
            warmups = warmups == null ? new HashSet<>() : warmups;
            AppService service = application.getService();
            String namespace = service == null ? null : service.getNamespace();
            String name = service == null || service.getName() == null ? null : service.getName();
            if (name != null) {
                warmups.add(name);
            }
            if (!warmups.isEmpty()) {
                warmups.forEach(o -> subscribe(new PolicySubscription(o, namespace, TYPE_SERVICE_POLICY, serviceSyncers)));
            }
        }
    }

    /**
     * Subscribes a {@link PolicySubscription} to the policy publisher.
     *
     * @param subscriber The {@link PolicySubscription} to be subscribed.
     */
    protected CompletableFuture<Void> subscribe(PolicySubscription subscriber) {
        PolicySubscription exist = subscriptions.putIfAbsent(subscriber.getFullName(), subscriber);
        if (exist == null) {
            // notify syncer by event bus.
            policyPublisher.offer(subscriber);
            return subscriber.watch();
        } else {
            return exist.watch();
        }
    }
}
