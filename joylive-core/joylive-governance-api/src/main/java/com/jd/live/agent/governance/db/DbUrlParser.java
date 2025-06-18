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
package com.jd.live.agent.governance.db;

import com.jd.live.agent.core.extension.annotation.Extensible;

import java.util.function.Function;

/**
 * Parser for database connection URLs with extensible protocol support.
 * The default implementation handles JDBC-style URLs (jdbc:type:...).
 */
@Extensible("DbUrlParser")
public interface DbUrlParser {

    String TYPE_DEFAULT = "default";
    String JDBC = "jdbc";

    /**
     * Parses a database URL of specified type into structured form.
     *
     * @param type database type/protocol (e.g., "mysql", "postgresql")
     * @param url the connection URL to parse
     * @return parsed DbUrl object, or null if invalid format
     */
    DbUrl parse(String type, String url);

    /**
     * Default parser that delegates to protocol-specific implementations.
     * @param url JDBC-style URL (jdbc:type:...)
     * @param factory produces parsers for specific protocol types
     * @return parsed DbUrl or null if unsupported/invalid format
     */
    static DbUrl parse(String url, Function<String, DbUrlParser> factory) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        int pos1 = url.indexOf(':');
        if (pos1 < 0) {
            return null;
        }
        String type = url.substring(0, pos1);
        if (!JDBC.equalsIgnoreCase(type)) {
            // mongodb://
            if (url.length() < pos1 + 2 || url.charAt(pos1 + 1) != '/' || url.charAt(pos1 + 2) != '/') {
                return null;
            }
        } else {
            int pos2 = url.indexOf(':', pos1 + 1);
            if (pos2 < 0) {
                return null;
            }
            type = url.substring(pos1 + 1, pos2).toLowerCase();
        }
        DbUrlParser parser = factory.apply(type);
        parser = parser == null ? factory.apply(TYPE_DEFAULT) : parser;
        return parser == null ? null : parser.parse(type, url);
    }
}
