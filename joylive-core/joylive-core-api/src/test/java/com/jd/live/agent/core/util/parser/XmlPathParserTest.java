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
package com.jd.live.agent.core.util.parser;

import com.jd.live.agent.core.parser.XmlPathParser;
import com.jd.live.agent.core.parser.jdk.JdkXmlPathParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class XmlPathParserTest {

    @Test
    void testParse() {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<errorResponse>\n" +
                "    <timestamp>2023-11-15T14:30:45Z</timestamp>\n" +
                "    <status>404</status>\n" +
                "    <message>Resource not found</message>\n" +
                "    <details>\n" +
                "        <path>/api/users/123</path>\n" +
                "        <errorCode>USER_NOT_FOUND</errorCode>\n" +
                "        <suggestions>\n" +
                "            <suggestion>Check user ID</suggestion>\n" +
                "            <suggestion>Verify account exists</suggestion>\n" +
                "        </suggestions>\n" +
                "    </details>\n" +
                "    <contact supportEmail=\"support@example.com\" phone=\"+1-555-123-4567\"/>\n" +
                "</errorResponse>";
        XmlPathParser parser = new JdkXmlPathParser();
        Assertions.assertEquals("404", parser.read(response, "//status/text()"));
        Assertions.assertEquals("support@example.com", parser.read(response, "//contact/@supportEmail"));
    }
}
