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
package com.jd.live.agent.implement.cipher.jasypt;

import com.jd.live.agent.governance.security.CipherAlgorithm;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;

public class JasyptPBECipherAlgorithm implements CipherAlgorithm {

    private final JasyptConfig config;

    public JasyptPBECipherAlgorithm(JasyptConfig config) {
        this.config = config;
    }

    @Override
    public byte[] encrypt(byte[] encoded) throws Exception {
        // create Key
        final SecretKeyFactory factory = SecretKeyFactory.getInstance(config.getAlgorithm());
        byte[] salt = config.getSaltGenerator().generateSalt(8);
        final PBEKeySpec keySpec = new PBEKeySpec(config.getPassword().toCharArray(), salt, config.getIterations());
        SecretKey key = factory.generateSecret(keySpec);

        // Build cipher.
        final Cipher cipherEncrypt = Cipher.getInstance(config.getAlgorithm());
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
    }

    @Override
    public byte[] decrypt(byte[] encryptedMessage) throws Exception {
        int paramsLength = Byte.toUnsignedInt(encryptedMessage[0]);
        int messageLength = encryptedMessage.length - paramsLength - 1;
        byte[] params = new byte[paramsLength];
        byte[] message = new byte[messageLength];
        System.arraycopy(encryptedMessage, 1, params, 0, paramsLength);
        System.arraycopy(encryptedMessage, paramsLength + 1, message, 0, messageLength);

        // create Key
        final SecretKeyFactory factory = SecretKeyFactory.getInstance(config.getAlgorithm());
        final PBEKeySpec keySpec = new PBEKeySpec(config.getPassword().toCharArray());
        SecretKey key = factory.generateSecret(keySpec);

        // Build parameters
        AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance(config.getAlgorithm());
        algorithmParameters.init(params);

        // Build Cipher
        final Cipher cipherDecrypt = Cipher.getInstance(config.getAlgorithm());
        cipherDecrypt.init(Cipher.DECRYPT_MODE, key, algorithmParameters);

        return cipherDecrypt.doFinal(message);
    }
}
