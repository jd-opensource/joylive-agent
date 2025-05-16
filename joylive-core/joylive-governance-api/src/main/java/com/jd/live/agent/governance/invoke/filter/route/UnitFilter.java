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

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.annotation.ConditionalOnLiveEnabled;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.instance.EndpointGroup;
import com.jd.live.agent.governance.instance.UnitGroup;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.UnitAction;
import com.jd.live.agent.governance.invoke.UnitAction.UnitActionType;
import com.jd.live.agent.governance.invoke.filter.LiveFilter;
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

import java.util.*;
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
@ConditionalOnLiveEnabled
public class UnitFilter implements RouteFilter, LiveFilter {

    @Override
    public <T extends OutboundRequest> void filter(final OutboundInvocation<T> invocation, final RouteFilterChain chain) {
        RouteTarget target = route(invocation);
        invocation.setRouteTarget(target);
        UnitAction action = target.getUnitAction();
        if (action.getType() == UnitActionType.FORWARD) {
            chain.filter(invocation);
        } else {
            throw FaultType.UNIT.reject(action.getMessage());
        }
    }

    /**
     * Determines the best route for the outbound request based on the live metadata,
     * service metadata, and unit policy.
     *
     * @param <T>        The type parameter of the outbound request.
     * @param invocation The outbound invocation containing the request and related metadata.
     * @return The route target that specifies the instances to route the request to.
     */
    private <T extends OutboundRequest> RouteTarget route(final OutboundInvocation<T> invocation) {
        UnitRule rule = invocation.getLiveMetadata().getRule();
        UnitPolicy policy = rule == null ? UnitPolicy.NONE : invocation.getServiceMetadata().getUnitPolicy();
        switch (policy) {
            case NONE:
                // None live
                return routeNone(invocation);
            case CENTER:
                // Guiding to center unit
                return routeCenter(invocation);
            case UNIT:
                // Guiding to standard unit
                return routeUnit(invocation);
            case PREFER_LOCAL_UNIT:
            default:
                // Priority principle for current unit
                return routeLocal(invocation);
        }
    }

    /**
     * Filter available unit instances based on the provided live metadata.
     *
     * @param invocation   The outbound invocation to be routed.
     * @return The route target indicating the action to be taken (forward, reject, etc.).
     */
    private RouteTarget routeNone(final OutboundInvocation<?> invocation) {
        LiveMetadata metadata = invocation.getLiveMetadata();
        String targetSpaceId = metadata.getTargetSpaceId();
        UnitRule rule = metadata.getRule();
        List<UnitRoute> routes = rule == null ? null : rule.getUnitRoutes();
        Set<String> units = getAvailableUnits(invocation, routes);
        return RouteTarget.forward(invocation.getRouteTarget().filtrate(e -> e.isUnit(targetSpaceId, units) || e.isLiveless()));
    }

    /**
     * Gets the available units for the given outbound invocation based on the provided routes.
     *
     * @param invocation the outbound invocation
     * @param routes     the list of unit routes
     * @return a set of available unit codes, or null if no routes are provided or all units are inaccessible
     */
    private Set<String> getAvailableUnits(final OutboundInvocation<?> invocation, final List<UnitRoute> routes) {
        if (routes != null && !routes.isEmpty()) {
            Set<String> availableUnits = new HashSet<>(routes.size());
            for (UnitRoute route : routes) {
                if (invocation.isAccessible(route.getUnit())) {
                    availableUnits.add(route.getCode());
                }
            }
            return availableUnits;
        }
        return null;
    }

