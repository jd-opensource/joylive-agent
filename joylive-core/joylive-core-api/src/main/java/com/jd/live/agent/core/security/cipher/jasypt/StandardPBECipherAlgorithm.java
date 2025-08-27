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
package com.jd.live.agent.core.security.cipher.jasypt;

import com.jd.live.agent.core.config.CipherConfig;
import com.jd.live.agent.core.exception.CipherException;
import com.jd.live.agent.core.security.CipherAlgorithm;
import com.jd.live.agent.core.security.CipherAlgorithmContext;
import com.jd.live.agent.core.security.CipherGenerator;
import com.jd.live.agent.core.security.CipherNormalizer;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.spec.AlgorithmParameterSpec;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;

/**
 * Basic PBE (Password-Based Encryption) cipher algorithm implementation.
 * Provides simplified encryption/decryption operations with password protection.
 */
public class StandardPBECipherAlgorithm implements CipherAlgorithm {

    private final String algorithm;
    private final String provider;
    private final char[] password;
    private final int iterations;
    private final CipherGenerator salt;
    private final int saltSize;
    private final CipherGenerator iv;
    private final int ivSize;
    private final boolean withSalt;
    private final boolean withIV;

    public StandardPBECipherAlgorithm(CipherAlgorithmContext ctx) {
        CipherConfig config = ctx.getConfig();
        this.algorithm = isEmpty(config.getAlgorithm()) ? CIPHER_DEFAULT_ALGORITHM : config.getAlgorithm();
        this.provider = config.getProvider();
        this.password = CipherNormalizer.normalizeToNfc(config.getPassword()).toCharArray();
        this.iterations = config.getIterations();
        this.salt = ctx.getSalt();
        // only random generator
        this.withSalt = salt.withResult();
        this.saltSize = config.getSaltSize();
        this.iv = ctx.getIv();
        this.ivSize = config.getIvSize();
        // only random generator
        this.withIV = iv.withResult();
    }

    @Override
    public byte[] encrypt(byte[] data) throws CipherException {
        try {
            int saltSize = this.saltSize;
            int ivSize = this.ivSize;
            Cipher cipher = createCipher();
            // The salt size and the IV size for the chosen algorithm are set to be equal
            // to the algorithm's block size (if it is a block algorithm).
            final int blockSize = cipher.getBlockSize();
            if (blockSize > 0) {
                saltSize = blockSize;
                ivSize = blockSize;
            }

            // compatible with jasypt StandardPBEByteEncryptor
            byte[] saltBytes = salt.create(saltSize);
            byte[] ivBytes = iv.create(ivSize);
            setupCipher(cipher, saltBytes, ivBytes, Cipher.ENCRYPT_MODE);

            byte[] encoded = cipher.doFinal(data);
            return append(encoded, saltBytes, saltSize, ivBytes, ivSize);
        } catch (CipherException e) {
            throw e;
        } catch (Throwable e) {
            throw new CipherException(e.getMessage(), e);
        }
    }

    @Override
    public byte[] decrypt(byte[] data) throws CipherException {
        try {
            int saltSize = this.saltSize;
            int ivSize = this.ivSize;
            Cipher cipher = createCipher();
            // The salt size and the IV size for the chosen algorithm are set to be equal
            // to the algorithm's block size (if it is a block algorithm).
            final int blockSize = cipher.getBlockSize();
            if (blockSize > 0) {
                saltSize = blockSize;
                ivSize = blockSize;
            }
            byte[] saltBytes;
            byte[] ivBytes;
            int offset = 0;
            if (withSalt && withIV) {
                offset = saltSize + ivSize;
                saltBytes = new byte[saltSize];
                System.arraycopy(data, 0, saltBytes, 0, saltSize);
                ivBytes = new byte[ivSize];
                System.arraycopy(data, saltSize, ivBytes, 0, ivSize);
            } else if (withSalt) {
                offset = saltSize;
                saltBytes = new byte[saltSize];
                System.arraycopy(data, 0, saltBytes, 0, saltSize);
                ivBytes = iv.create(ivSize);
            } else if (withIV) {
                offset = ivSize;
                saltBytes = salt.create(saltSize);
                ivBytes = new byte[ivSize];
                System.arraycopy(data, 0, ivBytes, 0, ivSize);
            } else {
                saltBytes = salt.create(saltSize);
                ivBytes = iv.create(ivSize);
            }

            setupCipher(cipher, saltBytes, ivBytes, Cipher.DECRYPT_MODE);
            return cipher.doFinal(data, offset, data.length - offset);
        } catch (CipherException e) {
            throw e;
        } catch (Throwable e) {
            throw new CipherException(e.getMessage(), e);
        }
    }

    /**
     * Initializes cipher with PBE parameters (salt, IV, and password).
     *
     * @param cipher      cipher to initialize
     * @param saltBytes   salt bytes (non-null if used)
     * @param ivBytes     IV bytes (non-null if used)
     * @param encryptMode Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
     * @throws Exception if cipher initialization fails
     */
    private void setupCipher(Cipher cipher, byte[] saltBytes, byte[] ivBytes, int encryptMode) throws Exception {
        // TODO cache to improve performance in some case
        SecretKeyFactory factory = isEmpty(provider)
                ? SecretKeyFactory.getInstance(algorithm)
                : SecretKeyFactory.getInstance(algorithm, provider);

        SecretKey key = factory.generateSecret(new PBEKeySpec(password));

        AlgorithmParameterSpec paramSpec = ivBytes == null || ivBytes.length == 0 ? null : new IvParameterSpec(ivBytes);
        PBEParameterSpec parameterSpec = new PBEParameterSpec(saltBytes, iterations, paramSpec);

        cipher.init(encryptMode, key, parameterSpec);
    }

    /**
     * Creates new cipher instance with configured algorithm/provider.
     *
     * @return initialized cipher
     * @throws Exception if cipher creation fails
     */
    private Cipher createCipher() throws Exception {
        return isEmpty(provider) ? Cipher.getInstance(algorithm) : Cipher.getInstance(algorithm, provider);
    }

    /**
     * Combines salt, IV and encrypted data into single byte array according to configuration.
     *
     * @param encoded   encrypted data
     * @param saltBytes salt (nullable if not used)
     * @param ivBytes   IV (nullable if not used)
     * @return combined byte array in format: [salt?][IV?][data]
     */
    private byte[] append(byte[] encoded, byte[] saltBytes, int saltSize, byte[] ivBytes, int ivSize) {
        if (withSalt && withIV) {
            byte[] result = new byte[saltSize + ivSize + encoded.length];
            System.arraycopy(saltBytes, 0, result, 0, saltSize);
            System.arraycopy(ivBytes, 0, result, saltSize, ivSize);
            System.arraycopy(encoded, 0, result, ivSize + saltSize, encoded.length);
            return result;
        } else if (withSalt) {
            byte[] result = new byte[saltSize + encoded.length];
            System.arraycopy(saltBytes, 0, result, 0, saltSize);
            System.arraycopy(encoded, 0, result, saltSize, encoded.length);
            return result;
        } else if (withIV) {
            byte[] result = new byte[ivSize + encoded.length];
            System.arraycopy(ivBytes, 0, result, 0, ivSize);
            System.arraycopy(encoded, 0, result, ivSize, encoded.length);
            return result;
        }
        return encoded;
    }

}
