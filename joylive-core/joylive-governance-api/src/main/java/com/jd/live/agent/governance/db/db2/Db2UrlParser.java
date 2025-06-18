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
package com.jd.live.agent.governance.db.db2;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.db.AbstractUrlParser;
import com.jd.live.agent.governance.db.DbUrl.DbUrlBuilder;

import static com.jd.live.agent.core.util.StringUtils.QUERY;
import static com.jd.live.agent.core.util.StringUtils.splitMap;

@Extension("db2")
public class Db2UrlParser extends AbstractUrlParser {

    @Override
    protected String parserParameter(String url, DbUrlBuilder builder) {
        int pos = url == null ? -1 : url.lastIndexOf('/');
        if (pos > 0) {
            pos = url.indexOf(':', pos + 1);
            if (pos > 0) {
                String parameter = url.substring(pos + 1);
                builder.parameter(parameter);
                builder.parameters(splitMap(parameter, QUERY));
                builder.parameterPart(url.substring(pos));
                url = url.substring(0, pos);
            }
        }
        return url;
    }


}
