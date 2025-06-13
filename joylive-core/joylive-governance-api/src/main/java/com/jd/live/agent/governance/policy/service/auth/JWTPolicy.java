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

import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.governance.invoke.auth.Authenticate;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

/**
 * JWT policy
 *
 * @since 1.8.0
 */
@Getter
@Setter
public class JWTPolicy implements Serializable {

    private static final long DEFAULT_EXPIRE_TIME = 5 * 60 * 1000L;
    private static final String DEFAULT_KEY_STORE = "file";
    private static final String DEFAULT_ALGORITHM = "HMAC256";

    private static final String KEY_JWT_KEY = "jwt.key";
    private static final String KEY_JWT_ALGORITHM = "jwt.algorithm";
    private static final String KEY_JWT_KEY_STORE = "jwt.keyStore";
    private static final String KEY_JWT_ISSUER = "jwt.issuer";
    private static final String KEY_JWT_AUDIENCE = "jwt.audience";
    private static final String KEY_JWT_PRIVATE_KEY = "jwt.privateKey";
    private static final String KEY_JWT_PUBLIC_KEY = "jwt.publicKey";
    private static final String KEY_JWT_SECRET = "jwt.secret";
    private static final String KEY_EXPIRE_TIME = "jwt.expireTime";

    private String key;

    private String algorithm = DEFAULT_ALGORITHM;

    private String keyStore = DEFAULT_KEY_STORE;

    private String privateKey;

    private String publicKey;

    private String secret;

    private String issuer;

    private String audience;

    private long expireTime = DEFAULT_EXPIRE_TIME;

    public JWTPolicy() {
    }

    public JWTPolicy(Map<String, String> map) {
        Option option = MapOption.of(map);
        this.key = option.getString(KEY_JWT_KEY, Authenticate.KEY_AUTH);
        this.algorithm = option.getString(KEY_JWT_ALGORITHM, DEFAULT_ALGORITHM);
        this.keyStore = option.getString(KEY_JWT_KEY_STORE, DEFAULT_KEY_STORE);
        this.issuer = option.getString(KEY_JWT_ISSUER);
        this.audience = option.getString(KEY_JWT_AUDIENCE);
        this.privateKey = option.getString(KEY_JWT_PRIVATE_KEY);
        this.publicKey = option.getString(KEY_JWT_PUBLIC_KEY);
        this.secret = option.getString(KEY_JWT_SECRET);
        this.expireTime = option.getPositive(KEY_EXPIRE_TIME, DEFAULT_EXPIRE_TIME);
    }
}
