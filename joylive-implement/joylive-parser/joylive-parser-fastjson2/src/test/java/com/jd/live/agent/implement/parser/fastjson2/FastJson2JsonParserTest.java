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
package com.jd.live.agent.implement.parser.fastjson2;

import com.jd.live.agent.core.util.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

public class FastJson2JsonParserTest {

    @Test
    public void testJson() throws IOException {
        Fastjson2JsonParser jsonParser = new Fastjson2JsonParser();
        jsonParser.initialize();
        Fastjson2JsonPathParser pathParser = new Fastjson2JsonPathParser();
        try (InputStream inputStream = this.getClass().getResourceAsStream("/person.json")) {
            Assertions.assertNotNull(inputStream);
            String json = new String(IOUtils.read(inputStream));
            Person person = jsonParser.read(new StringReader(json), Person.class);
            Assertions.assertEquals(person.getSex(), Sex.MALE);
            Assertions.assertEquals(pathParser.read(json, "$.name"), "person");
            Assertions.assertTrue(person.containsAlias("john doe"));
        }

    }
}
