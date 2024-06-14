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
package com.jd.live.agent.bootstrap.bytekit.advice;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * AdviceKey class provides static methods for generating unique keys for methods and constructors.
 */
public class AdviceKey {

    private static void update(CRC32 crc32, byte[] b) {
        crc32.update(b, 0, b.length);
    }

    /**
     * Generates a unique key for a given method based on its description, and the class loader.
     *
     * @param methodDesc  the description of the method
     * @param classLoader the class loader used to load the method's class
     * @return a unique string representing the method key
     */
    public static String getMethodKey(String methodDesc, ClassLoader classLoader) {
        CRC32 crc32 = new CRC32();
        update(crc32, methodDesc.getBytes(StandardCharsets.UTF_8));
        update(crc32, Integer.toString(System.identityHashCode(classLoader)).getBytes(StandardCharsets.UTF_8));
        return Long.toHexString(crc32.getValue());
    }

}
