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
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.instance.CellGroup;
import com.jd.live.agent.governance.instance.Endpoint;
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
import com.jd.live.agent.governance.policy.service.live.CellPolicy;
import com.jd.live.agent.governance.policy.service.live.ServiceLivePolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * CellRouteFilter filter cell instances
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Extension(value = "CellRouteFilter", order = RouteFilter.ORDER_LIVE_CELL)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class CellRouteFilter implements RouteFilter.LiveRouteFilter {

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        RouteTarget target = invocation.getRouteTarget();
        UnitAction action = target.getUnitAction();
        if (action.getType() == UnitActionType.FORWARD && forward(invocation, target)) {
            chain.filter(invocation);
        } else {
            invocation.reject(FaultType.CELL, action.getMessage());
        }
    }

    /**
     * Forwards an OutboundInvocation to a specific RouteTarget based on various service policies and configurations.
     *
     * @param invocation The OutboundInvocation to be forwarded.
     * @param target     The RouteTarget where the invocation should be directed.
     * @return true if the routing decision was successful and endpoints were set, false otherwise.
     */
    private boolean forward(OutboundInvocation<?> invocation, RouteTarget target) {
        ServiceMetadata serviceMetadata = invocation.getServiceMetadata();
        ServiceConfig serviceConfig = serviceMetadata.getServiceConfig();
        ServiceLivePolicy livePolicy = serviceMetadata.getServiceLivePolicy();
        CellPolicy cellPolicy = livePolicy == null ? null : livePolicy.getCellPolicy();

        boolean localFirst = cellPolicy == CellPolicy.PREFER_LOCAL_CELL || cellPolicy == null && serviceConfig.isLocalFirst();
        Function<String, Integer> thresholdFunc = !localFirst ? null : (
                cellPolicy == CellPolicy.PREFER_LOCAL_CELL
                        ? livePolicy::getCellThreshold
                        : serviceConfig::getCellFailoverThreshold);
        if (target.getUnit() == null) {
            // unit policy is none.
            return routeAny(invocation, target, localFirst, thresholdFunc);
        }
        return routeUnit(invocation, target, localFirst, thresholdFunc);
    }

    /**
     * Routes an outbound invocation to a suitable unit.
     *
     * @param invocation    The outbound invocation containing metadata for the election.
     * @param target        The {@code RouteTarget} containing the endpoints to be filtered.
     * @param localFirst    Indicates whether the local unit should be preferred for routing.
     * @param thresholdFunc A function that returns a threshold value for the current cell. If the number of local endpoints exceeds this threshold,
     *                      those endpoints are preferred.
     * @return true if the routing decision was successful and endpoints were set, false otherwise.
     */
    private boolean routeUnit(OutboundInvocation<?> invocation,
                              RouteTarget target,
                              boolean localFirst,
                              Function<String, Integer> thresholdFunc) {
        Unit unit = target.getUnit();
        UnitGroup unitGroup = target.getUnitGroup();
        // previous filters may filtrate the endpoints
        unitGroup = unitGroup != null && unitGroup.getEndpoints() == target.getEndpoints() && unitGroup.size() == target.size()
                ? unitGroup
                : new UnitGroup(unit.getCode(), target.getEndpoints());
        Election election = sponsor(invocation, target.getUnitRoute(), localFirst, unitGroup, thresholdFunc);
        if (!election.isOver()) {
            randomWeight(election);
        }
        if (election.isMutable()) {
            // Attempt to failover if not in the whitelist or prefix
            failover(election);
        }
        Candidate winner = election.getWinner();
        CellRoute cellRoute = winner == null ? null : winner.getCellRoute();
        target.setCellRoute(cellRoute);
        Cell cell = target.getCell();
        if (cell != null) {
            CellGroup cellGroup = unitGroup.getCell(cell.getCode());
            target.setEndpoints(cellGroup == null ? new ArrayList<>() : cellGroup.getEndpoints());
            return true;
        }
        return false;
    }

    /**
     * Routes an outbound invocation to suitable endpoints
     *
     * @param invocation The outbound invocation containing metadata for the election.
     * @param target The {@code RouteTarget} containing the endpoints to be filtered.
     * @param localFirst Indicates whether the local cell or unit should be preferred for routing.
     * @param thresholdFunc A function that returns a threshold value for the current cell. If the number of local endpoints exceeds this threshold,
     * those endpoints are preferred.
     * @return true if the routing decision was successful, false otherwise.
     */
    private boolean routeAny(OutboundInvocation<?> invocation,
                             RouteTarget target,
                             boolean localFirst,
                             Function<String, Integer> thresholdFunc) {
        LiveMetadata liveMetadata = invocation.getLiveMetadata();
        Unit preferUnit = localFirst ? liveMetadata.getCurrentUnit() : null;
        Cell preferCell = localFirst ? liveMetadata.getCurrentCell() : null;
        LiveSpace liveSpace = liveMetadata.getLiveSpace();
        if (liveSpace != null) {
            Set<String> unavailableCells = getUnavailableCells(invocation);
            if (!unavailableCells.isEmpty()) {
                target.filter(endpoint -> !unavailableCells.contains(endpoint.getCell()));
            }
            if (preferCell != null || preferUnit != null) {
                List<Endpoint> preferCellEndpoints = new ArrayList<>();
                List<Endpoint> preferUnitEndpoints = new ArrayList<>();
                for (Endpoint endpoint : target.getEndpoints()) {
                    if (preferUnit != null && endpoint.isUnit(preferUnit.getCode())) {
                        preferUnitEndpoints.add(endpoint);
                    }
                    if (preferCell != null && endpoint.isCell(preferCell.getCode())) {
                        preferCellEndpoints.add(endpoint);
                    }
                }
                Integer threshold = preferCell == null ? null : thresholdFunc.apply(preferCell.getCode());
                if (!preferCellEndpoints.isEmpty() && (threshold == null || threshold <= preferCellEndpoints.size())) {
                    target.setEndpoints(preferCellEndpoints);
                } else if (!preferUnitEndpoints.isEmpty() && (threshold == null || threshold <= preferUnitEndpoints.size())) {
                    target.setEndpoints(preferUnitEndpoints);
                }
            }
        }
        return true;
    }

    /**
     * Returns a set of cell codes that are not available.
     *
     * @param invocation   The outbound invocation containing metadata for the election.
     * @return A set of cell codes that are not accessible for the given invocation and live metadata.
     */
    private Set<String> getUnavailableCells(OutboundInvocation<?> invocation) {
        Set<String> unavailableCells = new HashSet<>();
        LiveMetadata liveMetadata = invocation.getLiveMetadata();
        LiveSpace liveSpace = liveMetadata.getLiveSpace();
        List<Unit> units = liveSpace == null ? null : liveSpace.getSpec().getUnits();

        if (units != null) {
            UnitRule rule = liveMetadata.getUnitRule();
            for (Unit unit : units) {
                UnitRoute unitRoute = rule == null ? null : rule.getUnitRoute(unit.getCode());
                boolean unitAccessible = invocation.isAccessible(unit);
                if (unit.getCells() != null) {
                    for (Cell cell : unit.getCells()) {
                        CellRoute cellRoute = unitRoute == null ? null : unitRoute.getCellRoute(cell.getCode());
                        if (!unitAccessible
                                || !invocation.isAccessible(cell)
                                || cellRoute != null && (cellRoute.isEmpty() || !invocation.isAccessible(cellRoute.getAccessMode()))) {
                            unavailableCells.add(cell.getCode());
                        }
                    }
                }
            }
        }
        return unavailableCells;
    }

    /**
     * Sponsors an election process by creating an Election object based on the provided parameters.
     * It iterates through the cells in the unit route, checks their accessibility, and constructs
     * candidates for the election based on the cell's weight, priority, and instance count.
     * The method also considers local preference and failover thresholds.
     *
     * @param invocation            The outbound invocation containing metadata for the election.
     * @param unitRoute             The route containing cells to be considered as candidates.
     * @param localFirst            A boolean indicating whether local cells should be preferred.
     * @param unitGroup             The group from which to retrieve the size of instances for each cell.
     * @param failoverThresholdFunc A function that provides the failover threshold for each cell.
     * @return An Election object representing the sponsored election.
     */
    private Election sponsor(OutboundInvocation<?> invocation,
                             UnitRoute unitRoute,
                             boolean localFirst, UnitGroup unitGroup,
                             Function<String, Integer> failoverThresholdFunc) {
        // Extract necessary information from the invocation metadata.
        LiveMetadata liveMetadata = invocation.getLiveMetadata();
        String variable = liveMetadata.getVariable();
        Cell localCell = liveMetadata.getCurrentCell();

        // Initialize variables to keep track of the total weight, instance count, max priority, and preferred candidate.
        int weights = 0;
        int instances = 0;
        int maxPriority = -1;
        Candidate prefer = null;
        List<Candidate> candidates = new ArrayList<>(unitRoute.size());

        // Iterate through the cells in the unit route.
        for (CellRoute cellRoute : unitRoute.getCells()) {
            Cell cell = cellRoute.getCell();
            // Check if the cell is accessible and has a non-empty route.
            if (invocation.isAccessible(cell) && !cellRoute.isEmpty() && invocation.isAccessible(cellRoute.getAccessMode())) {
                // Get the instance count for the cell from the unit group, if available.
                Integer instance = unitGroup == null ? null : unitGroup.getSize(cellRoute.getCode());
                instance = instance == null ? 0 : instance;

                // Degrade the weight to 0 if the cell has instances.
                instances += instance;
                int weight = instance == 0 ? 0 : cellRoute.getWeight();
                weights += weight;

                // Determine the priority of the cell based on the variable and local preference.
                int priority = cellRoute.getPriority(variable, localFirst ? localCell : null);

                // Get the failover threshold for the cell using the provided function.
                Integer threshold = !localFirst ? null : failoverThresholdFunc.apply(cell.getCode());
                threshold = threshold == null ? 0 : threshold;

                // Create a Candidate object and add it to the list of candidates.
                Candidate candidate = new Candidate(cellRoute, instance, weight, priority, threshold);
                candidates.add(candidate);

                // Update the preferred candidate if the current cell has a higher priority.
                if (priority > maxPriority && priority >= CellRoute.PRIORITY_LOCAL) {
                    maxPriority = priority;
                    prefer = candidate;
                }
            }
        }

        // Create and return an Election object with the collected information.
        return new Election(candidates, weights, instances, prefer, failoverThresholdFunc);
    }

    /**
     * Selects a winner randomly from the list of candidates based on their weights.
     * The probability of a candidate being chosen is proportional to its weight.
     * If there are no candidates, no action is taken. If there is only one candidate,
     * that candidate is set as the winner. Otherwise, a random number is generated within
     * the range of the total weights, and the winner is selected based on the cumulative
     * weight range that includes the random number.
     *
     * @param election The election object containing the current state of the election.
     */
    private void randomWeight(Election election) {
        List<Candidate> candidates = election.getCandidates();
        switch (candidates.size()) {
            case 0:
                // If there are no candidates, do nothing.
                break;
            case 1:
                // If there is only one candidate, set it as the winner.
                election.setWinner(candidates.get(0));
                break;
            default:
                if (election.getWeights() == 0) {
                    election.setWinner(candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())));
                } else {
                    // Generate a random number within the range of the total weights.
                    int random = ThreadLocalRandom.current().nextInt(election.getWeights());
                    int range = 0;

                    // Iterate through the candidates and calculate the cumulative weight range.
                    for (Candidate candidate : candidates) {
                        // Add the candidate's weight to the range.
                        range += candidate.getWeight();
                        // If the random number is within the current range, set the candidate as the winner.
                        if (range > random) {
                            election.setWinner(candidate);
                            break;
                        }
                    }
                }
        }
    }

    /**
     * Handles failover logic in case the current winner of the election does not meet the required threshold.
     * If the current winner's instance count is below the threshold, this method searches for other candidates
     * that have instances above their own threshold and have more instances than the current winner.
     * If suitable candidates are found, a random failover may occur based on a calculated failover ratio.
     *
     * @param election The election object containing the current state of the election.
     */
    private void failover(Election election) {
        // If there's only one candidate or fewer, there's no failover needed.
        if (election.size() <= 1) {
            return;
        }

        // Retrieve the current winner's information.
        Candidate winner = election.getWinner();
        int instance = winner.getInstance();
        int threshold = winner.getThreshold();
        int shortage = threshold - instance;

        // If the current winner does not meet the threshold, proceed with failover logic.
        if (shortage > 0) {
            int instances = 0;
            List<Candidate> targets = new ArrayList<>(election.size() - 1);

            // Iterate through the candidates to find potential failover targets.
            for (Candidate candidate : election.getCandidates()) {
                if (candidate != winner) {
                    instance = candidate.getInstance();
                    // Check if the candidate has instances above their threshold and more than the current winner.
                    if (instance >= candidate.getThreshold() && instance > winner.getInstance()) {
                        targets.add(candidate);
                        instances += instance;
                    }
                }
            }

            // If there are potential targets, calculate the failover ratio.
            if (instances > 0) {
                ThreadLocalRandom localRandom = ThreadLocalRandom.current();
                // If the random number is within the shortage range, perform a failover.
                if (localRandom.nextInt(threshold) < shortage) {
                    int random = localRandom.nextInt(instances);
                    int weight = 0;

                    // Find the candidate to failover to based on the random number.
                    for (Candidate candidate : targets) {
                        weight += candidate.getInstance();
                        if (weight > random) {
                            election.setWinner(candidate);
                            break;
                        }
                    }
                }
            }
        }
    }


    /**
     * Represents an election process for selecting a winner from a list of candidates.
     * This class holds information about the candidates, their total weights, total instances,
     * the current winner, and a function for determining failover thresholds.
     */
    @Getter
    private static class Election {

        /**
         * The list of candidates participating in the election.
         */
        private final List<Candidate> candidates;

        /**
         * The total weight of all candidates.
         */
        private final int weights;

        /**
         * The total number of instances across all candidates.
         */
        private final int instances;

        /**
         * A function that takes a string argument and returns an integer failover threshold.
         */
        private final Function<String, Integer> failoverThresholdFunc;

        /**
         * The current winner of the election.
         * This field is mutable and can be set using the provided setter method.
         */
        @Setter
        private Candidate winner;

        /**
         * Constructs a new Election with the provided candidates and election parameters.
         *
         * @param candidates            The list of candidates participating in the election.
         * @param weights               The total weight of all candidates.
         * @param instances             The total number of instances across all candidates.
         * @param winner                The current winner of the election.
         * @param failoverThresholdFunc A function for determining failover thresholds.
         */
        Election(List<Candidate> candidates, int weights, int instances, Candidate winner,
                 Function<String, Integer> failoverThresholdFunc) {
            this.candidates = candidates;
            this.weights = weights;
            this.instances = instances;
            this.winner = winner;
            this.failoverThresholdFunc = failoverThresholdFunc;
        }

        /**
         * Returns the size of the candidate list.
         *
         * @return The number of candidates participating in the election.
         */
        public int size() {
            return candidates == null ? 0 : candidates.size();
        }

        /**
         * Checks if the election has concluded with a winner.
         *
         * @return True if a winner has been determined, false otherwise.
         */
        public boolean isOver() {
            return winner != null;
        }

        /**
         * Checks if the election is mutable, which is determined by the priority of the current winner.
         *
         * @return True if the election can be modified, false otherwise.
         */
        public boolean isMutable() {
            return winner != null && winner.isMutable();
        }
    }

    /**
     * Represents a potential routing target with additional routing decision-making attributes.
     * This class holds information about the cell route, the number of instances, and routing
     * attributes such as weight, priority, and threshold.
     */
    @Getter
    private static class Candidate {

        /**
         * The cell route associated with this candidate.
         */
        private final CellRoute cellRoute;

        /**
         * The number of instances available for this candidate.
         */
        private final int instance;

        /**
         * The weight of this candidate, which may influence routing decisions.
         */
        private final int weight;

        /**
         * The priority of this candidate, which may influence the order in which candidates are considered.
         */
        private final int priority;

        /**
         * The threshold value for this candidate, which may be used to filter candidates during routing.
         */
        private final int threshold;

        /**
         * Constructs a new Candidate with the provided cell route and routing attributes.
         *
         * @param cellRoute The cell route for this candidate.
         * @param instance  The number of instances available for this candidate.
         * @param weight    The weight of this candidate.
         * @param priority  The priority of this candidate.
         * @param threshold The threshold value for this candidate.
         */
        Candidate(CellRoute cellRoute, int instance, int weight, int priority, int threshold) {
            this.cellRoute = cellRoute;
            this.instance = instance;
            this.weight = weight;
            this.priority = priority;
            this.threshold = threshold;
        }

        /**
         * Checks if the election is mutable.
         *
         * @return True if the election can be modified, false otherwise.
         */
        public boolean isMutable() {
            return priority < CellRoute.PRIORITY_PREFIX;
        }

    }

}
