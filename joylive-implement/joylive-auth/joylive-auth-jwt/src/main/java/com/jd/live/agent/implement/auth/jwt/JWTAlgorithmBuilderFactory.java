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
public class JWTAlgorithmBuilderFactory {

    private static final Map<String, JWTAlgorithmBuilder> factories = new HashMap<>();

    static {
        register(new RSA256AlgorithmBuilder());
        register(new RSA384AlgorithmBuilder());
        register(new RSA512AlgorithmBuilder());

        register(new EC256AlgorithmBuilder());
        register(new EC384AlgorithmBuilder());
        register(new EC512AlgorithmBuilder());

        register(new HAMC256AlgorithmBuilder());
        register(new HAMC384AlgorithmBuilder());
        register(new HAMC512AlgorithmBuilder());
    }

    /**
     * Adds the builder to the factories map using all its names as keys.
     *
     * @param builder the algorithm builder to register
     */
    private static void register(JWTAlgorithmBuilder builder) {
        for (String name : builder.getNames()) {
            factories.put(name, builder);
        }
    }

    /**
     * Gets the AlgorithmBuilder for the specified algorithm name.
     *
     * @param algorithm the name of the algorithm (e.g., "RSA256", "HMAC384")
     * @return the corresponding AlgorithmBuilder, or null if not found
     */
    public static JWTAlgorithmBuilder getBuilder(String algorithm) {
        return factories.get(algorithm);
    }

}
