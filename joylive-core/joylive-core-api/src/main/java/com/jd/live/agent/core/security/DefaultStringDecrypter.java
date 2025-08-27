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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.exception.CipherException;

import java.util.function.Consumer;

public class DefaultStringDecrypter implements StringDecrypter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultStringDecrypter.class);

    private final CipherDetector detector;

    private final Cipher cipher;

    public DefaultStringDecrypter(CipherDetector detector, Cipher cipher) {
        this.detector = detector;
        this.cipher = cipher;
    }

    @Override
    public String tryDecrypt(String value) {
        try {
            return decrypt(value);
        } catch (CipherException e) {
            logger.error("Error occurs while decrypting {}", value);
            return value;
        }
    }

    @Override
    public void tryDecrypt(String value, Consumer<String> onSuccess) {
        if (cipher != null && detector.isEncrypted(value)) {
            try {
                onSuccess.accept(cipher.decrypt(detector.unwrap(value)));
            } catch (CipherException ignore) {
                logger.error("Error occurs while decrypting {}", value);
            }
        }
    }

    @Override
    public String decrypt(String value) throws CipherException {
        if (cipher != null && detector.isEncrypted(value)) {
            return cipher.decrypt(detector.unwrap(value));
        }
        return value;
    }
}
