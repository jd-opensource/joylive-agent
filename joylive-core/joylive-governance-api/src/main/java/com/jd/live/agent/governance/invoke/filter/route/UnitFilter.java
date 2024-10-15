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
package com.jd.live.agent.governance.invoke.filter.route;

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.instance.EndpointGroup;
import com.jd.live.agent.governance.instance.UnitGroup;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.UnitAction;
import com.jd.live.agent.governance.invoke.UnitAction.UnitActionType;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.governance.invoke.metadata.LiveMetadata;
import com.jd.live.agent.governance.invoke.metadata.ServiceMetadata;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.governance.policy.service.live.ServiceLivePolicy;
import com.jd.live.agent.governance.policy.service.live.UnitPolicy;
import com.jd.live.agent.governance.policy.variable.UnitFunction;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import static com.jd.live.agent.governance.invoke.Invocation.*;

/**
 * UnitFilter is a filter that routes requests to specific instances based on unit rules and policies.
 * It takes into account the live space, service policy, and unit rules to determine the best route for each request.
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "UnitFilter", order = RouteFilter.ORDER_LIVE_UNIT)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class UnitFilter implements RouteFilter {

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        RouteTarget target = invocation.getRouteTarget();
        LiveSpace liveSpace = invocation.getLiveMetadata().getLiveSpace();
        if (liveSpace != null) {
            target.filter(e -> e.isLiveSpace(liveSpace.getId()), 0, true);
        }
        target = route(invocation, target.getEndpoints());
        invocation.setRouteTarget(target);
        UnitAction action = target.getUnitAction();
        if (action.getType() == UnitActionType.FORWARD) {
            chain.filter(invocation);
        } else {
            invocation.reject(FaultType.UNIT, action.getMessage());
        }
    }

    /**
     * Determines the best route for the outbound request based on the live metadata,
     * service metadata, and unit policy.
     *
     * @param <T>        The type parameter of the outbound request.
     * @param invocation The outbound invocation containing the request and related metadata.
     * @param endpoints  A list of potential endpoints to which the request can be forwarded if routing conditions are satisfied.
     * @return The route target that specifies the instances to route the request to.
     */
    private <T extends OutboundRequest> RouteTarget route(OutboundInvocation<T> invocation,
                                                          List<? extends Endpoint> endpoints) {
        LiveMetadata liveMetadata = invocation.getLiveMetadata();
        ServiceMetadata serviceMetadata = invocation.getServiceMetadata();
        ServiceLivePolicy livePolicy = serviceMetadata.getServiceLivePolicy();
        UnitPolicy unitPolicy = serviceMetadata.getUnitPolicy();
        switch (unitPolicy) {
            case NONE:
                return routeNone(invocation, endpoints, liveMetadata);
            case CENTER:
                // Guiding to center unit
                return routeCenter(invocation, endpoints, liveMetadata);
            case UNIT:
                // Guiding to standard unit
                return routeUnit(invocation, endpoints, liveMetadata, getUnitRoute(invocation));
            case PREFER_LOCAL_UNIT:
            default:
                // Priority principle for current unit
                return routeLocal(invocation, endpoints, liveMetadata, livePolicy, getUnitRoute(invocation));
        }
    }

    /**
     * Filter available unit instances based on the provided live metadata.
     *
     * @param invocation   The outbound invocation to be routed.
     * @param endpoints    A list of potential endpoints to which the request can be forwarded if routing conditions are satisfied.
     * @param liveMetadata The live metadata associated with the request.
     * @return The route target indicating the action to be taken (forward, reject, etc.).
     */
    private RouteTarget routeNone(OutboundInvocation<?> invocation,
                                  List<? extends Endpoint> endpoints,
                                  LiveMetadata liveMetadata) {
        UnitRule rule = liveMetadata.getUnitRule();
        List<UnitRoute> routes = rule == null ? null : rule.getUnitRoutes();
        if (routes != null && !routes.isEmpty()) {
            Set<String> availableUnits = new HashSet<>(routes.size());
            for (UnitRoute route : routes) {
                if (invocation.isAccessible(route.getUnit())) {
                    availableUnits.add(route.getCode());
                }
            }
            if (availableUnits.isEmpty()) {
                return RouteTarget.reject(invocation.getError(REJECT_UNIT_NOT_ACCESSIBLE));
            } else if (routes.size() == 1) {
                return RouteTarget.forward(endpoints, routes.get(0));
            } else if (routes.size() != availableUnits.size()) {
                RouteTarget.filter(endpoints, e -> availableUnits.contains(e.getUnit()));
                if (availableUnits.size() == 1) {
                    return RouteTarget.forward(endpoints, rule.getUnitRoute(availableUnits.iterator().next()));
                }
            }
        }
        return RouteTarget.forward(endpoints);
    }

    /**
     * Routes an outbound invocation to a local unit based on the provided live metadata, live policy, and target route.
     *
     * @param invocation   The outbound invocation to be routed.
     * @param endpoints    A list of potential endpoints to which the request can be forwarded if routing conditions are satisfied.
     * @param liveMetadata The live metadata associated with the request.
     * @param livePolicy   The service live policy used to determine routing behavior.
     * @param targetRoute  The target unit route determined by the routing logic.
     * @return The route target indicating the action to be taken (forward, reject, etc.).
     */
    private RouteTarget routeLocal(OutboundInvocation<?> invocation,
                                   List<? extends Endpoint> endpoints,
                                   LiveMetadata liveMetadata,
                                   ServiceLivePolicy livePolicy,
                                   UnitRoute targetRoute) {
        EndpointGroup instanceGroup = new EndpointGroup(endpoints);
        Election election = getPreferUnits(liveMetadata, targetRoute, new CandidateBuilder(invocation, instanceGroup));
        List<Candidate> candidates = election.getCandidates();
        if (election.isEmpty()) {
            return RouteTarget.reject(invocation.getError(REJECT_UNIT_NOT_ACCESSIBLE));
        } else if (election.size() == 1) {
            // One target may be inaccessible or empty
            Candidate target = candidates.get(0);
            Unit unit = target.getUnit();
            if (!target.isAccessible()) {
                return RouteTarget.reject(unit, invocation.getError(REJECT_UNIT_NOT_ACCESSIBLE, unit.getCode()));
            } else if (target.getRoute() == null) {
                return RouteTarget.reject(unit, invocation.getError(REJECT_NO_UNIT_ROUTE, unit.getCode()));
            }
            return RouteTarget.forward(instanceGroup, target.getUnit(), target.getRoute());
        }
        // All targets are accessible and have instances
        int remaining = election.getInstances();
        Candidate target = candidates.get(0);
        Unit unit = target.getUnit();
        remaining -= target.getInstances();
        ServiceConfig serviceConfig = invocation.getServiceMetadata().getServiceConfig();
        Integer threshold = livePolicy.getUnitThreshold(unit.getCode());
        threshold = threshold == null ? serviceConfig.getUnitFailoverThreshold(unit.getCode()) : threshold;
        threshold = threshold == null ? 0 : threshold;
        int shortage = threshold - target.getInstances();
        if (shortage > 0) {
            // failover other unit
            ThreadLocalRandom localRandom = ThreadLocalRandom.current();
            if (localRandom.nextInt(threshold) < shortage) {
                int random = localRandom.nextInt(remaining);
                int weight = 0;
                for (int j = 1; j < candidates.size(); j++) {
                    Candidate failoverTarget = candidates.get(j);
                    weight += failoverTarget.getInstances();
                    if (weight > random) {
                        return RouteTarget.forward(instanceGroup, failoverTarget.getUnit(), failoverTarget.getRoute());
                    }
                }
            }
        }
        return RouteTarget.forward(instanceGroup, unit, target.getRoute());
    }

    /**
     * Retrieves the unit route based on the live metadata and the context of the outbound invocation.
     *
     * @param invocation The outbound invocation containing the request and related metadata.
     * @return The unit route determined by the unit rule and the provided variable function.
     */
    private UnitRoute getUnitRoute(OutboundInvocation<?> invocation) {
        LiveMetadata liveMetadata = invocation.getLiveMetadata();
        UnitRule rule = liveMetadata.getUnitRule();
        if (rule == null) {
            return null;
        }
        String variable = liveMetadata.getVariable();
        UnitFunction func = invocation.getContext().getUnitFunction(rule.getVariableFunction());
        return rule.getUnitRoute(variable, func);
    }

    /**
     * Routes the outbound request to the specified unit if it is accessible.
     *
     * @param invocation   The outbound invocation containing the request and related metadata.
     * @param endpoints    A list of potential endpoints to which the request can be forwarded if routing conditions are satisfied.
     * @param liveMetadata The live metadata associated with the request.
     * @param targetRoute  The target unit route determined by the routing logic.
     * @return The route target indicating the instances to forward the request to, or a rejection if the unit is not accessible.
     */
    private RouteTarget routeUnit(OutboundInvocation<?> invocation,
                                  List<? extends Endpoint> endpoints,
                                  LiveMetadata liveMetadata,
                                  UnitRoute targetRoute) {
        if (targetRoute == null) {
            String variable = liveMetadata.getVariable();
            return RouteTarget.reject(invocation.getError(variable == null || variable.isEmpty() ? REJECT_NO_VARIABLE : REJECT_NO_UNIT_ROUTE));
        }
        Unit unit = targetRoute.getUnit();
        if (!invocation.isAccessible(unit)) {
            return RouteTarget.reject(unit, invocation.getError(REJECT_UNIT_NOT_ACCESSIBLE, unit.getCode()));
        }
        return RouteTarget.forward(endpoints, targetRoute);
    }

    /**
     * Routes the outbound request to the center unit if it is accessible and a valid unit route is found.
     *
     * @param invocation   The outbound invocation containing the request and related metadata.
     * @param endpoints    A list of potential endpoints to which the request can be forwarded if routing conditions are satisfied.
     * @param liveMetadata The live metadata associated with the request.
     * @return The route target indicating the instances to forward the request to, or a rejection if the center unit is not accessible or no unit route is found.
     */
    private RouteTarget routeCenter(OutboundInvocation<?> invocation,
                                    List<? extends Endpoint> endpoints,
                                    LiveMetadata liveMetadata) {
        Unit unit = liveMetadata.getCenterUnit();
        UnitRule rule = liveMetadata.getUnitRule();
        UnitRoute unitRoute = unit == null || rule == null ? null : rule.getUnitRoute(unit.getCode());
        if (unit == null) {
            return RouteTarget.reject(invocation.getError(REJECT_NO_CENTER));
        } else if (!invocation.isAccessible(unit)) {
            return RouteTarget.reject(unit, invocation.getError(REJECT_UNIT_NOT_ACCESSIBLE, unit.getCode()));
        } else if (unitRoute == null) {
            return RouteTarget.reject(unit, invocation.getError(REJECT_NO_UNIT_ROUTE, unit.getCode()));
        }
        return RouteTarget.forward(endpoints, unitRoute);
    }

    /**
     * Determines the preferred units for the election process when no center unit is specified.
     *
     * @param liveMetadata The live metadata associated with the request.
     * @param targetRoute  The target unit route determined by the routing logic.
     * @param builder      The candidate builder used to build election candidates.
     * @param units        The list of available units to consider for the election.
     * @return An election object containing the preferred units for the election.
     */
    private Election getAvailableUnits(LiveMetadata liveMetadata,
                                       UnitRoute targetRoute,
                                       CandidateBuilder builder,
                                       List<Unit> units) {
        Election result = new Election();
        for (Unit unit : units) {
            result.add(builder.build(unit), Candidate::isAvailable);
        }
        return result;
    }

    /**
     * Determines the preferred units for the election process based on the live metadata and target route.
     *
     * @param liveMetadata The live metadata associated with the request.
     * @param targetRoute  The target unit route determined by the routing logic.
     * @param builder      The candidate builder used to build election candidates.
     * @return An election object containing the preferred units for the election.
     */
    private Election getPreferUnits(LiveMetadata liveMetadata,
                                    UnitRoute targetRoute,
                                    CandidateBuilder builder) {
        LiveSpace liveSpace = liveMetadata.getLiveSpace();
        List<Unit> units = liveSpace == null ? null : liveSpace.getUnits();
        if (units == null || units.isEmpty()) {
            return new Election();
        } else if (units.size() == 1) {
            return new Election(builder.build(units.get(0)));
        } else if (liveMetadata.getCenterUnit() != null) {
            return getPreferUnitsWithCenter(liveMetadata, targetRoute, builder);
        } else {
            return getPreferUnitsWithoutCenter(liveMetadata, targetRoute, builder, units);
        }
    }

    /**
     * Determines the preferred units for the election process when a center unit is specified.
     *
     * @param liveMetadata The live metadata associated with the request.
     * @param targetRoute  The target unit route determined by the routing logic.
     * @param builder      The candidate builder used to build election candidates.
     * @return An election object containing the preferred units for the election, including the center unit.
     */
    private Election getPreferUnitsWithCenter(LiveMetadata liveMetadata, UnitRoute targetRoute, CandidateBuilder builder) {
        Election result = new Election();
        Unit unit = liveMetadata.getCurrentUnit() != null ? liveMetadata.getCurrentUnit() : null;
        unit = unit == null && targetRoute != null ? targetRoute.getUnit() : unit;
        Candidate local = builder.build(unit);
        Candidate center = builder.build(liveMetadata.getCenterUnit());
        result.add(local, Candidate::isAvailable);
        result.add(center, Candidate::isAvailable);
        if (result.isEmpty()) {
            result.add(local != null ? local : center, null);
        }
        return result;
    }

    /**
     * Determines the preferred units for the election process when no center unit is specified.
     *
     * @param liveMetadata The live metadata associated with the request.
     * @param targetRoute  The target unit route determined by the routing logic.
     * @param builder      The candidate builder used to build election candidates.
     * @param units        The list of available units to consider for the election.
     * @return An election object containing the preferred units for the election.
     */
    private Election getPreferUnitsWithoutCenter(LiveMetadata liveMetadata,
                                                 UnitRoute targetRoute,
                                                 CandidateBuilder builder,
                                                 List<Unit> units) {
        Election result = new Election();
        Unit localUnit = liveMetadata.getCurrentUnit() != null ? liveMetadata.getCurrentUnit() : null;
        localUnit = localUnit == null && targetRoute != null ? targetRoute.getUnit() : localUnit;
        Candidate localCandidate = builder.build(localUnit);
        result.add(localCandidate, Candidate::isAvailable);
        int random = ThreadLocalRandom.current().nextInt(units.size());
        int i = random;
        Unit unit;
        while (i < units.size()) {
            unit = units.get(i++);
            result.add(builder.build(unit == localUnit ? null : unit), Candidate::isAvailable);
        }
        i = 0;
        while (i < random) {
            unit = units.get(i++);
            result.add(builder.build(unit == localUnit ? null : unit), Candidate::isAvailable);
        }
        if (result.isEmpty()) {
            result.add(localCandidate != null ? localCandidate : builder.build(units.get(0)), null);
        }
        return result;
    }

    /**
     * Represents a potential target for routing an outbound invocation.
     * It contains information about the unit, the route, the number of instances, and its accessibility.
     */
    @Getter
    private static class Candidate {

        /**
         * The unit that is a candidate for routing.
         */
        private final Unit unit;

        /**
         * The route to be used if this candidate is selected for routing.
         */
        private final UnitRoute route;

        /**
         * The number of instances available for this candidate.
         */
        private final int instances;

        /**
         * Indicates whether the candidate is accessible for routing.
         */
        private final boolean accessible;

        /**
         * Constructs a new Candidate with the provided unit, route, number of instances, and accessibility status.
         *
         * @param unit       The unit to be considered as a candidate.
         * @param route      The route associated with the unit.
         * @param instances  The number of instances available for the unit.
         * @param accessible True if the unit is accessible for routing, false otherwise.
         */
        Candidate(Unit unit, UnitRoute route, int instances, boolean accessible) {
            this.unit = unit;
            this.route = route;
            this.instances = instances;
            this.accessible = accessible;
        }

        /**
         * Checks if the candidate is available for routing, which means it is accessible, has instances, and has a route.
         *
         * @return True if the candidate is available for routing, false otherwise.
         */
        public boolean isAvailable() {
            return accessible && instances > 0 && route != null;
        }
    }

    /**
     * A utility class to build instances of the {@link Candidate} class based on the provided
     * {@link OutboundInvocation} and {@link EndpointGroup}.
     */
    private static class CandidateBuilder {

        private final OutboundInvocation<?> invocation;

        private final EndpointGroup instanceGroup;

        /**
         * Constructs a new CandidateBuilder with the specified invocation and endpoint group.
         *
         * @param invocation    The outbound invocation for which candidates are being built.
         * @param instanceGroup The endpoint group containing the units and instances.
         */
        CandidateBuilder(OutboundInvocation<?> invocation, EndpointGroup instanceGroup) {
            this.invocation = invocation;
            this.instanceGroup = instanceGroup;
        }

        /**
         * Builds a new Candidate from the provided Unit.
         *
         * @param unit The unit for which to create a Candidate.
         * @return A new Candidate object or null if the unit is null.
         */
        public Candidate build(Unit unit) {
            if (unit == null) {
                return null;
            }
            boolean accessible = invocation.isAccessible(unit);
            UnitRule unitRule = invocation.getLiveMetadata().getUnitRule();
            UnitRoute unitRoute = unitRule == null ? null : unitRule.getUnitRoute(unit.getCode());
            UnitGroup group = instanceGroup.getUnitGroup(unit.getCode());
            int instances = group == null ? 0 : group.size();
            return new Candidate(unit, unitRoute, instances, accessible);
        }

        /**
         * Builds a new Candidate from the provided UnitRoute.
         *
         * @param unitRoute The unit route for which to create a Candidate.
         * @return A new Candidate object or null if the unit route is null.
         */
        public Candidate build(UnitRoute unitRoute) {
            if (unitRoute == null) {
                return null;
            }
            Unit unit = unitRoute.getUnit();
            boolean accessible = invocation.isAccessible(unit);
            UnitGroup group = instanceGroup.getUnitGroup(unit.getCode());
            int instances = group == null ? 0 : group.size();
            return new Candidate(unit, unitRoute, instances, accessible);
        }
    }


    /**
     * Represents an election process for selecting candidates for routing an outbound invocation.
     * It holds a list of candidates and tracks the total number of instances across all candidates.
     */
    @Getter
    private static class Election {

        /**
         * List of candidates considered for routing.
         */
        private final List<Candidate> candidates;

        /**
         * Total number of instances across all candidates.
         */
        private int instances;

        /**
         * Default constructor.
         */
        Election() {
            this.candidates = new ArrayList<>();
            this.instances = 0;
        }

        /**
         * Constructor to initialize the election with a set of candidates.
         *
         * @param candidates The initial candidates for the election.
         */
        Election(Candidate... candidates) {
            this();
            if (candidates != null) {
                for (Candidate candidate : candidates) {
                    add(candidate, null);
                }
            }
        }

        /**
         * Adds a candidate to the election if it meets the provided predicate condition.
         *
         * @param candidate The candidate to add.
         * @param predicate An optional predicate to test the candidate before adding.
         */
        public void add(Candidate candidate, Predicate<Candidate> predicate) {
            if (candidate != null && (predicate == null || predicate.test(candidate))) {
                instances += candidate.getInstances();
                candidates.add(candidate);
            }
        }

        /**
         * Returns the number of candidates in the election.
         *
         * @return The size of the candidates list.
         */
        public int size() {
            return candidates.size();
        }

        /**
         * Checks if the election has no candidates.
         *
         * @return True if the candidates list is empty, false otherwise.
         */
        public boolean isEmpty() {
            return candidates.isEmpty();
        }
    }

}
