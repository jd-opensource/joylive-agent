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
package com.jd.live.agent.governance.db.oracle;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.db.AbstractUrlParser;
import com.jd.live.agent.governance.db.DbUrl;

@Extension("oracle")
public class OracleUrlParser extends AbstractUrlParser {

    private static final String JDBC_ORACLE_THIN = "jdbc:oracle:thin";
    private static final String JDBC_ORACLE_THIN_PREFIX = "jdbc:oracle:thin:@";
    private static final String JDBC_ORACLE_THIN_SERVICE_PREFIX = "jdbc:oracle:thin:@//";

    @Override
    protected String parserScheme(String url, DbUrl.DbUrlBuilder builder) {
        //jdbc:oracle:thin:@//<host>:<port>/<service_name>
        //jdbc:oracle:thin:@<host>:<port>:<SID>
        //jdbc:oracle:thin:@<TNSName>
        if (url.startsWith(JDBC_ORACLE_THIN_SERVICE_PREFIX)) {
            builder.scheme(JDBC_ORACLE_THIN);
            builder.schemePart(JDBC_ORACLE_THIN_SERVICE_PREFIX);
            url = url.substring(JDBC_ORACLE_THIN_SERVICE_PREFIX.length());
        } else if (url.startsWith(JDBC_ORACLE_THIN_PREFIX)) {
            builder.scheme(JDBC_ORACLE_THIN);
            builder.schemePart(JDBC_ORACLE_THIN_PREFIX);
            url = url.substring(JDBC_ORACLE_THIN_PREFIX.length());
        }
        return url;
    }

    @Override
    protected String parsePath(String url, DbUrl.DbUrlBuilder builder) {
        url = super.parsePath(url, builder);
        if (builder.getPath() == null || builder.getPath().isEmpty()) {
            if (!url.startsWith("(")) {
                int pos = url.lastIndexOf(':');
                if (pos > 0) {
                    builder.path(url.substring(pos));
                    builder.database(url.substring(pos + 1));
                    url = url.substring(0, pos);
                }
            } else {
                // TODO TNSName
            }
        }
        return url;
    }
}
