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

import com.jd.live.agent.governance.exception.CipherException;
import com.jd.live.agent.governance.security.CipherGenerator;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class RandomGenerator implements CipherGenerator {

    public static final CipherGenerator INSTANCE = new RandomGenerator();

    private static final String DEFAULT_SECURE_RANDOM_ALGORITHM = "SHA1PRNG";

    private final String randomAlgorithm;

    private final int size;

    private final Object mutex = new Object();

    private volatile SecureRandom random;

    public RandomGenerator() {
        this(DEFAULT_SECURE_RANDOM_ALGORITHM, 8);
    }

    public RandomGenerator(String randomAlgorithm, int size) {
        this.randomAlgorithm = randomAlgorithm == null ? DEFAULT_SECURE_RANDOM_ALGORITHM : randomAlgorithm;
        this.size = size;
    }

    @Override
    public byte[] create(int size) throws CipherException {
        try {
            final byte[] salt = new byte[size];
            getRandom().nextBytes(salt);
            return salt;
        } catch (NoSuchAlgorithmException e) {
            throw new CipherException(e.getMessage(), e);
        }
    }

    @Override
    public boolean withResult() {
        return true;
    }

    @Override
    public int size() {
        return size;
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
