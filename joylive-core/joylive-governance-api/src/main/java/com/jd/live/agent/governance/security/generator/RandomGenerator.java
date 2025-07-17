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
package com.jd.live.agent.governance.security.generator;

import com.jd.live.agent.governance.security.CipherGenerator;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class RandomGenerator implements CipherGenerator {

    public static final CipherGenerator INSTANCE = new RandomGenerator();

    private static final String DEFAULT_SECURE_RANDOM_ALGORITHM = "SHA1PRNG";

    private final String randomAlgorithm;

    private final Object mutex = new Object();

    private volatile SecureRandom random;

    public RandomGenerator() {
        this(DEFAULT_SECURE_RANDOM_ALGORITHM);
    }

    public RandomGenerator(String randomAlgorithm) {
        this.randomAlgorithm = randomAlgorithm == null ? DEFAULT_SECURE_RANDOM_ALGORITHM : randomAlgorithm;
    }

    @Override
    public byte[] create(int size) throws Exception {
        final byte[] salt = new byte[size];
        getRandom().nextBytes(salt);
        return salt;
    }

    @Override
    public boolean withResult() {
        return true;
    }

    private SecureRandom getRandom() throws NoSuchAlgorithmException {
        SecureRandom rd = random;
        if (rd == null) {
            synchronized (mutex) {
                rd = random;
                if (rd == null) {
                    rd = SecureRandom.getInstance(randomAlgorithm);
                    random = rd;
                }
            }
        }
        return rd;
    }
}
