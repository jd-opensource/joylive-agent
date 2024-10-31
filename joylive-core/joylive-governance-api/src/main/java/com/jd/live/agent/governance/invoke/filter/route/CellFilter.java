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
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.LocalFirstMode;
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
 * CellFilter filter cell instances
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Extension(value = "CellFilter", order = RouteFilter.ORDER_LIVE_CELL)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class CellFilter implements RouteFilter {

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
        LocalFirstMode localFirstMode = localFirst ? serviceConfig.getLocalFirstMode() : null;
        Function<String, Integer> thresholdFunc = !localFirst ? null :
                (cellPolicy == CellPolicy.PREFER_LOCAL_CELL
                        ? livePolicy::getCellThreshold : serviceConfig::getCellFailoverThreshold);
        if (target.getUnit() == null) {
            // unit policy is none. maybe not live request or route any.
            return routeAny(invocation, target, localFirstMode, thresholdFunc);
        }
        return routeUnit(invocation, target, localFirstMode, thresholdFunc);
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
                              LocalFirstMode localFirst,
                              Function<String, Integer> thresholdFunc) {
        UnitGroup unitGroup = target.getUnitGroup();
        Election election = sponsor(invocation, target.getUnitRoute(), localFirst, unitGroup, thresholdFunc);
        if (!election.isOver()) {
            randomWeight(election);
        }
        if (election.isMutable()) {
            // Attempt to failover if not in the allow list
            failover(election, invocation.getContext().getLocation());
        }
        Candidate winner = election.getWinner();
        CellRoute cellRoute = winner == null ? null : winner.getCellRoute();
        target.setCellRoute(cellRoute);
        Cell cell = target.getCell();
        if (cell != null) {
            CellGroup cellGroup = unitGroup.getCell(cell.getCode());
            List<? extends Endpoint> endpoints = cellGroup == null ? new ArrayList<>() : cellGroup.getEndpoints();
            // cluster first
            String cluster = localFirst == LocalFirstMode.CLUSTER ? invocation.getContext().getLocation().getCluster() : null;
            if (cluster != null && !cluster.isEmpty() && !endpoints.isEmpty()) {
                endpoints = routeCluster(endpoints, cell, cluster, thresholdFunc);
            }
            target.setEndpoints(endpoints);
            return true;
        }
        return false;
    }

    /**
     * Routes the given endpoints within a cluster, applying a local-first strategy.
     *
     * @param endpoints     the list of endpoints to route
     * @param cell          the cell to which the endpoints belong
     * @param cluster       the name of the cluster to route within
     * @param thresholdFunc a function that takes a cell code and returns the threshold for local-first routing
     * @return the filtered list of endpoints
     */
    private List<? extends Endpoint> routeCluster(List<? extends Endpoint> endpoints,
                                                  Cell cell,
                                                  String cluster,
                                                  Function<String, Integer> thresholdFunc) {
        Integer threshold = thresholdFunc.apply(cell.getCode());
        List<Endpoint> clusters = new ArrayList<>(endpoints.size() / 2);
        List<Endpoint> others = new ArrayList<>(endpoints.size() / 2);
        endpoints.forEach(endpoint -> {
            if (endpoint.isCluster(cluster)) {
                clusters.add(endpoint);
            } else {
                others.add(endpoint);
            }
        });
        int size = clusters.size();
        if (!clusters.isEmpty() && size < endpoints.size()) {
            if (threshold == null || size >= threshold) {
                return clusters;
            } else {
                // threshold=3, shortage=2
                int shortage = threshold - size;
                // 0,1,2
                int random = ThreadLocalRandom.current().nextInt(threshold);
                if (random >= shortage) {
                    // 2
                    return clusters;
                } else {
                    // 0,1
                    shortage = shortage - others.size();
                    if (random >= shortage) {
                        return others;
                    }
                }
            }
        }
        return endpoints;
    }

    /**
     * Routes an outbound invocation to suitable endpoints
     *
     * @param invocation The outbound invocation containing metadata for the election.
     * @param target The {@code RouteTarget} containing the endpoints to be filtered.
     * @param localFirst Local first mode.
     * @param thresholdFunc A function that returns a threshold value for the current cell. If the number of local endpoints exceeds this threshold,
     * those endpoints are preferred.
     * @return true if the routing decision was successful, false otherwise.
     */
    private boolean routeAny(OutboundInvocation<?> invocation,
                             RouteTarget target,
                             LocalFirstMode localFirst,
                             Function<String, Integer> thresholdFunc) {
        LiveMetadata liveMetadata = invocation.getLiveMetadata();
        LiveSpace targetSpace = liveMetadata.getTargetSpace();
        if (targetSpace == null) {
            return true;
        }
        Unit preferUnit = localFirst != null ? targetSpace.getLocalUnit() : null;
        Unit centerUnit = targetSpace.getCenter();
        Cell preferCell = localFirst != null ? targetSpace.getLocalCell() : null;
        String preferCloud = localFirst != null ? invocation.getContext().getLocation().getCloud() : null;
        String cluster = localFirst == LocalFirstMode.CLUSTER ? invocation.getContext().getLocation().getCluster() : null;
        Set<String> unavailableCells = getUnavailableCells(invocation);
        if (!unavailableCells.isEmpty()) {
            target.filter(endpoint -> !unavailableCells.contains(endpoint.getCell()));
        }
        // prefer local cluster>local cell>local cloud>local unit>center unit>other unit
        List<Endpoint> preferClusterEndpoints = new ArrayList<>();
        List<Endpoint> preferCellEndpoints = new ArrayList<>();
        List<Endpoint> preferCloudEndpoints = new ArrayList<>();
        List<Endpoint> preferUnitEndpoints = new ArrayList<>();
        List<Endpoint> centerUnitEndpoints = new ArrayList<>();
        List<Endpoint> otherUnitEndpoints = new ArrayList<>();
        for (Endpoint endpoint : target.getEndpoints()) {
            if (preferUnit != null && endpoint.isUnit(preferUnit.getCode())) {
                if (preferCell != null && endpoint.isCell(preferCell.getCode())) {
                    if (cluster != null && endpoint.isCluster(cluster)) {
                        preferClusterEndpoints.add(endpoint);
                    } else {
                        preferCellEndpoints.add(endpoint);
                    }
                } else if (preferCloud != null && endpoint.isCloud(preferCloud)) {
                    preferCloudEndpoints.add(endpoint);
                } else {
                    preferUnitEndpoints.add(endpoint);
                }
            } else if (centerUnit != null && endpoint.isUnit(centerUnit.getCode())) {
                centerUnitEndpoints.add(endpoint);
            } else {
                otherUnitEndpoints.add(endpoint);
            }
        }
        List<Endpoint>[] candidates = new List[]{
                preferClusterEndpoints, preferCellEndpoints, preferCloudEndpoints,
                preferUnitEndpoints, centerUnitEndpoints, otherUnitEndpoints
        };
        Integer threshold = preferCell == null ? null : thresholdFunc.apply(preferCell.getCode());
        if (threshold == null || threshold <= 0) {
            for (List<Endpoint> candidate : candidates) {
                if (!candidate.isEmpty()) {
                    target.setEndpoints(candidate);
                    break;
                }
            }
        } else {
            int random = ThreadLocalRandom.current().nextInt(threshold);
            int shortage = threshold;
            for (List<Endpoint> candidate : candidates) {
                shortage = shortage - candidate.size();
                if (random >= shortage) {
                    target.setEndpoints(candidate);
                    break;
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
        LiveMetadata metadata = invocation.getLiveMetadata();
        List<Unit> units = metadata.getTargetSpace().getSpec().getUnits();

        if (units != null) {
            UnitRule rule = metadata.getRule();
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
                             LocalFirstMode localFirst,
                             UnitGroup unitGroup,
                             Function<String, Integer> failoverThresholdFunc) {
        // Extract necessary information from the invocation metadata.
        LiveMetadata metadata = invocation.getLiveMetadata();
        String variable = metadata.getVariable();
        Cell localCell = metadata.getTargetLocalCell();

        // Initialize variables to keep track of the total weight, instance count, max priority, and preferred candidate.
        int weights = 0;
        int instances = 0;
        int maxPriority = -1;
        Candidate prefer = null;
        List<Candidate> candidates = new ArrayList<>(unitRoute.size());

        // TODO Deployed partitions and ingress traffic rules may not always match.
        // Iterate through the cells in the unit route.
        for (CellRoute cellRoute : unitRoute.getCells()) {
            Cell cell = cellRoute.getCell();
            // Check if the cell is accessible and has a non-empty route.
            if (invocation.isAccessible(cell) && !cellRoute.isEmpty() && invocation.isAccessible(cellRoute.getAccessMode())) {
                // Get the instance count for the cell from the unit group, if available.
                CellGroup cellGroup = unitGroup == null ? null : unitGroup.getCell(cellRoute.getCode());
                Integer instance = cellGroup == null ? null : cellGroup.size();
                instance = instance == null ? 0 : instance;

                // Degrade the weight to 0 if the cell has instances.
                instances += instance;
                int weight = instance == 0 ? 0 : cellRoute.getWeight();
                weights += weight;

                // Determine the priority of the cell based on the variable and local preference.
                int priority = cellRoute.getPriority(variable, localFirst != null ? localCell : null);

                // Get the failover threshold for the cell using the provided function.
                Integer threshold = localFirst == null ? null : failoverThresholdFunc.apply(cell.getCode());
                threshold = threshold == null ? 0 : threshold;

                String cloud = cellRoute.getCell().getLabel(Cell.LABEL_CLOUD);
                if (cloud == null) {
                    cloud = cellGroup == null || cellGroup.isEmpty() ? "" : cellGroup.getEndpoints().get(0).getCloud();
                }

                // Create a Candidate object and add it to the list of candidates.
                Candidate candidate = new Candidate(cellRoute, instance, weight, priority, threshold, cloud);
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
     * @param location The current location.
     */
    private void failover(Election election, Location location) {
        // If there's only one candidate or fewer, there's no failover needed.
        if (election.size() <= 1) {
            return;
        }

        // Retrieve the current winner's information.
        Candidate winner = election.getWinner();
        int threshold = winner.getThreshold();
        int shortage = threshold - winner.getInstance();
        if (shortage <= 0) {
            return;
        }

        // If the current winner does not meet the threshold, proceed with failover logic.
        int redundants = 0;
        List<Candidate> targets = new ArrayList<>(election.size() - 1);

        // Iterate through the candidates to find potential failover targets.
        for (Candidate candidate : election.getCandidates()) {
            if (candidate != winner) {
                // Check if the candidate has instances above their threshold.
                if (candidate.getRedundant() > 0) {
                    targets.add(candidate);
                    redundants += candidate.getRedundant();
                }
            }
        }

        // If there are potential targets, calculate the failover ratio.
        if (redundants > 0) {
            // sort by cloud
            sortByCloud(targets, location);
            if (shortage + redundants < threshold) {
                threshold = winner.getInstance() + redundants;
                shortage = redundants;
            }
            int random = ThreadLocalRandom.current().nextInt(threshold);
            if (random < shortage) {
                // Find the candidate to failover to based on the random number.
                for (Candidate candidate : targets) {
                    shortage -= candidate.getRedundant();
                    if (random >= shortage) {
                        election.setWinner(candidate);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Sorts the given list of candidates based on their cloud label, giving priority to candidates with a matching cloud label.
     *
     * @param candidates The list of candidates to sort.
     * @param location   The location object containing the cloud label to match against.
     */
    private void sortByCloud(List<Candidate> candidates, Location location) {
        String cloud = location.getCloud();
        if (cloud != null && !cloud.isEmpty()) {
            // prefer local cloud
            candidates.sort((o1, o2) -> {
                String cloud1 = o1.getCloud();
                String cloud2 = o2.getCloud();
                if (cloud.equals(cloud1)) {
                    return cloud.equals(cloud2) ? randomOrder() : -1;
                } else {
                    return cloud.equals(cloud2) ? 1 : randomOrder();
                }
            });
        }
    }

    private int randomOrder() {
        return ThreadLocalRandom.current().nextInt(2) < 1 ? -1 : 1;
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
         * The cloud value for this candidate, which may be used to filter candidates during routing.
         */
        private final String cloud;

        /**
         * Constructs a new Candidate with the provided cell route and routing attributes.
         *
         * @param cellRoute The cell route for this candidate.
         * @param instance  The number of instances available for this candidate.
         * @param weight    The weight of this candidate.
         * @param priority  The priority of this candidate.
         * @param threshold The threshold value for this candidate.
         * @param cloud The cloud value for this candidate.
         */
        Candidate(CellRoute cellRoute, int instance, int weight, int priority, int threshold, String cloud) {
            this.cellRoute = cellRoute;
            this.instance = instance;
            this.weight = weight;
            this.priority = priority;
            this.threshold = threshold;
            this.cloud = cloud;
        }

        /**
         * Checks if the election is mutable.
         *
         * @return True if the election can be modified, false otherwise.
         */
        public boolean isMutable() {
            return priority < CellRoute.PRIORITY_PREFIX;
        }

        public int getRedundant() {
            return instance > threshold ? instance - threshold : 0;
        }

    }

}
