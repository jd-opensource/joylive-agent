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
import com.jd.live.agent.governance.invoke.auth.Authenticate;
import com.jd.live.agent.governance.invoke.auth.Permission;
import com.jd.live.agent.governance.policy.service.auth.AuthPolicy;
import com.jd.live.agent.governance.policy.service.auth.JWTPolicy;
import com.jd.live.agent.governance.request.HttpRequest;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.security.KeyStore;

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

    private final Map<String, JWTToken> tokens = new ConcurrentHashMap<>();

    @Override
    public Permission authenticate(ServiceRequest request, AuthPolicy policy) {
        JWTPolicy jwtPolicy = policy.getJwtPolicy();
        String token = decode(request, jwtPolicy);
        try {
            AlgorithmBuilder factory = AlgorithmBuilderFactory.getBuilder(jwtPolicy.getAlgorithm());
            String consumer = request.getHeader(Constants.LABEL_APPLICATION);
            String provider = application.getName();
            String service = request.getService();
            KeyStore keyStore = keyStores.get(choose(jwtPolicy.getKeyStore(), KeyStore.TYPE_CLASSPATH));
            AlgorithmContext context = new AlgorithmContext(jwtPolicy, keyStore, consumer, provider, service);
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
            String store = choose(jwtPolicy.getKeyStore(), KeyStore.TYPE_CLASSPATH);
            JWTToken jwtToken = getJwtToken(request, jwtPolicy, store);
            if (jwtToken != null) {
                String key = jwtPolicy.getKey();
                if (request.getHeader(key) == null) {
                    request.setHeader(key, decorate(request, key, jwtToken.getToken()));
                }
            } else {
                logger.warn("Failed to create JWT token, algorithm: {}, keyStore: {}", jwtPolicy.getAlgorithm(), store);
            }
        } catch (Exception e) {
            // Invalid Signing configuration / Couldn't convert Claims.
            logger.error("Failed to create JWT token", e);
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
        if (token != null
                && request instanceof HttpRequest
                && KEY_AUTH.equalsIgnoreCase(key)
                && token.startsWith(BEARER_PREFIX)) {
            // bear auth
            return token.substring(BEARER_PREFIX.length());
        }
        return token;
    }

    /**
     * Creates or reuses a JWT token based on request and policy.
     * <p>
     * Tokens are cached by issuer+audience and reused until expired.
     *
     * @param request   Target service request (for audience fallback)
     * @param jwtPolicy Configuration for token generation
     * @param store     Key store identifier for signing
     * @return Valid JWT token or null if algorithm unavailable
     */
    private JWTToken getJwtToken(ServiceRequest request, JWTPolicy jwtPolicy, String store) throws Exception {
        JWTToken jwtToken = null;
        AlgorithmBuilder factory = AlgorithmBuilderFactory.getBuilder(jwtPolicy.getAlgorithm());
        String consumer = application.getName();
        String service = request.getService();
        KeyStore keyStore = keyStores.get(store);
        AlgorithmContext context = new AlgorithmContext(jwtPolicy, keyStore, consumer, null, service);
        Algorithm algorithm = factory == null ? null : factory.create(context);
        if (algorithm != null) {
            Instant now = Instant.now();
            String issuer = choose(jwtPolicy.getIssuer(), application.getName());
            String audience = choose(jwtPolicy.getAudience(), request.getService());
            String tokenKey = issuer + "@" + audience;
            jwtToken = tokens.get(tokenKey);
            if (jwtToken == null || jwtToken.isExpired()) {
                Instant expiresAt = now.plus(jwtPolicy.getExpireTime(), ChronoUnit.MILLIS);
                String token = JWT.create()
                        .withIssuer(issuer)
                        .withAudience(audience)
                        .withIssuedAt(now)
                        .withExpiresAt(expiresAt)
                        .sign(algorithm);
                jwtToken = new JWTToken(token, issuer, audience, expiresAt);
                tokens.put(tokenKey, jwtToken);
            }
        }
        return jwtToken;
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

        private final String token;

        private final String issuer;

        private final String audience;

        private final Instant expireTime;

        public JWTToken(String token, String issuer, String audience, Instant expireTime) {
            this.token = token;
            this.issuer = issuer;
            this.audience = audience;
            this.expireTime = expireTime;
        }

        public boolean isExpired() {
            return expireTime.isBefore(Instant.now());
        }

        public String getToken() {
            return token;
        }

        public String getIssuer() {
            return issuer;
        }

        public String getAudience() {
            return audience;
        }
    }
}
