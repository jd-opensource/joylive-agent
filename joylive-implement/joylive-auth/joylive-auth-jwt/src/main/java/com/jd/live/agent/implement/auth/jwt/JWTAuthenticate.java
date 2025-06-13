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
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.invoke.auth.Authenticate;
import com.jd.live.agent.governance.invoke.auth.Permission;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.service.auth.AuthPolicy;
import com.jd.live.agent.governance.policy.service.auth.JWTPolicy;
import com.jd.live.agent.governance.request.HttpRequest;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.security.KeyStore;
import lombok.Getter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.core.util.StringUtils.choose;

@Injectable
@Extension("jwt")
public class JWTAuthenticate implements Authenticate {

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthenticate.class);

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject
    private Map<String, KeyStore> keyStores;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    private final Map<Long, JWTToken> tokens = new ConcurrentHashMap<>();

    @Override
    public Permission authenticate(ServiceRequest request, AuthPolicy policy) {
        JWTPolicy jwtPolicy = policy.getJwtPolicy();
        String token = decode(request, jwtPolicy);
        try {
            AlgorithmBuilder factory = AlgorithmBuilderFactory.getBuilder(jwtPolicy.getAlgorithm());
            String issue = request.getHeader(Constants.LABEL_APPLICATION);
            KeyStore keyStore = keyStores.get(choose(jwtPolicy.getKeyStore(), KeyStore.TYPE_CLASSPATH));
            AlgorithmContext context = AlgorithmContext.builder()
                    .role(AlgorithmRole.VERIFY)
                    .keyStore(keyStore)
                    .privateKey(jwtPolicy.getPrivateKey())
                    .publicKey(jwtPolicy.getPublicKey())
                    .issuer(issue)
                    .audience(request.getService())
                    .build();
            // TODO cache algorithm
            Algorithm algorithm = factory == null ? null : factory.create(context);
            if (algorithm == null) {
                return Permission.success();
            }
            JWT.require(algorithm).build().verify(token);
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
    public void inject(OutboundRequest request, AuthPolicy policy) {
        try {
            JWTPolicy jwtPolicy = policy.getJwtPolicy();
            String keyStore = choose(jwtPolicy.getKeyStore(), KeyStore.TYPE_CLASSPATH);
            JWTToken jwtToken = getOrCreateToken(request, policy, keyStore);
            if (jwtToken != null) {
                if (jwtToken.validate()) {
                    String key = jwtPolicy.getKey();
                    if (request.getHeader(key) == null) {
                        request.setHeader(key, decorate(request, key, jwtToken.getToken()));
                    }
                }
            } else {
                logger.warn("Failed to create jwt token for {}", policy.getUri());
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
     * @param policy  the token policy
     * @return the token value, or null if not found
     */
    private String decode(ServiceRequest request, JWTPolicy policy) {
        String key = policy.getKey();
        String token = request.getHeader(key);
        if (token != null && request instanceof HttpRequest && KEY_AUTH.equalsIgnoreCase(key) && token.startsWith(BEARER_PREFIX)) {
            // bear auth
            return token.substring(BEARER_PREFIX.length());
        }
        return token;
    }

    /**
     * Generates or retrieves a cached JWT token for the given request.
     *
     * @param request Service request containing audience details
     * @param policy  Authentication policy with JWT configuration
     * @param store   KeyStore identifier for cryptographic operations
     * @return Valid JWT token, or null if algorithm creation fails
     * @throws Exception if token generation or cryptographic operations fail
     */
    private JWTToken getOrCreateToken(ServiceRequest request, AuthPolicy policy, String store) throws Exception {
        JWTPolicy jwtPolicy = policy.getJwtPolicy();
        AlgorithmBuilder factory = AlgorithmBuilderFactory.getBuilder(jwtPolicy.getAlgorithm());
        if (factory == null) {
            return null;
        }
        KeyStore keyStore = store == null ? null : keyStores.get(store);
        AlgorithmContext context = AlgorithmContext.builder()
                .role(AlgorithmRole.VERIFY)
                .keyStore(keyStore)
                .algorithm(jwtPolicy.getAlgorithm())
                .privateKey(jwtPolicy.getPrivateKey())
                .publicKey(jwtPolicy.getPublicKey())
                .secretKey(jwtPolicy.getSecretKey())
                .issuer(application.getName())
                .audience(request.getService())
                .expireTime(jwtPolicy.getExpireTime())
                .build();
        Algorithm algorithm = factory.create(context);
        if (algorithm == null) {
            return null;
        }
        // cache token to improve performance
        JWTToken jwtToken = tokens.get(policy.getId());
        if (jwtToken == null || jwtToken.isExpired() || !context.equals(jwtToken.getContext())) {
            jwtToken = new JWTToken(policy, context, algorithm);
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
                tokens.remove(token.getId());
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

    /**
     * Immutable holder for JWT token and its metadata.
     * <p>
     * Tracks issuer, audience and expiration time for cached JWT tokens.
     */
    private static class JWTToken {

        @Getter
        private final PolicyId id;

        @Getter
        private final AlgorithmContext context;

        private final Algorithm algorithm;

        @Getter
        private String token;

        private Instant expireAt;

        @Getter
        private long refreshAt;

        private long counter;

        public JWTToken(PolicyId id, AlgorithmContext context, Algorithm algorithm) {
            this.id = id;
            this.context = context;
            this.algorithm = algorithm;
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
                this.refreshAt = expireAt.toEpochMilli() - Timer.getRetryInterval(5000, 15000);
                if (counter++ > 0) {
                    logger.info("Success refreshing jwt token for {}", id.getUri());
                }

            } catch (Throwable e) {
                logger.error("Failed to build jwt token for {}", id.getUri(), e);
            }
        }

    }
}
