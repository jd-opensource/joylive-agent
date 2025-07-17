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

import java.text.Normalizer;

/**
 * Utility for normalizing text to Unicode NFC form.
 */
public class CipherNormalizer {

    /**
     * Normalizes the input string to NFC form.
     *
     * @param message the text to normalize (may be null)
     * @return normalized string in NFC form, or null if input was null
     */
    public static String normalizeToNfc(final String message) {
        return Normalizer.normalize(message, Normalizer.Form.NFC);
    }
}

