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
package com.jd.live.agent.governance.invoke.filter;

import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.ConstraintRouteFilter.Constraint;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Composite filter that combines multiple constraint route filters.
 */
public class CompositeConstraintRouteFilter implements RouteFilter {

    private final ConstraintRouteFilter[] filters;

    /**
     * Creates a composite filter with the given filters.
     *
     * @param filters the constraint route filters to combine
     */
    public CompositeConstraintRouteFilter(ConstraintRouteFilter... filters) {
        this.filters = filters;
    }

    /**
     * Composes route filters by grouping consecutive ConstraintRouteFilter instances.
     *
     * @param filters the array of route filters to compose
     * @return composed array with grouped ConstraintRouteFilter instances
     */
    public static RouteFilter[] compose(RouteFilter[] filters) {
        if (filters == null) {
            return new RouteFilter[0];
        } else if (filters.length < 2) {
            return filters;
        }
        List<RouteFilter> result = new ArrayList<>(filters.length);
        List<ConstraintRouteFilter> constraints = new ArrayList<>(4);
        for (RouteFilter filter : filters) {
            if (filter instanceof ConstraintRouteFilter) {
                constraints.add((ConstraintRouteFilter) filter);
            } else {
                if (!constraints.isEmpty()) {
                    if (constraints.size() == 1) {
                        result.add(constraints.get(0));
                    } else {
                        result.add(new CompositeConstraintRouteFilter(constraints.toArray(new ConstraintRouteFilter[0])));
                    }
                    constraints.clear();
                }
                result.add(filter);
            }
        }
        return result.toArray(new RouteFilter[0]);
    }

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        CompositeConstraint constraints = getConstraints(invocation);
        if (constraints != null) {
            RouteTarget target = invocation.getRouteTarget();
            constraints.forEach(c -> target.filter(c.getPredicate(), c.getMaxSize(), c.getProtect()));
        }
        chain.filter(invocation);
    }

    /**
     * Collects and combines constraints from all filters.
     *
     * @param <T>        the request type
     * @param invocation the outbound invocation
     * @return combined constraints, or null if no constraints found
     */
    private <T extends OutboundRequest> CompositeConstraint getConstraints(OutboundInvocation<T> invocation) {
        CompositeConstraint constraint = null;
        for (ConstraintRouteFilter filter : filters) {
            if (constraint == null) {
                constraint = new CompositeConstraint(filter.getConstraint(invocation));
            } else {
                constraint.add(filter.getConstraint(invocation));
            }
        }
        return constraint;
    }

    /**
     * Manages composition of multiple constraints.
     */
    private static class CompositeConstraint {

        private Constraint constraint;

        private List<Constraint> constraints;

        /**
         * Creates a composite constraint with initial constraint.
         *
         * @param constraint the initial constraint
         */
        CompositeConstraint(Constraint constraint) {
            this.constraint = constraint;
        }

        /**
         * Adds a constraint to the composite.
         *
         * @param item the constraint to add
         */
        public void add(Constraint item) {
            if (item == null) {
                return;
            }
            if (constraint == null) {
                constraint = item;
            } else if (constraint.isComposable(item)) {
                constraint.compose(item);
            } else {
                if (constraints == null) {
                    constraints = new ArrayList<>(8);
                    constraints.add(constraint);
                }
                constraints.add(item);
                constraint = item;
            }
        }

        /**
         * Applies action to each constraint in the composite.
         *
         * @param consumer the action to apply
         */
        public void forEach(Consumer<Constraint> consumer) {
            if (constraints != null) {
                for (Constraint item : constraints) {
                    consumer.accept(item);
                }
            } else if (constraint != null) {
                consumer.accept(constraint);
            }
        }
    }
}
