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
package com.jd.live.agent.governance.db.parser;

import com.jd.live.agent.core.util.network.Address;
import com.jd.live.agent.governance.db.DbUrl;
import com.jd.live.agent.governance.db.DbUrl.DbUrlBuilder;
import com.jd.live.agent.governance.db.DbUrlParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.core.util.StringUtils.split;
import static com.jd.live.agent.core.util.StringUtils.splitMap;

/**
 * Abstract base class for parsing database connection URLs.
 * Implements a step-by-step parsing process for common URL components.
 */
public abstract class AbstractUrlParser implements DbUrlParser {

    @Override
    public DbUrl parse(String type, String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        DbUrlBuilder builder = DbUrl.builder().type(type);
        parse(url, builder);
        return builder.build();
    }

    /**
     * Parses a database URL into structured components.
     * <p>
     * Processing order:
     * 1. Parameters
     * 2. Scheme/protocol
     * 3. Path (if scheme exists)
     * 4. Security options
     * 5. Host addresses
     * 6. Database name
     *
     * @param url     the connection URL to parse
     * @param builder receives the parsed URL components
     */
    protected void parse(String url, DbUrlBuilder builder) {
        url = parserParameter(url, builder);
        url = parserScheme(url, builder);
        // fix type
        parseType(builder);
        if (builder.getScheme() != null) {
            // Support for JDBC URL
            url = parsePath(url, builder);
            url = parseSecure(url, builder);
            parseHosts(url, builder);
            parseDatabase(builder);
        }
    }

    /**
     * Extracts database name from URL (part after last '/')
     */
    protected void parseDatabase(DbUrlBuilder builder) {
        String path = builder.getPath();
        // database
        int pos = path == null ? -1 : path.lastIndexOf('/');
        if (pos >= 0) {
            builder.database(path.substring(pos + 1));
        }
    }

    /**
     * Extracts database name from file path.
     * <p>
     * Uses last segment of path (after last '/' or system file separator)
     * as the database name.
     *
     * @param builder the URL builder to update with database name
     */
    protected void parseFileDatabase(DbUrlBuilder builder) {
        String path = builder.getPath();
        int pos = path.lastIndexOf('/');
        if (pos >= 0) {
            builder.database(path.substring(pos + 1));
            return;
        } else if (File.separatorChar != '/') {
            pos = path.lastIndexOf(File.separatorChar);
            if (pos >= 0) {
                builder.database(path.substring(pos + 1));
                return;
            }
        }
        builder.database(path);
    }

    /**
     * Extracts path from URL
     */
    protected String parsePath(String url, DbUrlBuilder builder) {
        // jdbc:h2:file:/data/sample
        int pos = url == null ? -1 : url.lastIndexOf("/");
        if (pos >= 0) {
            builder.path(url.substring(pos));
            url = url.substring(0, pos);
        }
        return url;
    }

    /**
     * Extracts query parameters from URL
     */
    protected String parserParameter(String url, DbUrlBuilder builder) {
        // query
        char beginDelimiter = getParameterBeginDelimiter();
        char delimiter = getParameterDelimiter();
        int pos = url == null ? -1 : getParameterIndex(url, beginDelimiter);
        if (pos >= 0) {
            String parameter = url.substring(pos + 1);
            builder.parameterBeginDelimiter(beginDelimiter)
                    .parameterDelimiter(delimiter)
                    .parameters(splitMap(parameter, o -> o == delimiter))
                    .parameterPart(url.substring(pos));
            url = url.substring(0, pos);
        }
        return url;
    }

    /**
     * Extracts scheme/protocol information (part with '://')
     */
    protected String parserScheme(String url, DbUrlBuilder builder) {
        int pos = url == null ? -1 : url.indexOf("://");
        if (pos >= 0) {
            builder.scheme(url.substring(0, pos)).schemePart(url.substring(0, pos + 3));
            url = url.substring(pos + 3);
        }
        return url;
    }

    protected void parseType(DbUrlBuilder builder) {

    }

    /**
     * Extracts authentication credentials (user:password@ part)
     */
    protected String parseSecure(String url, DbUrlBuilder builder) {
        int pos = url == null ? -1 : url.indexOf('@');
        if (pos >= 0) {
            builder.userPart(url.substring(0, pos + 1));
            String secure = url.substring(0, pos);
            url = url.substring(pos + 1);
            pos = secure.indexOf(':');
            if (pos >= 0) {
                builder.user(secure.substring(0, pos)).password(secure.substring(pos + 1));
            }
        }
        return url;
    }

    /**
     * Parses host:port addresses (comma-separated list)
     */
    protected void parseHosts(String url, DbUrlBuilder builder) {
        // jdbc:mysql://[host:port],[host:port].../[database]
        if (url == null || url.isEmpty()) {
            url = getDefaultHost(builder);
        }
        String[] hosts = split(url, ',');
        if (hosts.length != 0) {
            List<Address> nodes = new ArrayList<>(hosts.length);
            for (String host : hosts) {
                nodes.add(Address.parse(host));
            }
            builder.address(url).nodes(nodes);
        }
    }

    protected int getParameterIndex(String url, char beginDelimiter) {
        return url.indexOf(beginDelimiter);
    }

    protected char getParameterBeginDelimiter() {
        return '?';
    }

    protected char getParameterDelimiter() {
        return '&';
    }

    protected String getDefaultHost(DbUrlBuilder builder) {
        return null;
    }

}
