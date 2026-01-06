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

import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import lombok.Getter;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Route filter that applies constraints to filter endpoints based on predicates.
 */
public interface ConstraintRouteFilter extends RouteFilter {

    @Override
    default <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        Constraint constraint = getConstraint(invocation);
        if (constraint != null) {
            invocation.getRouteTarget().filter(constraint.predicate, constraint.maxSize, constraint.protect);
        }
    }

    /**
     * Gets the constraint for the given invocation.
     *
     * @param <T>        the outbound request type
     * @param invocation the outbound invocation
     * @return the constraint to apply
     */
    <T extends OutboundRequest> Constraint getConstraint(OutboundInvocation<T> invocation);

    /**
     * Constraint definition containing predicate, size limit and nullability settings.
     */
    @Getter
    class Constraint {

        private Predicate<Endpoint> predicate;

        private int maxSize;

        private BiPredicate<Integer, Integer> protect;

        /**
         * Creates a constraint with the given parameters.
         *
         * @param predicate the endpoint predicate
         */
        public Constraint(Predicate<Endpoint> predicate) {
            this(predicate, -1, null);
        }

        /**
         * Creates a constraint with the given parameters.
         *
         * @param predicate the endpoint predicate
         * @param maxSize   the maximum size limit
         * @param protect   Predicate controlling element removal (null=allow)
         */
        public Constraint(Predicate<Endpoint> predicate, int maxSize, BiPredicate<Integer, Integer> protect) {
            this.predicate = predicate;
            this.maxSize = maxSize;
            this.protect = protect;
        }

        public boolean isComposable(Constraint other) {
            return other == null || other.protect == protect;
        }

        /**
         * Composes this constraint with another constraint.
         *
         * @param other the other constraint to combine with
         * @return the composed constraint with combined predicates, minimum maxSize, and logical AND of nullable flags
         */
        public void compose(Constraint other) {
            if (other == null) {
                return;
            }
            predicate = predicate.and(other.predicate);
            if (maxSize <= 0) {
                maxSize = other.maxSize;
            } else if (other.maxSize > 0) {
                maxSize = Math.min(maxSize, other.maxSize);
            }
        }
    }

}
