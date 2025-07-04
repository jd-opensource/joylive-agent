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
package com.jd.live.agent.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class URITest {

    @Test
    void testParse() {
        URI uri = URI.parse("http://a.b.com/order?id=123&a&c=&=d");
        Assertions.assertNotNull(uri);
        Assertions.assertEquals("http", uri.getScheme());
        Assertions.assertEquals("a.b.com", uri.getHost());
        Assertions.assertEquals("/order", uri.getPath());
        Assertions.assertEquals("123", uri.getParameter("id"));
        Assertions.assertTrue(uri.hasParameter("a"));
        uri = uri.port(8080).path("/book").parameter("author", "zhangsan");
        Assertions.assertNotNull(uri);
        Assertions.assertEquals(8080, uri.getPort());
        Assertions.assertEquals("/book", uri.getPath());
        Assertions.assertEquals("zhangsan", uri.getParameter("author"));
        uri = URI.parse("a.b.com:8080");
        Assertions.assertNotNull(uri);
        Assertions.assertEquals(8080, uri.getPort());
        Assertions.assertEquals("a.b.com", uri.getHost());
    }

    @Test
    void testPort() {
        Assertions.assertEquals(8080, URI.parse("http://a:8080").getPort());
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parse("[1080:0:0:0:8:800:200C:417A]:8080#book").getHost());
        Assertions.assertEquals(8080, URI.parse("[1080:0:0:0:8:800:200C:417A]:8080#book").getPort());
        Assertions.assertEquals("[1080:0:0:0:8:800:200C:417A]:8080", URI.parse("[1080:0:0:0:8:800:200C:417A]:8080#book").getAddress());
    }

    @Test
    void testParseHost() {

        Assertions.assertEquals("a.b.com", URI.parseHost("http://a.b.com"));
        Assertions.assertEquals("a.b.com", URI.parseHost("http://a.b.com?userid=1"));
        Assertions.assertEquals("a.b.com", URI.parseHost("http://a.b.com/book"));
        Assertions.assertEquals("a.b.com", URI.parseHost("http://u1:p1@a.b.com"));
        Assertions.assertEquals("a.b.com", URI.parseHost("http://a.b.com:8080"));
        Assertions.assertEquals("a.b.com", URI.parseHost("http://u1:p1@a.b.com:8080"));
        Assertions.assertEquals("a.b.com", URI.parseHost("http://a.b.com:8080?userid=1"));
        Assertions.assertEquals("a.b.com", URI.parseHost("http://a.b.com:8080/book"));
        Assertions.assertEquals("a.b.com", URI.parseHost("http://a.b.com:8080#book"));
        Assertions.assertEquals("a.b.com", URI.parseHost("a.b.com"));
        Assertions.assertEquals("a.b.com", URI.parseHost("u1:p1@a.b.com"));
        Assertions.assertEquals("a.b.com", URI.parseHost("a.b.com?userid=1"));
        Assertions.assertEquals("a.b.com", URI.parseHost("a.b.com/book"));
        Assertions.assertEquals("a.b.com", URI.parseHost("a.b.com:8080"));
        Assertions.assertEquals("a.b.com", URI.parseHost("u1:p1@a.b.com:8080"));
        Assertions.assertEquals("a.b.com", URI.parseHost("a.b.com:8080?userid=1"));
        Assertions.assertEquals("a.b.com", URI.parseHost("a.b.com:8080/book"));
        Assertions.assertEquals("a.b.com", URI.parseHost("a.b.com:8080#book"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("http://[1080:0:0:0:8:800:200C:417A]"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("http://u1:p1@[1080:0:0:0:8:800:200C:417A]"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("http://[1080:0:0:0:8:800:200C:417A]?userid=1"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("http://[1080:0:0:0:8:800:200C:417A]/book"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("http://[1080:0:0:0:8:800:200C:417A]:8080"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("http://[1080:0:0:0:8:800:200C:417A]:8080?userid=1"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("http://[1080:0:0:0:8:800:200C:417A]:8080/book"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("http://[1080:0:0:0:8:800:200C:417A]:8080#book"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("[1080:0:0:0:8:800:200C:417A]"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("u1:p1@[1080:0:0:0:8:800:200C:417A]"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("[1080:0:0:0:8:800:200C:417A]?userid=1"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("[1080:0:0:0:8:800:200C:417A]/book"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("[1080:0:0:0:8:800:200C:417A]:8080"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("[1080:0:0:0:8:800:200C:417A]:8080?userid=1"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("[1080:0:0:0:8:800:200C:417A]:8080/book"));
        Assertions.assertEquals("1080:0:0:0:8:800:200C:417A", URI.parseHost("[1080:0:0:0:8:800:200C:417A]:8080#book"));
        Assertions.assertEquals("1:", URI.parseHost("http://[1:"));
        Assertions.assertNull(URI.parseHost("http://[]"));
        Assertions.assertNull(URI.parseHost("http://u1:p1@"));
        Assertions.assertNull(URI.parseHost("u1:p1@"));
        Assertions.assertNull(URI.parseHost("@"));
        Assertions.assertNull(URI.parseHost("http://u1:p1@:8080"));
        Assertions.assertNull(URI.parseHost("http://:8080"));
        Assertions.assertNull(URI.parseHost(":8080"));
        Assertions.assertNull(URI.parseHost("http://"));
        Assertions.assertNull(URI.parseHost("http:///"));
        Assertions.assertNull(URI.parseHost("http://?userid=1"));
        Assertions.assertEquals(8, URI.parse("a:8?b=9").getPort());
    }

    @Test
    void testParseJdbc() {
        URI uri = URI.parse("jdbc:mariadb://localhost:8080/book");
        Assertions.assertNotNull(uri);
        Assertions.assertEquals("jdbc:mariadb", uri.getScheme());
        Assertions.assertEquals("localhost", uri.getHost());
        Assertions.assertEquals(8080, uri.getPort());
        Assertions.assertEquals("/book", uri.getPath());
    }

    @Test
    void testLb() {
        URI uri = URI.parse("lb://com.jd.live.agent.demo.service.SleepService:DEFAULT");
        Assertions.assertEquals("lb", uri.getScheme());
        Assertions.assertEquals("com.jd.live.agent.demo.service.SleepService:DEFAULT", uri.getHost());
    }

    @Test
    void testPassword() {
        URI uri = URI.parse("redis://12345@localhost:6957/0");
        Assertions.assertNotNull(uri);
        Assertions.assertEquals("redis", uri.getScheme());
        Assertions.assertEquals("localhost", uri.getHost());
        Assertions.assertEquals(6957, uri.getPort());
        Assertions.assertNull(uri.getUser());
        Assertions.assertEquals("12345", uri.getPassword());
    }


}
