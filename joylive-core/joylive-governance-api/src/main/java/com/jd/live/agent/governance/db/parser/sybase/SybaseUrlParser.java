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
package com.jd.live.agent.governance.db.parser.sybase;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.db.parser.AbstractUrlParser;
import com.jd.live.agent.governance.db.DbUrl.DbUrlBuilder;

@Extension("sybase")
public class SybaseUrlParser extends AbstractUrlParser {

    private static final String JDBC_SQLITE = "jdbc:sybase:Tds";
    private static final String JDBC_SQLITE_PREFIX = JDBC_SQLITE + ":";

    @Override
    protected String parserScheme(String url, DbUrlBuilder builder) {
        builder.schemePart(JDBC_SQLITE_PREFIX).scheme("jdbc:sybase:Tds");
        return url.substring(JDBC_SQLITE_PREFIX.length());
    }
}
