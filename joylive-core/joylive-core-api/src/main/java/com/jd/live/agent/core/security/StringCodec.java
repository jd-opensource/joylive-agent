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
package com.jd.live.agent.core.security;

import com.jd.live.agent.core.exception.CipherException;
import com.jd.live.agent.core.extension.annotation.Extensible;

/**
 * Bidirectional converter between strings and byte arrays.
 */
@Extensible("StringCodec")
public interface StringCodec {
    /**
     * Encodes binary data into a string representation.
     *
     * @param data raw byte array to encode (non-null)
     * @return encoded string
     */
    String encode(byte[] data) throws CipherException;

    /**
     * Decodes a string back into its original byte array.
     *
     * @param data encoded string to decode (non-null)
     * @return original byte array
     */
    byte[] decode(String data) throws CipherException;
}
