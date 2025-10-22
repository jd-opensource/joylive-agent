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
package com.jd.live.agent.implement.parser.jackson;

import com.jd.live.agent.core.util.IOUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JacksonTest {

    @Test
    public void testParsePerson() throws IOException {
        JacksonJsonParser jsonParser = new JacksonJsonParser();
        JacksonJsonPathParser pathParser = new JacksonJsonPathParser();
        try (InputStream inputStream = this.getClass().getResourceAsStream("/person.json")) {
            Assertions.assertNotNull(inputStream);
            String json = new String(IOUtils.read(inputStream));
            Person person = jsonParser.read(new StringReader(json), Person.class);
            Assertions.assertEquals(person.getSex(), Sex.FEMALE);
            Assertions.assertEquals(pathParser.read(json, "$.name"), "person");
            Assertions.assertTrue(person.containsAlias("john doe"));
        }

    }

    @Test
    public void testParseObject() throws IOException {
        JacksonJsonParser jsonParser = new JacksonJsonParser();
        Object obj1 = jsonParser.read(new StringReader("123"), Object.class);
        Object obj2 = jsonParser.read(new StringReader("[123]"), Object.class);
        Object obj3 = jsonParser.read(new StringReader("\"abc\""), Object.class);
        Object obj4 = jsonParser.read(new StringReader("{\"name\":\"abc\"}"), Object.class);
        Assertions.assertNotNull(obj1);
        Assertions.assertNotNull(obj2);
        Assertions.assertNotNull(obj3);
        Assertions.assertNotNull(obj4);
    }

    @Test
    public void testObjectConverter() throws Exception {

        JacksonConverter converter = new JacksonConverter();
        Assert.assertEquals(1, (Object) converter.convert("1", int.class));
        Assert.assertEquals(1, (Object) converter.convert(1L, int.class));
        Assert.assertEquals(1, (Object) converter.convert(1.1, int.class));

        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 30L);
        map.put("sex", "MALE");
        map.put("aliases", Arrays.asList("john", "doe"));
        Person person = converter.convert(map, Person.class);
        Assert.assertEquals("John", person.getName());
        Assert.assertEquals(30, person.getAge());
        Assert.assertEquals(Sex.MALE, person.getSex());
        Assert.assertEquals(2, person.getAliases().size());
        Assert.assertTrue(person.getAliases().contains("john"));
        Assert.assertTrue(person.getAliases().contains("doe"));
    }
}
