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

import com.jd.live.agent.implement.auth.jwt.ec.EC256AlgorithmBuilder;
import com.jd.live.agent.implement.auth.jwt.ec.EC384AlgorithmBuilder;
import com.jd.live.agent.implement.auth.jwt.ec.EC512AlgorithmBuilder;
import com.jd.live.agent.implement.auth.jwt.hamc.HAMC256AlgorithmBuilder;
import com.jd.live.agent.implement.auth.jwt.hamc.HAMC384AlgorithmBuilder;
import com.jd.live.agent.implement.auth.jwt.hamc.HAMC512AlgorithmBuilder;
import com.jd.live.agent.implement.auth.jwt.rsa.RSA256AlgorithmBuilder;
import com.jd.live.agent.implement.auth.jwt.rsa.RSA384AlgorithmBuilder;
import com.jd.live.agent.implement.auth.jwt.rsa.RSA512AlgorithmBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class that provides AlgorithmBuilder instances based on algorithm names.
 * <p>
 * Maintains a static registry of supported algorithms and their corresponding builders.
 * </p>
 */
public class AlgorithmBuilderFactory {

    private static final Map<String, AlgorithmBuilder> factories = new HashMap<>();

    static {
        AlgorithmBuilder builder = new RSA256AlgorithmBuilder();
        factories.put("RSA256", builder);
        factories.put("SHA256withRSA", builder);
        builder = new RSA384AlgorithmBuilder();
        factories.put("RSA384", builder);
        factories.put("SHA384withRSA", builder);
        builder = new RSA512AlgorithmBuilder();
        factories.put("RSA512", builder);
        factories.put("SHA512withRSA", builder);

        builder = new EC256AlgorithmBuilder();
        factories.put("ECDSA256", builder);
        factories.put("SHA256withECDSA", builder);
        builder = new EC384AlgorithmBuilder();
        factories.put("ECDSA384", builder);
        factories.put("SHA384withECDSA", builder);
        builder = new EC512AlgorithmBuilder();
        factories.put("ECDSA512", builder);
        factories.put("SHA512withECDSA", builder);

        builder = new HAMC256AlgorithmBuilder();
        factories.put("HMAC256", builder);
        factories.put("HmacSHA256", builder);
        builder = new HAMC384AlgorithmBuilder();
        factories.put("HMAC384", builder);
        factories.put("HmacSHA384", builder);
        builder = new HAMC512AlgorithmBuilder();
        factories.put("HMAC512", builder);
        factories.put("HmacSHA512", builder);
    }

    /**
     * Gets the AlgorithmBuilder for the specified algorithm name.
     *
     * @param algorithm the name of the algorithm (e.g., "RSA256", "HMAC384")
     * @return the corresponding AlgorithmBuilder, or null if not found
     */
    public static AlgorithmBuilder getBuilder(String algorithm) {
        return factories.get(algorithm);
    }

}
