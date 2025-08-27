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

import com.jd.live.agent.core.config.CipherConfig;
import com.jd.live.agent.core.exception.CipherException;
import com.jd.live.agent.core.security.cipher.jasypt.StandardPBECipherAlgorithmFactory;
import com.jd.live.agent.core.security.cipher.simple.SimplePBECipherAlgorithmFactory;
import com.jd.live.agent.core.security.codec.Base64StringCodec;
import com.jd.live.agent.core.security.codec.HexStringCodec;
import com.jd.live.agent.core.security.generator.Base64GeneratorFactory;
import com.jd.live.agent.core.security.generator.EmptyGeneratorFactory;
import com.jd.live.agent.core.security.generator.RandomGeneratorFactory;
import com.jd.live.agent.core.security.generator.StringGeneratorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class CipherTest {

    private static final DefaultCipherFactory factory = DefaultCipherFactory.builder()
            .add("SimplePBE", new SimplePBECipherAlgorithmFactory())
            .add("StandardPBE", new StandardPBECipherAlgorithmFactory())
            .add("base64", new Base64StringCodec())
            .add("hex", new HexStringCodec())
            .add("random", new RandomGeneratorFactory())
            .add("empty", new EmptyGeneratorFactory())
            .add("zero", new EmptyGeneratorFactory())
            .add("base64", new Base64GeneratorFactory())
            .add("string", new StringGeneratorFactory())
            .build();

    @Test
    void testBase64Codec() {
        testCodec(new Base64StringCodec());
    }

    @Test
    void testHexCodec() {
        testCodec(new HexStringCodec());
    }

    @Test
    void testSimplePBE() throws CipherException {
        testCipher(CipherConfig.builder()
                .cipher("SimplePBE")
                .password("test")
                .algorithm(CipherAlgorithm.CIPHER_DEFAULT_ALGORITHM)
                .build()
        );
    }

    @Test
    void testStandardPBE() throws CipherException {
        // with salt(Random)
        testCipher(CipherConfig.builder()
                .cipher("StandardPBE")
                .password("test")
                .algorithm(CipherAlgorithm.CIPHER_DEFAULT_ALGORITHM)
                .build()
        );
        // with salt(string) and iv(String)
        testCipher(CipherConfig.builder()
                .cipher("StandardPBE")
                .password("test")
                .saltType("string")
                .salt("ssssssss")
                .ivType("string")
                .iv("aaaaaaaa")
                .codec("hex")
                .algorithm(CipherAlgorithm.CIPHER_DEFAULT_ALGORITHM)
                .build()
        );

        // with salt(empty) and iv(String)
        testCipher(CipherConfig.builder()
                .cipher("StandardPBE")
                .password("test")
                .saltType("empty")
                .ivType("string")
                .iv("a")
                .codec("hex")
                .algorithm(CipherAlgorithm.CIPHER_DEFAULT_ALGORITHM)
                .build()
        );

        // without salt(Empty) and iv(Empty)
        testCipher(CipherConfig.builder()
                .cipher("StandardPBE")
                .password("test")
                .saltType("empty")
                .ivType("empty")
                .algorithm(CipherAlgorithm.CIPHER_DEFAULT_ALGORITHM)
                .build()
        );

    }

    protected void testCipher(CipherConfig config) {
        Cipher cipher = factory.create(config);
        String source = "abcde";
        String encoded = cipher.encrypt(source);
        System.out.println(encoded);
        String decoded = cipher.decrypt(encoded);
        Assertions.assertEquals(source, decoded);
    }

    protected void testCodec(StringCodec codec) {
        byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
        String encoded = codec.encode(bytes);
        byte[] decoded = codec.decode(encoded);
        Assertions.assertArrayEquals(bytes, decoded);
    }
}