    /**
     * Routes the outbound request to the center unit if it is accessible and a valid unit route is found.
     *
     * @param invocation The outbound invocation containing the request and related metadata.
     * @return The route target indicating the instances to forward the request to, or a rejection if the center unit is not accessible or no unit route is found.
     */
    private RouteTarget routeCenter(final OutboundInvocation<?> invocation) {
        LiveMetadata metadata = invocation.getLiveMetadata();
        UnitRule rule = metadata.getRule();
        Unit unit = metadata.getTargetCenter();
        UnitRoute route = null;
        if (unit != null) {
            route = rule == null ? null : rule.getUnitRoute(unit.getCode());
        } else if (rule != null && rule.size() == 1) {
            // one unit
            route = rule.getUnitRoutes().get(0);
            unit = route.getUnit();
        }
        if (unit == null) {
            return RouteTarget.reject(invocation.getError(REJECT_NO_CENTER));
        } else if (!invocation.isAccessible(unit)) {
            return RouteTarget.reject(unit, invocation.getError(REJECT_UNIT_NOT_ACCESSIBLE, unit.getCode()));
        } else if (route == null) {
            return RouteTarget.reject(unit, invocation.getError(REJECT_NO_UNIT_ROUTE, unit.getCode()));
        }
        String targetSpaceId = metadata.getTargetSpaceId();
        RouteTarget target = invocation.getRouteTarget();
        return RouteTarget.forward(target.filtrate(e -> e.isLiveSpace(targetSpaceId)), route);
    }

    /**
     * Routes the outbound request to the specified unit if it is accessible.
     *
     * @param invocation The outbound invocation containing the request and related metadata.
     * @return The route target indicating the instances to forward the request to, or a rejection if the unit is not accessible.
     */
    private RouteTarget routeUnit(final OutboundInvocation<?> invocation) {
        UnitRoute route = getUnitRoute(invocation);
        if (route == null) {
            String variable = invocation.getLiveMetadata().getVariable();
            return RouteTarget.reject(invocation.getError(variable == null || variable.isEmpty() ? REJECT_NO_VARIABLE : REJECT_NO_UNIT_ROUTE));
        }
        Unit unit = route.getUnit();
        if (!invocation.isAccessible(unit)) {
            return RouteTarget.reject(unit, invocation.getError(REJECT_UNIT_NOT_ACCESSIBLE, unit.getCode()));
        }
        String targetSpaceId = invocation.getLiveMetadata().getTargetSpaceId();
        RouteTarget target = invocation.getRouteTarget();
        return RouteTarget.forward(target.filtrate(e -> e.isLiveSpace(targetSpaceId)), route);
    }

