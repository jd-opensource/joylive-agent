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
package com.jd.live.agent.governance.exception;

import lombok.Getter;

import java.util.Set;
import java.util.function.Predicate;

/**
 * An interface that defines a retry predicate used to determine if a failed operation should be retried.
 */
public interface ErrorPredicate {

    /**
     * Returns the predicate used to determine if a failed operation should be retried.
     *
     * @return the retry predicate.
     */
    Predicate<Throwable> getPredicate();

    /**
     * Returns the set of exception names that should be retried.
     *
     * @return the set of exception names.
     */
    Set<String> getExceptions();

    /**
     * A default implementation of the ErrorPredicate interface.
     */
    @Getter
    class DefaultErrorPredicate implements ErrorPredicate {

        private final Predicate<Throwable> predicate;

        private final Set<String> exceptions;

        /**
         * Creates a new instance of the DefaultErrorPredicate class.
         *
         * @param predicate  the predicate used to determine if a failed operation should be retried.
         * @param exceptions the set of exception names that should be retried.
         */
        public DefaultErrorPredicate(Predicate<Throwable> predicate, Set<String> exceptions) {
            this.predicate = predicate;
            this.exceptions = exceptions;
        }

        @Override
        public Predicate<Throwable> getPredicate() {
            return predicate;
        }

        @Override
        public Set<String> getExceptions() {
            return exceptions;
        }
    }
}
