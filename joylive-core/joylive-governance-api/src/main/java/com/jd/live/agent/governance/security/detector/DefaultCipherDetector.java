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
package com.jd.live.agent.governance.security.detector;

import com.jd.live.agent.governance.config.CipherConfig;
import com.jd.live.agent.governance.security.CipherDetector;

import java.util.function.Predicate;

/**
 * Default implementation of {@link CipherDetector} that uses prefix/suffix markers.
 * <p>Detects encryption by checking for wrapping markers and removes them during unwrap.
 */
public class DefaultCipherDetector implements CipherDetector {

    private final String prefix;

    private final String suffix;

    private final Predicate<String> predicate;

    public DefaultCipherDetector(CipherConfig config) {
        this.prefix = config.getPrefix();
        this.suffix = config.getSuffix();
        int prefixLen = prefix.length();
        int suffixLen = suffix.length();
        this.predicate = prefixLen + suffixLen == 0
                ? null
                : (prefixLen == 0
                ? s -> s.endsWith(suffix)
                : (suffixLen == 0
                ? s -> s.startsWith(prefix)
                : s -> s.startsWith(prefix) && s.endsWith(prefix)));
    }

    @Override
    public boolean isEncrypted(String key, String data) {
        return data != null && !data.isEmpty() && predicate != null && predicate.test(data);
    }

    @Override
    public String unwrap(String encoded) {
        return encoded.substring(prefix.length(), encoded.length() - suffix.length());
    }
}
