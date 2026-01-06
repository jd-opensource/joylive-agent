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

import java.lang.reflect.Method;
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
            if (isDefaultConstraintRouteFilter(filter)) {
                constraints.add((ConstraintRouteFilter) filter);
            } else if (!constraints.isEmpty()) {
                result.add(compose(constraints));
                result.add(filter);
                constraints.clear();
            } else {
                result.add(filter);
            }
        }
        return result.toArray(new RouteFilter[0]);
    }

    /**
     * Composes multiple constraint route filters into a single filter.
     * Returns null for empty list, the single filter for one element,
     * or a composite filter for multiple elements.
     *
     * @param filters the list of constraint route filters to compose
     * @return composed route filter, or null if filters is empty
     */
    private static RouteFilter compose(List<ConstraintRouteFilter> filters) {
        switch (filters.size()) {
            case 0:
                return null;
            case 1:
                return filters.get(0);
            default:
                return new CompositeConstraintRouteFilter(filters.toArray(new ConstraintRouteFilter[0]));
        }
    }

    /**
     * Checks if the filter is a default ConstraintRouteFilter instance.
     * A default instance has the filter method declared in ConstraintRouteFilter class itself.
     *
     * @param filter the route filter to check
     * @return true if it's a default ConstraintRouteFilter, false otherwise
     */
    private static boolean isDefaultConstraintRouteFilter(RouteFilter filter) {
        if (filter instanceof ConstraintRouteFilter) {
            try {
                Method method = filter.getClass().getMethod("filter", OutboundInvocation.class, RouteFilterChain.class);
                if (method.getDeclaringClass() == ConstraintRouteFilter.class) {
                    return true;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return false;
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
