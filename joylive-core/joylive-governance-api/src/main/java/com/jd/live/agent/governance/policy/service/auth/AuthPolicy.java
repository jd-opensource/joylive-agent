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
package com.jd.live.agent.governance.policy.service.auth;

import com.jd.live.agent.governance.policy.PolicyId;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static java.util.Collections.sort;

/**
 * Auth policy
 *
 * @since 1.2.0
 */
public class AuthPolicy extends PolicyId implements Serializable {

    public static final String AUTH_TYPE_TOKEN = "token";

    public static final String AUTH_TYPE_JWT = "jwt";

    @Getter
    @Setter
    private String application;

    /**
     * The type of the auth policy.
     */
    @Getter
    @Setter
    private String type;

    @Getter
    private List<TokenPolicy> tokenPolicies;

    @Getter
    private List<JWTPolicy> jwtPolicies;

    @Getter
    @Setter
    private List<Map<String, String>> params;

    public AuthPolicy() {
    }

    /**
     * Gets the best effective token policy at specified time.
     *
     * @return the latest effective token policy, or null if none found
     */
    public TokenPolicy getLatestEffectiveTokenPolicy() {
        return getLatestEffectivePolicy(tokenPolicies);
    }

    /**
     * Gets the best effective JWT policy at specified time.
     *
     * @return the latest effective JWT policy, or null if none found
     */
    public JWTPolicy getLatestEffectiveJwtPolicy() {
        return getLatestEffectivePolicy(jwtPolicies);
    }

    /**
     * Gets the latest effective policy from a list of auth strategies.
     *
     * @param <T>      type of auth strategy
     * @param policies list of policies to check
     * @return first valid and effective policy found, or null if none found
     */
    protected <T extends AuthStrategy> T getLatestEffectivePolicy(List<T> policies) {
        long time = System.currentTimeMillis();
        int size = policies == null ? 0 : policies.size();
        switch (size) {
            case 0:
                return null;
            case 1:
                T first = policies.get(0);
                return first.isEffective(time) ? first : null;
            default:
                for (T policy : policies) {
                    if (policy.isEffective(time)) {
                        return policy;
                    }
                }
                return null;
        }
    }

    /**
     * Checks if this strategy matches given application.
     * Returns true if application is null/empty or equals to the given one.
     *
     * @param application application name to match
     * @return true if matches, false otherwise
     */
    public boolean match(String application) {
        return this.application == null || this.application.isEmpty() || this.application.equals(application);
    }

    public void cache() {
        if ((tokenPolicies == null || tokenPolicies.isEmpty()) && AUTH_TYPE_TOKEN.equals(type) && params != null && !params.isEmpty()) {
            tokenPolicies = toList(params, p -> {
                TokenPolicy tokenPolicy = new TokenPolicy(p);
                return tokenPolicy.isValid() ? tokenPolicy : null;
            });
        }
        if ((jwtPolicies == null || jwtPolicies.isEmpty()) && AUTH_TYPE_JWT.equals(type) && params != null && !params.isEmpty()) {
            jwtPolicies = toList(params, p -> {
                JWTPolicy jwtPolicy = new JWTPolicy(p);
                return jwtPolicy.isValid() ? jwtPolicy : null;
            });
        }
        supplement();
        if (tokenPolicies != null) {
            tokenPolicies.forEach(TokenPolicy::cache);
            sort(tokenPolicies, (o1, o2) -> Long.compare(o2.getStartTime(), o1.getStartTime()));
        }
        if (jwtPolicies != null) {
            jwtPolicies.forEach(JWTPolicy::cache);
            sort(jwtPolicies, (o1, o2) -> Long.compare(o2.getStartTime(), o1.getStartTime()));
        }
    }

    protected void supplement() {
        AtomicInteger counter = new AtomicInteger(0);
        if (tokenPolicies != null) {
            for (TokenPolicy tokenPolicy : tokenPolicies) {
                tokenPolicy.supplement(() -> uri.parameter(KEY_TOKEN_POLICY_ID, String.valueOf(
                        tokenPolicy.getId() == null ? counter.incrementAndGet() : tokenPolicy.getId()
                )));
            }
        }
        if (jwtPolicies != null) {
            counter.set(0);
            for (JWTPolicy jwtPolicy : jwtPolicies) {
                jwtPolicy.supplement(() -> uri.parameter(KEY_JWT_POLICY_ID, String.valueOf(
                        jwtPolicy.getId() == null ? counter.incrementAndGet() : jwtPolicy.getId()
                )));
            }
        }
    }
}
