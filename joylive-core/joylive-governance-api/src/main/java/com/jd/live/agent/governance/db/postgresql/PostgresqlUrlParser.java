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
package com.jd.live.agent.governance.db.postgresql;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.db.AbstractUrlParser;
import com.jd.live.agent.governance.db.DbUrl.DbUrlBuilder;

@Extension("postgresql")
public class PostgresqlUrlParser extends AbstractUrlParser {

    private static final String JDBC_POSTGRESQL = "jdbc:postgresql";
    private static final String JDBC_POSTGRESQL_PREFIX = JDBC_POSTGRESQL + ":";
    private static final String JDBC_POSTGRESQL_FULL_PREFIX = JDBC_POSTGRESQL + "://";

    @Override
    protected String parserScheme(String url, DbUrlBuilder builder) {
        url = super.parserScheme(url, builder);
        String scheme = builder.getScheme();
        if (scheme == null || scheme.isEmpty()) {
            if (url.startsWith(JDBC_POSTGRESQL_PREFIX)) {
                builder.scheme(JDBC_POSTGRESQL);
                builder.schemePart(JDBC_POSTGRESQL_FULL_PREFIX);
                url = url.substring(JDBC_POSTGRESQL_PREFIX.length());
                if (!url.startsWith("/")) {
                    // jdbc:postgresql:database
                    url = "localhost/" + url;
                }
            }
        }
        return url;
    }

    @Override
    protected String getDefaultHost() {
        return "localhost";
    }
}
