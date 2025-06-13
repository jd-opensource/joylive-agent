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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * Utility class for loading cryptographic keys from various formats.
 * Supports both raw binary and PEM-encoded key formats.
 */
public class KeyLoader {

    // PEM format
    private static final byte[] PEM_PREFIX = "-----BEGIN".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PEM_SUFFIX = "-----END".getBytes(StandardCharsets.UTF_8);
    private static final byte NEW_LINE = '\n';

    /**
     * Loads a cryptographic key from either raw binary or PEM-encoded data.
     * Automatically detects the format and processes accordingly.
     *
     * @param data the key data in either raw binary or PEM format
     * @return decoded key bytes (raw binary format)
     * @throws IllegalArgumentException if the PEM data is malformed
     */
    public static byte[] loadKey(byte[] data) {
        return isPEMFormat(data) ? decodePEM(data) : data;
    }

    /**
     * Checks if the data is in PEM format by verifying the standard PEM prefix.
     *
     * @param data the byte array to check
     * @return true if the data starts with PEM prefix ("-----BEGIN"), false otherwise
     */
    private static boolean isPEMFormat(byte[] data) {
        if (data.length < PEM_PREFIX.length) return false;
        for (int i = 0; i < PEM_PREFIX.length; i++) {
            if (data[i] != PEM_PREFIX[i]) return false;
        }
        return true;
    }

    /**
     * Decodes PEM file content into DER format by extracting and decoding Base64 payload.
     *
     * @param pemData the PEM-encoded data (including headers/footers)
     * @return decoded binary DER data
     * @throws IllegalArgumentException if invalid PEM format or Base64 data
     */
    private static byte[] decodePEM(byte[] pemData) {
        int base64Start = findBase64Start(pemData);
        int base64End = findBase64End(pemData);

        byte[] base64 = Arrays.copyOfRange(pemData, base64Start, base64End);
        base64 = removeWhitespace(base64);

        return Base64.getDecoder().decode(base64);
    }

    /**
     * Finds start position of Base64 payload in PEM data (after first newline).
     *
     * @param data PEM-encoded data
     * @return index where Base64 content begins
     */
    private static int findBase64Start(byte[] data) {
        int firstNewline = indexOf(data, NEW_LINE, 0);
        return firstNewline != -1 ? firstNewline + 1 : PEM_PREFIX.length;
    }

    /**
     * Finds end position of Base64 payload in PEM data (before suffix marker).
     *
     * @param data PEM-encoded data
     * @return index where Base64 content ends
     * @throws IllegalArgumentException if PEM suffix marker is missing
     */
    private static int findBase64End(byte[] data) {
        int suffixPos = lastIndexOf(data, PEM_SUFFIX);
        if (suffixPos == -1) {
            throw new IllegalArgumentException("Invalid PEM: no ending marker");
        }

        int lastNewline = lastIndexOf(data, NEW_LINE, suffixPos - 1);
        return lastNewline != -1 ? lastNewline : suffixPos;
    }

    /**
     * Removes all whitespace characters (newlines, spaces, tabs) from byte array.
     *
     * @param data input byte array potentially containing whitespace
     * @return new byte array with whitespace removed
     */
    private static byte[] removeWhitespace(byte[] data) {
        byte[] result = new byte[data.length];
        int pos = 0;
        for (byte b : data) {
            if (!isWhitespace(b)) {
                result[pos++] = b;
            }
        }
        return Arrays.copyOf(result, pos);
    }

    /**
     * Checks if byte represents a whitespace character.
     *
     * @param b byte to check
     * @return true if byte is newline, carriage return, space or tab
     */
    private static boolean isWhitespace(byte b) {
        return b == '\n' || b == '\r' || b == ' ' || b == '\t';
    }

    /**
     * Finds first occurrence of target byte in array starting from given index.
     *
     * @param array     byte array to search
     * @param target    byte value to find
     * @param fromIndex starting position (inclusive)
     * @return index of first match or -1 if not found
     */
    private static int indexOf(byte[] array, byte target, int fromIndex) {
        for (int i = fromIndex; i < array.length; i++) {
            if (array[i] == target) return i;
        }
        return -1;
    }

    /**
     * Finds last occurrence of target byte in array before given index.
     *
     * @param array     byte array to search
     * @param target    byte value to find
     * @param fromIndex upper bound (inclusive)
     * @return index of last match or -1 if not found
     */
    private static int lastIndexOf(byte[] array, byte target, int fromIndex) {
        for (int i = fromIndex; i >= 0; i--) {
            if (array[i] == target) return i;
        }
        return -1;
    }

    /**
     * Finds last occurrence of target byte sequence in array.
     *
     * @param array  byte array to search
     * @param target sequence to find
     * @return starting index of last match or -1 if not found
     */
    private static int lastIndexOf(byte[] array, byte[] target) {
        for (int i = array.length - target.length; i >= 0; i--) {
            boolean match = true;
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }
}
