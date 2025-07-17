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

import java.nio.charset.StandardCharsets;

public class StringGenerator implements CipherGenerator {

    protected final byte[] salt;

    public StringGenerator(String salt) {
        this.salt = getBytes(salt);
    }

    @Override
    public byte[] create(int size) throws Exception {
        byte[] result = new byte[size];
        System.arraycopy(salt, 0, result, 0, Math.min(salt.length, size));
        return result;
    }

    protected byte[] getBytes(String salt) {
        return salt.getBytes(StandardCharsets.UTF_8);
    }
}
