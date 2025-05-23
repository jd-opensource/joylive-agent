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
package com.jd.live.agent.governance.policy.service.exception.xml;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.XmlPathParser;
import com.jd.live.agent.governance.policy.service.exception.AbstractErrorParser;

import java.io.InputStream;

@Injectable
@Extension("XmlPath")
public class XmlPathErrorParser extends AbstractErrorParser {

    @Inject
    private XmlPathParser parser;

    public XmlPathErrorParser() {
    }

    public XmlPathErrorParser(XmlPathParser parser) {
        this.parser = parser;
    }

    @Override
    protected String parse(String expression, String response) {
        return parser.read(response, expression);
    }

    @Override
    protected String parse(String expression, InputStream response) {
        return parser.read(response, expression);
    }
}
