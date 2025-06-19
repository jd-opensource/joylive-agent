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
package com.jd.live.agent.governance.db.parser.sqlserver;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.db.parser.AbstractUrlParser;
import com.jd.live.agent.governance.db.DbUrl.DbUrlBuilder;

import static com.jd.live.agent.core.util.StringUtils.splitMap;

@Extension("sqlserver")
public class SqlServerUrlParser extends AbstractUrlParser {

    @Override
    protected void parseDatabase(DbUrlBuilder builder) {
        String path = builder.getPath();
        int pos = path == null ? -1 : path.lastIndexOf('/');
        if (pos >= 0) {
            builder.database(path.substring(pos + 1));
        } else {
            String parameter = builder.getParameter();
            if (parameter != null && !parameter.isEmpty()) {
                splitMap(parameter, c -> c == ';', true, (key, value) -> {
                    if (key.equalsIgnoreCase("databaseName")) {
                        builder.database(value);
                    }
                    return true;
                });
            }
        }
    }

    @Override
    protected char getParameterDelimiter() {
        return ';';
    }
}
