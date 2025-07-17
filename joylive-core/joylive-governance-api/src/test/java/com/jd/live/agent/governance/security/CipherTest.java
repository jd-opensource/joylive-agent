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

import com.jd.live.agent.governance.config.CipherConfig;
import com.jd.live.agent.governance.security.cipher.simple.SimplePBECipherAlgorithmFactory;
import com.jd.live.agent.governance.security.codec.Base64StringCodec;
import com.jd.live.agent.governance.security.codec.HexStringCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CipherTest {

    @Test
    void testBase64Codec() {
        testCodec(new Base64StringCodec());
    }

    @Test
    void testHexCodec() {
        testCodec(new HexStringCodec());
    }

    @Test
    void testSimpleCipher() throws Exception {

        Map<String, CipherAlgorithmFactory> factories = new HashMap<>();
        factories.put("SimplePBE",new SimplePBECipherAlgorithmFactory());

        DefaultCipherFactory factory = new DefaultCipherFactory(factories);
        CipherConfig config = new CipherConfig();
        config.setCipher("SimplePBE");
        config.setPassword("test");
        config.setAlgorithm(CipherAlgorithm.CIPHER_DEFAULT_ALGORITHM);
        Cipher cipher = factory.create(config);
        String source = "abcde";
        String encoded = cipher.encrypt(source);
        String decoded = cipher.decrypt(encoded);
        Assertions.assertEquals(source, decoded);
    }

    private void testCodec(StringCodec codec) {
        byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
        String encoded = codec.encode(bytes);
        byte[] decoded = codec.decode(encoded);
        Assertions.assertArrayEquals(bytes, decoded);
    }
}
