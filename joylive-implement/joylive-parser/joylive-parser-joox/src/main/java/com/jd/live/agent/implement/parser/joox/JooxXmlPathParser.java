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
package com.jd.live.agent.implement.parser.joox;

import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.parser.XmlPathParser;
import org.joox.JOOX;
import org.joox.Match;
import org.w3c.dom.Document;

import java.io.InputStream;

import static org.joox.JOOX.$;

@Extension(value = "joox", order = XmlPathParser.ORDER_JOOX)
public class JooxXmlPathParser implements XmlPathParser {

    private static final String TEXT = "text()";

    @Override
    public String read(InputStream in, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            String attr = null;
            int pos = path.lastIndexOf('/');
            if (pos >= 0) {
                String subPath = path.substring(pos + 1);
                if (subPath.equals(TEXT)) {
                    path = path.substring(0, pos);
                } else if (subPath.charAt(0) == '@') {
                    path = path.substring(0, pos);
                    attr = subPath.substring(1);
                }
            }
            Document doc = JOOX.builder().parse(in);
            Match match = $(doc).xpath(path);
            if (match.isEmpty()) {
                return null;
            } else if (attr != null && !attr.isEmpty()) {
                return match.attr(attr);
            } else {
                return match.text();
            }
        } catch (Throwable e) {
            throw new ParseException("Failed to parse XML with JOOX, path: " + path, e);
        }
    }
}
