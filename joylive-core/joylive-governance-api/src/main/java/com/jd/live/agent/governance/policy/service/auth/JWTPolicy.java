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

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * JWT policy
 *
 * @since 1.8.0
 */
@Getter
@Setter
public class JWTPolicy implements Serializable {

    public static final long DEFAULT_EXPIRE_TIME = 5 * 60 * 1000L;

    private String key;

    private String algorithm;

    private String keyStore = "local";

    private String privateKey;

    private String publicKey;

    private String secret;

    private String issuer;

    private String audience;

    private long expireTime = DEFAULT_EXPIRE_TIME;

}
