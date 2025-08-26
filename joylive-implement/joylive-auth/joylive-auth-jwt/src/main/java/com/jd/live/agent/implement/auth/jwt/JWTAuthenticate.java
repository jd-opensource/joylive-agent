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
package com.jd.live.agent.implement.auth.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.security.KeyStore;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.invoke.auth.Authenticate;
import com.jd.live.agent.governance.invoke.auth.Permission;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.service.auth.AuthPolicy;
import com.jd.live.agent.governance.policy.service.auth.JWTAlgorithmContext;
import com.jd.live.agent.governance.policy.service.auth.JWTAlgorithmRole;
import com.jd.live.agent.governance.policy.service.auth.JWTPolicy;
import com.jd.live.agent.governance.request.HttpRequest;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import lombok.Getter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static com.jd.live.agent.core.util.StringUtils.choose;

@Injectable
@Extension("jwt")
public class JWTAuthenticate implements Authenticate {

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthenticate.class);

    @Inject
    private Map<String, KeyStore> keyStores;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    // jwt algorithm cache
    private final Map<Long, JWTAlgorithm> algorithms = new ConcurrentHashMap<>();

    // jwt token cache
    private final Map<Long, JWTToken> tokens = new ConcurrentHashMap<>();

    @Override
    public Permission authenticate(ServiceRequest request, AuthPolicy policy, String service, String consumer) {
        String token = decode(request, policy);
        try {
            JWTAlgorithm algorithm = getOrCreateAlgorithm(policy, () -> getVerifyContext(policy.getJwtPolicy()));
            if (algorithm == null) {
                return Permission.success();
            }
            JWT.require(algorithm.getAlgorithm()).withIssuer(consumer).withAudience(service).build().verify(token);
            return Permission.success();
        } catch (JWTVerificationException e) {
            return Permission.failure("Failed to verify JWT token " + token);
        } catch (Exception e) {
            // Invalid Signing configuration / Couldn't convert Claims.
            logger.error("Failed to create JWT token", e);
            return Permission.success();
        }
    }

    @Override
    public void inject(OutboundRequest request, AuthPolicy policy, String service, String consumer) {
        try {
            JWTPolicy jwtPolicy = policy.getJwtPolicy();
            String key = jwtPolicy.getKey();
            if (request.getHeader(key) == null) {
                JWTToken jwtToken = getOrCreateToken(policy, () -> getSignatureContext(jwtPolicy, consumer, service));
                if (jwtToken != null && jwtToken.validate()) {
                    request.setHeader(key, decorate(request, key, jwtToken.getToken()));
                } else {
                    logger.warn("Failed to create jwt token for {}", policy.getUri());
                }
            }
        } catch (Exception e) {
            // Invalid Signing configuration / Couldn't convert Claims.
            logger.error("Failed to create jwt token for {}", policy.getUri(), e);
        }
    }

    /**
     * Retrieves a token from the specified service request using the given key.
     *
     * @param request the service request
     * @param policy  the auth policy
     * @return the token value, or null if not found
     */
    private String decode(ServiceRequest request, AuthPolicy policy) {
        String key = policy.getJwtPolicy().getKey();
        String token = request.getHeader(key);
        if (token != null && request instanceof HttpRequest && KEY_AUTH.equalsIgnoreCase(key) && token.startsWith(BEARER_PREFIX)) {
            // bear auth
            return token.substring(BEARER_PREFIX.length());
        }
        return token;
    }

    /**
     * Get or create verification context from JWT policy configuration.
     *
     * @param policy The JWT policy containing key material and algorithm
     * @return Context configured for signature verification
     */
    private JWTAlgorithmContext getVerifyContext(JWTPolicy policy) {
        return policy.getVerifyContext(() -> JWTAlgorithmContext.builder()
                .role(JWTAlgorithmRole.VERIFY)
                .keyStore(getKeyStore(policy))
                .algorithm(policy.getAlgorithm())
                .privateKey(policy.getPrivateKey())
                .publicKey(policy.getPublicKey())
                .secretKey(policy.getSecretKey())
                .expireTime(policy.getExpireTime())
                .build());
    }

    /**
     * Get or create JWT signing context with identity and crypto configuration.
     *
     * @param policy   JWT security policy with keys and algorithm
     * @param issuer   Token issuer identifier
     * @param audience Intended token recipient
     * @return Configured signing context
     */
    private JWTAlgorithmContext getSignatureContext(JWTPolicy policy, String issuer, String audience) {
        return policy.getSignatureContext(() -> JWTAlgorithmContext.builder()
                .role(JWTAlgorithmRole.SIGNATURE)
                .keyStore(getKeyStore(policy))
                .algorithm(policy.getAlgorithm())
                .privateKey(policy.getPrivateKey())
                .publicKey(policy.getPublicKey())
                .secretKey(policy.getSecretKey())
                .expireTime(policy.getExpireTime())
                .issuer(issuer)
                .audience(audience)
                .build());
    }

    /**
     * Gets the keystore configured in the JWT policy (falling back to default classpath keystore).
     *
     * @param policy JWT policy containing keystore configuration
     * @return Configured keystore instance, or null if not found
     */
    private KeyStore getKeyStore(JWTPolicy policy) {
        return keyStores.get(choose(policy.getKeyStore(), KeyStore.TYPE_CLASSPATH));
    }

    /**
     * Gets cached algorithm instance or creates new one if needed.
     *
     * @param policy   Authentication policy containing JWT configuration
     * @param supplier Context factory for algorithm creation
     * @return Initialized algorithm instance, or null if unsupported
     * @throws Exception If algorithm initialization fails
     */
    private JWTAlgorithm getOrCreateAlgorithm(AuthPolicy policy, Supplier<JWTAlgorithmContext> supplier) throws Exception {
        JWTAlgorithmContext context = supplier.get();
        JWTAlgorithm jwtAlgorithm = algorithms.get(policy.getId());
        if (jwtAlgorithm == null || jwtAlgorithm.getContext() != context) {
            JWTAlgorithmBuilder factory = JWTAlgorithmBuilderFactory.getBuilder(policy.getJwtPolicy().getAlgorithm());
            if (factory == null) {
                return null;
            }
            jwtAlgorithm = new JWTAlgorithm(policy, context, factory.create(context));
            algorithms.put(policy.getId(), jwtAlgorithm);
        }
        return jwtAlgorithm;
    }

    /**
     * Generates or retrieves a cached JWT token for the given request.
     *
     * @param policy  Authentication policy with JWT configuration
     * @return Valid JWT token, or null if algorithm creation fails
     * @throws Exception if token generation or cryptographic operations fail
     */
    private JWTToken getOrCreateToken(AuthPolicy policy, Supplier<JWTAlgorithmContext> supplier) throws Exception {
        JWTAlgorithm jwtAlgorithm = getOrCreateAlgorithm(policy, supplier);
        if (jwtAlgorithm == null) {
            return null;
        }
        // cache token to improve performance
        JWTToken jwtToken = tokens.get(policy.getId());
        if (jwtToken == null
                || jwtToken.isExpired()
                || jwtToken.getContext() != jwtAlgorithm.getContext()) {
            // new token
            jwtToken = new JWTToken(jwtAlgorithm);
            jwtToken.build();
            addRefreshTask(jwtToken);
            tokens.put(policy.getId(), jwtToken);
        }
        return jwtToken;
    }

    /**
     * Schedules a task to automatically refresh a JWT token before it expires.
     *
     * @param token The JWT token to refresh, must provide refresh timing via {@link JWTToken#getRefreshAt()}
     */
    private void addRefreshTask(JWTToken token) {
        timer.add("jwt-token-refresher", token.getRefreshAt(), () -> {
            if (token.isExpired()) {
                tokens.remove(token.getId().getId());
            } else {
                token.build();
                addRefreshTask(token);
            }
        });
    }

    /**
     * Decorates a token with 'Bearer' prefix when required for HTTP auth headers.
     *
     * @param request Target service request (checked for HTTP type)
     * @param key     Header name (checked against auth constant)
     * @param token   Original token value
     * @return Original token or Bearer-prefixed token if auth header required
     */
    private String decorate(ServiceRequest request, String key, String token) {
        if (request instanceof HttpOutboundRequest && key.equalsIgnoreCase(KEY_AUTH) && !token.startsWith(BEARER_PREFIX)) {
            token = BEARER_PREFIX + token;
        }
        return token;
    }

    @Getter
    private static class JWTAlgorithm {

        protected final PolicyId id;

        protected final JWTAlgorithmContext context;

        protected final Algorithm algorithm;

        JWTAlgorithm(PolicyId id, JWTAlgorithmContext context, Algorithm algorithm) {
            this.id = id;
            this.context = context;
            this.algorithm = algorithm;
        }
    }

    /**
     * Immutable holder for JWT token and its metadata.
     * <p>
     * Tracks issuer, audience and expiration time for cached JWT tokens.
     */
    private static class JWTToken extends JWTAlgorithm {

        @Getter
        private String token;

        private Instant expireAt;

        @Getter
        private long refreshAt;

        private long counter;

        JWTToken(PolicyId id, JWTAlgorithmContext context, Algorithm algorithm) {
            super(id, context, algorithm);
        }

        JWTToken(JWTAlgorithm algorithm) {
            this(algorithm.getId(), algorithm.getContext(), algorithm.getAlgorithm());
        }

        /**
         * Checks if the token has expired based on its expiration time.
         *
         * @return true if token has an expiration time and it's before current time
         */
        public boolean isExpired() {
            return expireAt != null && expireAt.isBefore(Instant.now());
        }

        /**
         * Validates basic token existence.
         *
         * @return true if the token string exists (non-null)
         */
        public boolean validate() {
            return token != null;
        }

        /**
         * Builds or refreshes the JWT token with current timestamp and configured expiration.
         */
        public void build() {
            try {
                Instant now = Instant.now();
                Instant expireAt = now.plus(context.getExpireTime(), ChronoUnit.MILLIS);
                this.token = JWT.create().withIssuer(context.getIssuer()).withAudience(context.getAudience())
                        .withIssuedAt(now).withExpiresAt(expireAt).sign(algorithm);
                this.expireAt = expireAt;
                this.refreshAt = expireAt.toEpochMilli() - Timer.getRetryInterval(10000, 30000);
                if (counter++ > 0) {
                    logger.info("Success refreshing jwt token for {}", id.getUri());
                }

            } catch (Throwable e) {
                logger.error("Failed to build jwt token for {}", id.getUri(), e);
            }
        }

    }
}
