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
package com.jd.live.agent.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A utility class for working with input and output streams.
 */
public class IOUtils {

    /**
     * Reads the contents of an input stream into a byte array.
     *
     * @param in The input stream to read from.
     * @return A byte array containing the contents of the input stream, or null if the input stream is null.
     * @throws IOException If an I/O error occurs while reading from the input stream.
     */
    public static byte[] read(InputStream in) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        return out.toByteArray();
    }

    /**
     * Copies data from an InputStream to an OutputStream.
     *
     * @param is the InputStream to copy from
     * @param os the OutputStream to copy to
     * @throws IOException if an error occurs during the copy
     */
    public static void copy(final InputStream is, final OutputStream os) throws IOException {
        if (is == null || os == null) {
            return;
        }
        byte[] buffer = new byte[1024 * 4];
        int c;
        while ((c = is.read(buffer, 0, buffer.length)) >= 0) {
            os.write(buffer, 0, c);
        }
    }
}
