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

import com.jd.live.agent.governance.security.KeyStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import static com.jd.live.agent.governance.security.KeyLoader.loadKey;

@Getter
@AllArgsConstructor
@Builder
public class JWTAlgorithmContext {
    private JWTAlgorithmRole role;
    private String algorithm;
    private String privateKey;
    private String publicKey;
    private String secretKey;
    private KeyStore keyStore;
    private String issuer;
    private String audience;
    private long expireTime;

    public byte[] loadPublicKey() throws Exception {
        return loadKey(keyStore.getPublicKey(publicKey));
    }

    public byte[] loadPrivateKey() throws Exception {
        return loadKey(keyStore.getPrivateKey(privateKey));
    }
}
