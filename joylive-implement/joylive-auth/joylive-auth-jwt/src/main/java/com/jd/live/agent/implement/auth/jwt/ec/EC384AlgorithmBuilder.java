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

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

/**
 * Factory for creating ECDSA384 algorithm instances using keys from a KeyStore.
 */
public class EC384AlgorithmBuilder extends ECPAlgorithmBuilder {

    @Override
    protected Algorithm doCreate(ECPublicKey publicKey, ECPrivateKey privateKey) {
        return Algorithm.ECDSA384(publicKey, privateKey);
    }

    @Override
    public String[] getNames() {
        return new String[]{"ES384", "ECDSA384", "SHA384withECDSA"};
    }
}
