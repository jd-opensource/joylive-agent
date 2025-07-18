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
package com.jd.live.agent.governance.security.cipher.simple;

import com.jd.live.agent.governance.config.CipherConfig;
import com.jd.live.agent.governance.exception.CipherException;
import com.jd.live.agent.governance.security.CipherAlgorithm;
import com.jd.live.agent.governance.security.CipherAlgorithmContext;
import com.jd.live.agent.governance.security.generator.RandomGenerator;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;

/**
 * Basic PBE (Password-Based Encryption) cipher algorithm implementation.
 * Provides simplified encryption/decryption operations with password protection.
 */
public class SimplePBECipherAlgorithm implements CipherAlgorithm {

    private final String algorithm;

    private final char[] password;

    private final int iterations;

    public SimplePBECipherAlgorithm(CipherAlgorithmContext ctx) {
        CipherConfig config = ctx.getConfig();
        this.algorithm = isEmpty(config.getAlgorithm()) ? CIPHER_DEFAULT_ALGORITHM : config.getAlgorithm();
        this.password = config.getPassword().toCharArray();
        this.iterations = config.getIterations();
    }

    @Override
    public byte[] encrypt(byte[] encoded) throws CipherException {
        try {
            // create Key
            final SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            // fixed random salt algorithm
            byte[] salt = RandomGenerator.INSTANCE.create(8);
            final PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations);
            SecretKey key = factory.generateSecret(keySpec);

            // Build cipher.
            final Cipher cipherEncrypt = Cipher.getInstance(algorithm);
            cipherEncrypt.init(Cipher.ENCRYPT_MODE, key);

            // Save parameters
            byte[] params = cipherEncrypt.getParameters().getEncoded();

            // Encrypted message
            byte[] encryptedMessage = cipherEncrypt.doFinal(encoded);

            return ByteBuffer
                    .allocate(1 + params.length + encryptedMessage.length)
                    .put((byte) params.length)
                    .put(params)
                    .put(encryptedMessage)
                    .array();
        } catch (CipherException e) {
            throw e;
        } catch (Throwable e) {
            throw new CipherException(e.getMessage(), e);
        }
    }

    @Override
    public byte[] decrypt(byte[] encryptedMessage) throws CipherException {
        try {
            int paramsLength = Byte.toUnsignedInt(encryptedMessage[0]);
            int messageLength = encryptedMessage.length - paramsLength - 1;
            byte[] params = new byte[paramsLength];
            byte[] message = new byte[messageLength];
            System.arraycopy(encryptedMessage, 1, params, 0, paramsLength);
            System.arraycopy(encryptedMessage, paramsLength + 1, message, 0, messageLength);

            // create Key
            final SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            final PBEKeySpec keySpec = new PBEKeySpec(password);
            SecretKey key = factory.generateSecret(keySpec);

            // Build parameters
            AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance(algorithm);
            algorithmParameters.init(params);

            // Build Cipher
            final Cipher cipherDecrypt = Cipher.getInstance(algorithm);
            cipherDecrypt.init(Cipher.DECRYPT_MODE, key, algorithmParameters);

            return cipherDecrypt.doFinal(message);
        } catch (CipherException e) {
            throw e;
        } catch (Throwable e) {
            throw new CipherException(e.getMessage(), e);
        }
    }

}
