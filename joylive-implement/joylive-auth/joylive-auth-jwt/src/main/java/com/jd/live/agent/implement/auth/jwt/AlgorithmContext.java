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

import com.jd.live.agent.governance.security.KeyStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

import static com.jd.live.agent.governance.security.KeyLoader.loadKey;

@Getter
@AllArgsConstructor
@Builder
public class AlgorithmContext {
    private AlgorithmRole role;
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AlgorithmContext)) return false;
        AlgorithmContext target = (AlgorithmContext) o;
        return role == target.role
                && expireTime == target.expireTime
                && Objects.equals(algorithm, target.algorithm)
                && Objects.equals(privateKey, target.privateKey)
                && Objects.equals(publicKey, target.publicKey)
                && Objects.equals(keyStore, target.keyStore)
                && Objects.equals(issuer, target.issuer)
                && Objects.equals(audience, target.audience);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, algorithm, privateKey, publicKey, keyStore, issuer, audience, expireTime);
    }
}
