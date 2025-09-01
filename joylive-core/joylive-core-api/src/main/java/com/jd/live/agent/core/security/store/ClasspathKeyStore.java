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
package com.jd.live.agent.core.security.store;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.security.KeyStore;
import com.jd.live.agent.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * {@link KeyStore} implementation that loads keys from classpath resources or files.
 * <p>
 * First attempts to locate the key as a classpath resource, then falls back to
 * filesystem lookup if not found. Returns {@code null} if the key doesn't exist.
 *
 * @Extension Default implementation with order {@value KeyStore#ORDER_CLASSPATH}
 */
@Extension(value = "classpath", order = KeyStore.ORDER_CLASSPATH)
public class ClasspathKeyStore implements KeyStore {

    @Override
    public byte[] getPrivateKey(String keyId) throws IOException {
        return getKey(keyId);
    }

    @Override
    public byte[] getPublicKey(String keyId) throws IOException {
        return getKey(keyId);
    }

    /**
     * Loads key bytes as a classpath resource using the context classloader.
     *
     * @param keyId Classpath resource path (e.g. "keys/private.der")
     * @return Raw key bytes or {@code null} if resource not found
     * @throws IOException If the resource exists but cannot be read
     */
    private byte[] getKey(String keyId) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource(keyId);
        if (url != null) {
            try (InputStream in = url.openStream()) {
                return IOUtils.read(in);
            }
        }
        return null;
    }
}
