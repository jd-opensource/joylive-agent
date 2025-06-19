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
package com.jd.live.agent.governance.db.parser.h2;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.db.parser.AbstractUrlParser;
import com.jd.live.agent.governance.db.DbUrl.DbUrlBuilder;
import lombok.Getter;

@Extension("h2")
public class H2UrlParser extends AbstractUrlParser {

    @Override
    protected String parserScheme(String url, DbUrlBuilder builder) {
        /*
         jdbc:h2:[file:][<path>]<databaseName>
         jdbc:h2:~/test
         jdbc:h2:file:/data/sample
         jdbc:h2:file:C:/data/sample (Windows only)
         jdbc:h2:mem:test_mem
         jdbc:h2:tcp://<server>[:<port>]/[<path>]<databaseName>
         jdbc:h2:tcp://dbserv:8084/~/sample
         jdbc:h2:tcp://localhost/mem:test
         jdbc:h2:ssl://localhost/~/test;CIPHER=AES
         jdbc:h2:zip:~/db.zip!/test
        */
        String lower = url.toLowerCase();
        for (H2Type type : H2Type.values()) {
            if (lower.startsWith(type.getPart())) {
                builder.schemePart(type.getPart()).scheme(type.getScheme());
                return url.substring(type.getPart().length());
            }
        }
        return url;
    }

    @Override
    protected void parseDatabase(DbUrlBuilder builder) {
       parseFileDatabase(builder);
    }

    @Override
    protected String parsePath(String url, DbUrlBuilder builder) {
        String schema = builder.getScheme();
        if (H2Type.TCP.getScheme().equals(schema) || H2Type.SSL.getScheme().equals(schema)) {
            int pos = url.indexOf('/');
            if (pos >= 0) {
                builder.path(url.substring(pos));
                return url.substring(0, pos);
            }
            return url;
        } else {
            builder.path(url);
            return null;
        }
    }

    @Override
    protected char getParameterDelimiter() {
        return ';';
    }

    @Override
    protected String parseSecure(String url, DbUrlBuilder builder) {
        return url;
    }

    @Getter
    private enum H2Type {
        FILE("jdbc:h2:file", "jdbc:h2:file:"),
        ZIP("jdbc:h2:zip", "jdbc:h2:zip:"),
        TCP("jdbc:h2:tcp", "jdbc:h2:tcp://"),
        SSL("jdbc:h2:ssl", "jdbc:h2:ssl://"),
        MEM("jdbc:h2:mem", "jdbc:h2:mem:"),
        DEFAULT("jdbc:h2", "jdbc:h2:");

        private final String scheme;

        private final String part;

        H2Type(String scheme, String part) {
            this.scheme = scheme;
            this.part = part;
        }
    }
}
