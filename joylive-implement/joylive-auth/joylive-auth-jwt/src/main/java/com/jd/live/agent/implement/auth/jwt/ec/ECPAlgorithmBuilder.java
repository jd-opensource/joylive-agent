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
package com.jd.live.agent.implement.auth.jwt.ec;

import com.auth0.jwt.algorithms.Algorithm;
import com.jd.live.agent.governance.policy.service.auth.JWTPolicy;
import com.jd.live.agent.governance.security.KeyStore;
import com.jd.live.agent.implement.auth.jwt.AlgorithmBuilder;
import com.jd.live.agent.implement.auth.jwt.AlgorithmContext;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import static com.jd.live.agent.governance.security.KeyLoader.loadKey;

/**
 * Factory for creating EC algorithm instances using keys from a KeyStore.
 */
public abstract class ECPAlgorithmBuilder implements AlgorithmBuilder {

    @Override
    public Algorithm create(AlgorithmContext context) throws Exception {
        KeyStore keyStore = context.getKeyStore();
        JWTPolicy policy = context.getPolicy();
        byte[] publicDer = loadKey(keyStore.getPublicKey(policy.getPublicKey()));
        byte[] privateDer = loadKey(keyStore.getPrivateKey(policy.getPrivateKey()));
        ECPublicKey publicKey = (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(publicDer));
        ECPrivateKey privateKey = (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(privateDer));
        return doCreate(publicKey, privateKey);
    }

    protected abstract Algorithm doCreate(ECPublicKey publicKey, ECPrivateKey privateKey);
}
