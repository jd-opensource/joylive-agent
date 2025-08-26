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
package com.jd.live.agent.core.security.generator;

import com.jd.live.agent.core.exception.CipherException;
import com.jd.live.agent.core.security.CipherGenerator;

public class ByteGenerator implements CipherGenerator {

    protected final int size;
    protected final byte[] source;
    protected volatile byte[] salt;
    protected final Object mutex = new Object();

    public ByteGenerator(byte[] source, int size) {
        this.source = source;
        this.size = size;
    }

    @Override
    public byte[] create(int size) throws CipherException {
        // The size maybe set to the algorithm's block size (if it is a block algorithm).
        byte[] result = salt;
        if (result == null || size != result.length) {
            synchronized (mutex) {
                result = salt;
                if (result == null) {
                    result = setupSalt(source, size);
                    salt = result;
                } else if (size != result.length) {
                    salt = copy(source, size);
                }
            }
        }
        return result;
    }

    @Override
    public boolean isFixed() {
        return true;
    }

    @Override
    public int size() {
        return size;
    }

    protected byte[] setupSalt(byte[] bytes, int size) {
        return bytes.length == size ? bytes : copy(bytes, size);
    }

    protected byte[] copy(byte[] bytes, int size) {
        byte[] newBytes = new byte[size];
        System.arraycopy(bytes, 0, newBytes, 0, Math.min(bytes.length, size));
        return newBytes;
    }
}
