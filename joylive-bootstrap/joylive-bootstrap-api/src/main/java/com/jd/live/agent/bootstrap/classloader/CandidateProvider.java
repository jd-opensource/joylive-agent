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
package com.jd.live.agent.bootstrap.classloader;

/**
 * Represents a functional interface for providing a candidate class loader. This interface
 * is used in class loading operations to supply a custom class loader that will be used
 * to find candidate classes for loading.
 * <p>
 * As a functional interface, an instance of it can be created with a lambda expression,
 * method reference, or an anonymous class that implements the single abstract method.
 */
@FunctionalInterface
public interface CandidateProvider {

    ThreadLocal<CandidateFeature> CONTEXT_LOADER_ENABLED = new ThreadLocal<>();

    /**
     * Gets the candidate class loader that should be used for class loading operations.
     *
     * @return The {@link ClassLoader} to be used as a candidate for loading classes.
     */
    ClassLoader[] getCandidates();

    static CandidateFeature getCandidateFeature() {
        return CONTEXT_LOADER_ENABLED.get();
    }

    static CandidateFeature setCandidateFeature(CandidateFeature feature) {
        CandidateFeature result = CONTEXT_LOADER_ENABLED.get();
        CONTEXT_LOADER_ENABLED.set(feature);
        return result;
    }

}

