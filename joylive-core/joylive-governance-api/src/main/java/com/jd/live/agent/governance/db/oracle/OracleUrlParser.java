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
import com.jd.live.agent.governance.db.DbUrl.DbUrlBuilder;

@Extension("oracle")
public class OracleUrlParser extends AbstractUrlParser {

    private static final String JDBC_ORACLE_THIN = "jdbc:oracle:thin";
    private static final String JDBC_ORACLE_THIN_SID_PREFIX = "jdbc:oracle:thin:@";
    private static final String JDBC_ORACLE_THIN_SERVICE_PREFIX = "jdbc:oracle:thin:@//";
    private static final String JDBC_ORACLE_THIN_TNS_PREFIX = "jdbc:oracle:thin:@(";

    @Override
    protected String parserScheme(String url, DbUrlBuilder builder) {
        if (url.startsWith(JDBC_ORACLE_THIN_SERVICE_PREFIX)) {
            //jdbc:oracle:thin:@//<host>:<port>/<service_name>
            builder.scheme(JDBC_ORACLE_THIN);
            builder.schemePart(JDBC_ORACLE_THIN_SERVICE_PREFIX);
            url = url.substring(JDBC_ORACLE_THIN_SERVICE_PREFIX.length());
        } else if (url.startsWith(JDBC_ORACLE_THIN_TNS_PREFIX)) {
            //Do not support jdbc:oracle:thin:@<TNSName>
        } else if (url.startsWith(JDBC_ORACLE_THIN_SID_PREFIX)) {
            //jdbc:oracle:thin:@<host>:<port>:<SID>
            builder.scheme(JDBC_ORACLE_THIN);
            builder.schemePart(JDBC_ORACLE_THIN_SID_PREFIX);
            url = url.substring(JDBC_ORACLE_THIN_SID_PREFIX.length());
        }
        return url;
    }

    @Override
    protected String parsePath(String url, DbUrlBuilder builder) {
        url = super.parsePath(url, builder);
        if (builder.getPath() == null || builder.getPath().isEmpty()) {
            int pos = url.lastIndexOf(':');
            if (pos > 0) {
                builder.path(url.substring(pos));
                builder.database(url.substring(pos + 1));
                url = url.substring(0, pos);
            }
        }
        return url;
    }
}
