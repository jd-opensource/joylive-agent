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
package com.jd.live.agent.core.security.codec;

import com.jd.live.agent.core.exception.CipherException;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.security.StringCodec;

@Extension("hex")
public class HexStringCodec implements StringCodec {

    private static final char[] hexDigits =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    @Override
    public String encode(byte[] data) throws CipherException {
        if (data == null) {
            return null;
        }
        final StringBuilder builder = new StringBuilder(data.length * 2);
        int curByte;
        for (byte datum : data) {
            curByte = datum & 0xff;
            builder.append(hexDigits[curByte >> 4]);
            builder.append(hexDigits[curByte & 0xf]);
        }
        return builder.toString();
    }

    @Override
    public byte[] decode(String data) throws CipherException {
        if (data == null) {
            return null;
        }
        int len = data.length();
        if ((len & 1) != 0) {
            throw new CipherException("Hex string length must be even");
        }

        byte[] result = new byte[len >>> 1]; // len / 2
        int first;
        int second;
        for (int i = 0; i < len; i += 2) {
            first = hexToBin(data.charAt(i));
            second = hexToBin(data.charAt(i + 1));
            result[i >>> 1] = (byte) ((first << 4) | second);
        }
        return result;
    }

    /**
     * Fast hex char to binary value converter (0-9, a-f, A-F).
     *
     * @param ch hex character to convert
     * @return integer value (0-15)
     * @throws CipherException if input is not a valid hex char
     */
    private static int hexToBin(char ch) {
        int val = ch - '0';
        if (val < 0) {
            throw new CipherException("Invalid hex character: " + ch);
        }
        if (val < 10) { // 0-9
            return val;
        }
        val = (ch | 0x20) - 'a' + 10; // lower case a-f/A-F
        if (val < 10 || val > 15) {
            throw new CipherException("Invalid hex character: " + ch);
        }
        return val;
    }
}
