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

import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.governance.invoke.auth.Authenticate;
import com.jd.live.agent.governance.policy.PolicyId;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Supplier;

/**
 * JWT policy
 *
 * @since 1.8.0
 */
public class JWTPolicy extends PolicyId implements AuthStrategy, Serializable {

    private static final long ONE_MINUTE = 60 * 1000L;
    private static final long FILE_MINUTE = 5 * ONE_MINUTE;
    private static final long DEFAULT_EXPIRE_TIME = 15 * ONE_MINUTE;
    private static final String DEFAULT_KEY_STORE = "file";
    private static final String DEFAULT_ALGORITHM = "HMAC256";

    private static final String KEY_JWT_KEY = "key";
    private static final String KEY_JWT_ALGORITHM = "algorithm";
    private static final String KEY_JWT_KEY_STORE = "keyStore";
    private static final String KEY_JWT_PRIVATE_KEY = "privateKey";
    private static final String KEY_JWT_PUBLIC_KEY = "publicKey";
    private static final String KEY_JWT_SECRET_KEY = "secretKey";
    private static final String KEY_JWT_EXPIRE_TIME = "expireTime";
    private static final String KEY_JWT_START_TIME = "startTime";
    private static final String KEY_JWT_END_TIME = "endTime";

    @Getter
    @Setter
    private String key;

    @Getter
    @Setter
    private String algorithm;

    @Getter
    @Setter
    private String keyStore;

    @Getter
    @Setter
    private String privateKey;

    @Getter
    @Setter
    private String publicKey;

    @Getter
    @Setter
    private String secretKey;

    @Getter
    @Setter
    private long expireTime;

    @Getter
    @Setter
    private long startTime;

    @Getter
    @Setter
    private long endTime;

    @Getter
    private transient boolean valid;

    private final transient LazyObject<JWTAlgorithmContext> signatureContext = LazyObject.empty();

    private final transient LazyObject<JWTAlgorithmContext> verifyContext = LazyObject.empty();

    public JWTPolicy() {
    }

    public JWTPolicy(Map<String, String> map) {
        Option option = MapOption.of(map);
        this.key = option.getString(KEY_JWT_KEY, Authenticate.KEY_AUTH);
        this.algorithm = option.getString(KEY_JWT_ALGORITHM, DEFAULT_ALGORITHM);
        this.keyStore = option.getString(KEY_JWT_KEY_STORE, DEFAULT_KEY_STORE);
        this.privateKey = option.getString(KEY_JWT_PRIVATE_KEY);
        this.publicKey = option.getString(KEY_JWT_PUBLIC_KEY);
        this.secretKey = option.getString(KEY_JWT_SECRET_KEY);
        this.expireTime = option.getPositive(KEY_JWT_EXPIRE_TIME, DEFAULT_EXPIRE_TIME);
        this.expireTime = expireTime < FILE_MINUTE ? DEFAULT_EXPIRE_TIME : expireTime;
        this.startTime = option.getLong(KEY_JWT_START_TIME, 0L);
        this.endTime = option.getLong(KEY_JWT_END_TIME, Long.MAX_VALUE);
    }

    public JWTAlgorithmContext getSignatureContext(Supplier<JWTAlgorithmContext> supplier) {
        return signatureContext.get(supplier);
    }

    public JWTAlgorithmContext getVerifyContext(Supplier<JWTAlgorithmContext> supplier) {
        return verifyContext.get(supplier);
    }

    @Override
    public String getAuthScheme() {
        return "Bearer";
    }

    public String getKeyOrDefault(final String defaultKey) {
        return key == null || key.isEmpty() ? defaultKey : key;
    }

    public boolean isEffective(long time) {
        return valid && time >= startTime && time < endTime;
    }

    public JWTPolicy copy() {
        JWTPolicy policy = new JWTPolicy();
        policy.key = key;
        policy.algorithm = algorithm;
        policy.keyStore = keyStore;
        policy.privateKey = privateKey;
        policy.publicKey = publicKey;
        policy.secretKey = secretKey;
        policy.expireTime = expireTime;
        policy.startTime = startTime;
        policy.endTime = endTime;
        return policy;
    }

    protected void cache() {
        endTime = endTime <= 0 ? Long.MAX_VALUE : endTime;
        valid = (secretKey != null && !secretKey.isEmpty())
                || (privateKey != null && !privateKey.isEmpty() && publicKey != null && !publicKey.isEmpty());
    }
}
