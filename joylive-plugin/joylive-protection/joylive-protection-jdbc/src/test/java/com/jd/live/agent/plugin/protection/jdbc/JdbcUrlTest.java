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
package com.jd.live.agent.plugin.protection.jdbc;

import com.jd.live.agent.plugin.protection.jdbc.util.JdbcUrl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JdbcUrlTest {

    @Test
    void testUri() {
        JdbcUrl url = JdbcUrl.parse("jdbc:mysql://127.0.0.1:3306/test");
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:mysql", url.getScheme());
        Assertions.assertEquals("127.0.0.1", url.getHost());
        Assertions.assertEquals(3306, url.getPort());
        Assertions.assertEquals("/test", url.getPath());
        Assertions.assertEquals("jdbc:mysql://127.0.0.1:3306/test", url.toString());

        url = JdbcUrl.parse("jdbc:jtds:sqlserver://[2001:db8:85a3::8a2e:370:7334]:1433/Database");
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:jtds:sqlserver", url.getScheme());
        Assertions.assertEquals("[2001:db8:85a3::8a2e:370:7334]", url.getHost());
        Assertions.assertEquals(1433, url.getPort());
        Assertions.assertEquals("/Database", url.getPath());
        Assertions.assertEquals("jdbc:jtds:sqlserver://[2001:db8:85a3::8a2e:370:7334]:1433/Database", url.toString());

        url = JdbcUrl.parse("jdbc:h2:mem:testdb");
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:h2:mem:testdb", url.getHost());
    }
}
