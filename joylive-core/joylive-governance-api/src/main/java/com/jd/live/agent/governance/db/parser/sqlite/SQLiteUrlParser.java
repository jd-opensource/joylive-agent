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
package com.jd.live.agent.governance.db.parser.sqlite;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.db.parser.AbstractUrlParser;
import com.jd.live.agent.governance.db.DbUrl.DbUrlBuilder;

@Extension("sqlite")
public class SQLiteUrlParser extends AbstractUrlParser {

    private static final String JDBC_SQLITE = "jdbc:sqlite";
    private static final String JDBC_SQLITE_PREFIX = JDBC_SQLITE + ":";

    @Override
    protected String parserScheme(String url, DbUrlBuilder builder) {
        //jdbc:sqlite:C:/sqlite/db/chinook.db
        builder.schemePart(JDBC_SQLITE_PREFIX).scheme(JDBC_SQLITE);
        return url.substring(JDBC_SQLITE_PREFIX.length());
    }

    @Override
    protected void parseDatabase(DbUrlBuilder builder) {
        parseFileDatabase(builder);
    }

    @Override
    protected String parsePath(String url, DbUrlBuilder builder) {
        builder.path(url);
        return null;
    }

    @Override
    protected String parseSecure(String url, DbUrlBuilder builder) {
        return url;
    }

}
