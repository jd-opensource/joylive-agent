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
package com.jd.live.agent.core.util.version;

import com.jd.live.agent.core.exception.VersionException;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Version
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class Version implements VersionMatcher, Comparable<Version> {

    public static final Version LOWEST = new Version("0");
    public static final Version HIGHEST = new Version("999999999");

    @Getter
    private String version;
    private int[] parts;

    public Version(String version) {
        if (version != null && !version.isEmpty()) {
            int start = -1;
            int end = -1;
            char ch;
            List<String> versions = new ArrayList<>();
            for (int i = 0; i < version.length(); i++) {
                ch = version.charAt(i);
                switch (ch) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        if (start == -1)
                            start = i;
                        end = i;
                        break;
                    case '.':
                    case '_':
                        if (end >= 0) {
                            versions.add(version.substring(start, end + 1));
                            start = -1;
                            end = -1;
                        } else {
                            throw new VersionException("illegal version " + version);
                        }
                    case '-':
                        if (!versions.isEmpty())
                            break;
                        else
                            throw new VersionException("illegal version " + version);
                    default:
                        throw new VersionException("illegal version " + version);
                }
            }
            if (end >= 0) {
                versions.add(version.substring(start, end + 1));
            }
            int index = 0;
            parts = new int[versions.size()];
            for (String v : versions) {
                parts[index++] = Integer.parseInt(v);
            }
        }
        this.version = version;
    }

    public Version(int[] parts) {
        this.parts = parts;
    }

    @Override
    public boolean match(Version version) {
        if (version == this)
            return true;
        if (parts.length == 0)
            return true;
        if (version == null || version.parts.length == 0) {
            return false;
        }
        // 1.7 match 1.7.1 in fuzzy matching
        for (int i = 0; i < parts.length; i++) {
            if (i >= version.parts.length)
                return false;
            else if (parts[i] > version.parts[i])
                return false;
            else if (parts[i] < version.parts[i])
                return false;
        }
        return true;
    }

    @Override
    public int compareTo(Version o) {
        if (o == this)
            return 0;
        if (o == null || o.parts.length == 0) {
            return parts.length == 0 ? 0 : 1;
        }
        if (parts.length == 0)
            return -1;
        // 1.7 lower than 1.7.1 in version fuzzy matching
        for (int i = 0; i < parts.length; i++) {
            if (i >= o.parts.length)
                return 1;
            else if (parts[i] > o.parts[i])
                return 1;
            else if (parts[i] < o.parts[i])
                return -1;
        }
        return parts.length < o.parts.length ? -1 : 0;
    }

    @Override
    public String toString() {
        return version;
    }

    static List<String> split(String expression, char splitter) {
        List<String> parts = new ArrayList<>();
        int start = -1;
        int end = -1;
        char ch;
        int length = expression.length();
        for (int i = 0; i < length; i++) {
            ch = expression.charAt(i);
            if (ch == splitter) {
                if (end >= 0) {
                    parts.add(expression.substring(start, end + 1));
                    if (i == length - 1) {
                        parts.add("");
                    }
                    start = -1;
                    end = -1;
                } else {
                    parts.add("");
                }
            } else {
                if (start == -1)
                    start = i;
                end = i;
            }
        }
        if (end >= 0) {
            parts.add(expression.substring(start, end + 1));
        }
        return parts;
    }
}
