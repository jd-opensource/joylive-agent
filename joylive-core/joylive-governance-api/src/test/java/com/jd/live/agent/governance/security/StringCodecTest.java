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
package com.jd.live.agent.governance.security;

import com.jd.live.agent.governance.security.base64.Base64StringCodec;
import com.jd.live.agent.governance.security.hex.HexStringCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class StringCodecTest {

    @Test
    void testBase64() {
        test(new Base64StringCodec());
    }

    @Test
    void testHex() {
        test(new HexStringCodec());
    }

    private void test(StringCodec codec) {
        byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
        String encoded = codec.encode(bytes);
        byte[] decoded = codec.decode(encoded);
        Assertions.assertArrayEquals(bytes, decoded);
    }
}
