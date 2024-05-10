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
package com.jd.live.agent.governance.invoke;

import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.invoke.matcher.TagMatcher;
import com.jd.live.agent.governance.invoke.retry.RetrierFactory;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.variable.UnitFunction;
import com.jd.live.agent.governance.policy.variable.VariableFunction;
import com.jd.live.agent.governance.policy.variable.VariableParser;

import java.util.Map;

/**
 * The {@code InvocationContext} interface defines a contract for an invocation context in a component-based application.
 * It provides methods to retrieve various configurations and functional interfaces that are relevant to the invocation
 * of components, such as application information, governance configurations, policy suppliers, and various functional
 * mappings for unit functions, variable functions, variable parsers, and tag matchers.
 */
public interface InvocationContext {

    /**
     * The constant string representing the key used to store or retrieve the InvocationContext.
     */
    String COMPONENT_INVOCATION_CONTEXT = "invocationContext";

    /**
     * Retrieves the application information associated with this invocation context.
     *
     * @return An instance of {@code Application} representing the application information.
     */
    Application getApplication();

    /**
     * Retrieves the governance configuration associated with this invocation context.
     *
     * @return An instance of {@code GovernanceConfig} representing the governance configurations.
     */
    GovernanceConfig getGovernanceConfig();

    /**
     * Retrieves the policy supplier associated with this invocation context.
     *
     * @return An instance of {@code PolicySupplier} that supplies policies.
     */
    PolicySupplier getPolicySupplier();

    /**
     * Retrieves a unit function by its name.
     *
     * @param name The name of the unit function.
     * @return The requested unit function, or null if not found.
     */
    UnitFunction getUnitFunction(String name);

    /**
     * Retrieves a variable function by its name.
     *
     * @param name The name of the variable function.
     * @return The requested variable function, or null if not found.
     */
    VariableFunction getVariableFunction(String name);

    /**
     * Retrieves a variable parser by its name.
     *
     * @param name The name of the variable parser.
     * @return The requested variable parser, or null if not found.
     */
    VariableParser<?, ?> getVariableParser(String name);

    /**
     * Retrieves the {@code RetrierFactory} instance associated with the specified name,
     * or returns the default factory instance if no factory is found with that name.
     *
     * @param name the name of the {@code RetrierFactory} to retrieve. If {@code null} or
     *             does not match any existing factory, the default factory is returned.
     * @return the {@code RetrierFactory} instance associated with the given name, or the
     *         default factory if no matching name is found.
     */
    RetrierFactory getOrDefaultRetrierFactory(String name);

    /**
     * Retrieves a map of tag matchers associated with this invocation context.
     *
     * @return A map of strings to {@code TagMatcher} instances.
     */
    Map<String, TagMatcher> getTagMatchers();

    /**
     * Retrieves the {@code LoadBalancer} instance associated with the specified name,
     *      * or returns the default loadbalancer instance if no loadbalancer is found with that name.
     *
     * @param name The name of the loadbalancer.
     * @return the {@code LoadBalancer} instance associated with the given name, or the
     *         default loadbalancer if no matching name is found.
     */
    LoadBalancer getOrDefaultLoadBalancer(String name);

    /**
     * Retrieves the {@code ClusterInvoker} instance associated with the specified name,
     * * or returns the default ClusterInvoker instance if no ClusterInvoker is found with that name.
     *
     * @param name The name of the loadbalancer.
     * @return the {@code ClusterInvoker} instance associated with the given name, or the
     * default ClusterInvoker if no matching name is found.
     */
    ClusterInvoker getOrDefaultClusterInvoker(String name);

    /**
     * A delegate class for {@link InvocationContext} that forwards all its operations to another {@link InvocationContext} instance.
     * This class acts as a wrapper or intermediary, allowing for additional behaviors to be inserted before or after
     * the delegation of method calls. It implements the {@link InvocationContext} interface and can be used
     * anywhere an InvocationContext is required, providing a flexible mechanism for extending or modifying the behavior
     * of invocation contexts dynamically.
     *
     * <p>This delegate pattern is particularly useful for adding cross-cutting concerns like logging, monitoring,
     * or security checks in a transparent manner, without altering the original behavior of the invocation context.</p>
     *
     * @see InvocationContext
     */
    class InvocationContextDelegate implements InvocationContext {

        /**
         * The {@link InvocationContext} instance to which this delegate will forward all method calls.
         */
        protected final InvocationContext delegate;

        /**
         * Constructs a new {@code InvocationContextDelegate} with a specified {@link InvocationContext} to delegate to.
         *
         * @param delegate The {@link InvocationContext} instance that this delegate will forward calls to.
         */
        public InvocationContextDelegate(InvocationContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public Application getApplication() {
            return delegate.getApplication();
        }

        @Override
        public GovernanceConfig getGovernanceConfig() {
            return delegate.getGovernanceConfig();
        }

        @Override
        public PolicySupplier getPolicySupplier() {
            return delegate.getPolicySupplier();
        }

        @Override
        public UnitFunction getUnitFunction(String name) {
            return delegate.getUnitFunction(name);
        }

        @Override
        public VariableFunction getVariableFunction(String name) {
            return delegate.getVariableFunction(name);
        }

        @Override
        public VariableParser<?, ?> getVariableParser(String name) {
            return delegate.getVariableParser(name);
        }

        @Override
        public RetrierFactory getOrDefaultRetrierFactory(String name) {
            return delegate.getOrDefaultRetrierFactory(name);
        }

        @Override
        public Map<String, TagMatcher> getTagMatchers() {
            return delegate.getTagMatchers();
        }

        @Override
        public LoadBalancer getOrDefaultLoadBalancer(String name) {
            return delegate.getOrDefaultLoadBalancer(name);
        }

        @Override
        public ClusterInvoker getOrDefaultClusterInvoker(String name) {
            return delegate.getOrDefaultClusterInvoker(name);
        }
    }

}
