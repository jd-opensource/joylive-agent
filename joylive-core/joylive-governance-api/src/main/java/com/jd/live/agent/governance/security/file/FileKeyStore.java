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
package com.jd.live.agent.governance.security.file;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.security.KeyStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A {@link KeyStore} implementation that loads cryptographic keys from files.
 * <p>
 * Each key is stored as a separate file, where the {@code keyId} parameter
 * represents the file path. Supports raw binary (DER) or Base64-encoded (PEM) keys.
 *
 * @implNote Returns {@code null} if the key file does not exist
 * @throws IOException If the key file cannot be read
 */
@Extension(value = KeyStore.TYPE_FILE, order = KeyStore.ORDER_FILE)
public class FileKeyStore implements KeyStore {

    @Override
    public byte[] getPrivateKey(String keyId) throws IOException {
        return getKey(keyId);
    }

    @Override
    public byte[] getPublicKey(String keyId) throws IOException {
        return getKey(keyId);
    }

    /**
     * Reads key bytes from filesystem.
     *
     * @param keyId Filesystem path (absolute or relative)
     * @return Raw key bytes or {@code null} if file doesn't exist
     * @throws IOException If file exists but cannot be read
     */
    private byte[] getKey(String keyId) throws IOException {
        Path path = Paths.get(keyId);
        if (path.toFile().exists()) {
            return Files.readAllBytes(path);
        }
        return null;
    }
}