    /**
     * Routes an outbound invocation to a local unit based on the provided live metadata, live policy, and target route.
     *
     * @param invocation   The outbound invocation to be routed.
     * @return The route target indicating the action to be taken (forward, reject, etc.).
     */
    private RouteTarget routeLocal(final OutboundInvocation<?> invocation) {
        String targetSpaceId = invocation.getLiveMetadata().getTargetSpaceId();
        EndpointGroup group = new EndpointGroup(invocation.getRouteTarget().filtrate(e -> e.isLiveSpace(targetSpaceId)));
        Election election = getPreferUnits(invocation, group);
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
            return RouteTarget.forward(group, target.getUnit(), target.getRoute());
        }
        // All targets are accessible and have instances
        int remaining = election.getInstances();
        Candidate target = candidates.get(0);
        Unit unit = target.getUnit();
        remaining -= target.getInstances();
        ServiceMetadata serviceMetadata = invocation.getServiceMetadata();
        ServiceConfig serviceConfig = serviceMetadata.getServiceConfig();
        ServiceLivePolicy livePolicy = serviceMetadata.getServiceLivePolicy();
        Integer threshold = livePolicy.getUnitThreshold(unit.getCode());
        threshold = threshold == null ? serviceConfig.getUnitFailoverThreshold(unit.getCode()) : threshold;
        threshold = threshold == null ? 0 : threshold;
        int shortage = threshold - target.getInstances();
        if (shortage > 0) {
            // failover other unit
            Random random = invocation.getRandom();
            if (random.nextInt(threshold) < shortage) {
                int randomWeight = random.nextInt(remaining);
                int weight = 0;
                for (int j = 1; j < candidates.size(); j++) {
                    Candidate failoverTarget = candidates.get(j);
                    weight += failoverTarget.getInstances();
                    if (weight > randomWeight) {
                        return RouteTarget.forward(group, failoverTarget.getUnit(), failoverTarget.getRoute());
                    }
                }
            }
        }
        return RouteTarget.forward(group, unit, target.getRoute());
    }

    /**
     * Retrieves the unit route based on the live metadata and the context of the outbound invocation.
     *
     * @param invocation The outbound invocation containing the request and related metadata.
     * @return The unit route determined by the unit rule and the provided variable function.
     */
    private UnitRoute getUnitRoute(final OutboundInvocation<?> invocation) {
        LiveMetadata liveMetadata = invocation.getLiveMetadata();
        UnitRule rule = liveMetadata.getRule();
        if (rule == null) {
            return null;
        } else if (rule.size() == 1) {
            return rule.getUnitRoutes().get(0);
        }
        String variable = liveMetadata.getVariable();
        UnitFunction func = invocation.getContext().getUnitFunction(rule.getVariableFunction());
        return rule.getUnitRoute(variable, func);
    }

    /**
     * Determines the preferred units for the election process based on the live metadata and target route.
     *
     * @param invocation   The outbound invocation to be routed.
     * @param group      The endpoint group used to build election candidates.
     * @return An election object containing the preferred units for the election.
     */
    private Election getPreferUnits(final OutboundInvocation<?> invocation, final EndpointGroup group) {
        UnitRoute route = getUnitRoute(invocation);
        LiveMetadata metadata = invocation.getLiveMetadata();
        LiveSpace liveSpace = metadata.getTargetSpace();
        UnitRule rule = metadata.getRule();
        List<Unit> units = rule != null ? rule.getAllUnits() : (liveSpace == null ? null : liveSpace.getUnits());
        CandidateBuilder builder = new CandidateBuilder(invocation, group);
        if (units == null || units.isEmpty()) {
            return new Election();
        } else if (units.size() == 1) {
            return new Election(builder.build(units.get(0)));
        } else if (metadata.getTargetCenter() != null) {
            return getPreferUnitsWithCenter(metadata, route, builder);
        } else {
            return getPreferUnitsWithoutCenter(invocation.getRequest(), metadata, route, builder, units);
        }
    }

    /**
     * Determines the preferred units for the election process when a center unit is specified.
     *
     * @param metadata The live metadata associated with the request.
     * @param route  The target unit route determined by the routing logic.
     * @param builder      The candidate builder used to build election candidates.
     * @return An election object containing the preferred units for the election, including the center unit.
     */
    private Election getPreferUnitsWithCenter(final LiveMetadata metadata, final UnitRoute route, final CandidateBuilder builder) {
        Election result = new Election();
        Unit localUnit = metadata.getTargetLocalUnit();
        localUnit = localUnit == null && route != null ? route.getUnit() : localUnit;
        Candidate local = builder.build(localUnit);
        result.add(local, Candidate::isAvailable);
        Candidate center = null;
        Unit centerUnit = metadata.getTargetCenter();
        if (centerUnit != localUnit) {
            center = builder.build(centerUnit);
            result.add(center, Candidate::isAvailable);
        }
        if (result.isEmpty()) {
            result.add(local != null ? local : center, null);
        }
        return result;
    }

    /**
     * Determines the preferred units for the election process when no center unit is specified.
     * This method selects a local unit as the primary candidate and randomly selects additional
     * units from the available list to build election candidates. If the local unit is not available,
     * it falls back to the first unit in the list. The method ensures that at least one candidate
     * is added to the election result.
     *
     * @param <T>       The type of the outbound request, extending {@link OutboundRequest}.
     * @param request   The outbound request containing context information for the election.
     * @param metadata  The live metadata associated with the request, providing access to the target local unit.
     * @param route     The target unit route determined by the routing logic, used to resolve the local unit if not provided by metadata.
     * @param builder   The candidate builder used to construct election candidates from units.
     * @param units     The list of available units to consider for the election.
     * @return An {@link Election} object containing the preferred units for the election, never {@code null}.
     */
    private <T extends OutboundRequest> Election getPreferUnitsWithoutCenter(final T request,
                                                                             final LiveMetadata metadata,
                                                                             final UnitRoute route,
                                                                             final CandidateBuilder builder,
                                                                             final List<Unit> units) {
        Election result = new Election();
        Unit localUnit = metadata.getTargetLocalUnit();
        localUnit = localUnit == null && route != null ? route.getUnit() : localUnit;
        Candidate localCandidate = builder.build(localUnit);
        result.add(localCandidate, Candidate::isAvailable);
        int random = request.getRandom().nextInt(units.size());
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
            UnitRule unitRule = invocation.getLiveMetadata().getRule();
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
