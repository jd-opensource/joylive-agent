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
package com.jd.live.agent.core.util.network;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AddressTest {

    @Test
    void testIsIpv4() {
        Address address = Address.parse("127.0.0.1", false, 8080);
        Assertions.assertEquals("127.0.0.1", address.getHost());
        Assertions.assertEquals("127.0.0.1", address.getUriHost());
        Assertions.assertEquals(8080, address.getPort());

        address = Address.parse("[2001:db8:85a3::8a2e:370:7334]", false, 8080);
        Assertions.assertEquals("2001:db8:85a3::8a2e:370:7334", address.getHost());
        Assertions.assertEquals("[2001:db8:85a3::8a2e:370:7334]", address.getUriHost());
        Assertions.assertEquals(8080, address.getPort());

        address = Address.parse("2001:db8:85a3::8a2e:370:7334", false, 8080);
        Assertions.assertEquals("2001:db8:85a3::8a2e:370:7334", address.getHost());
        Assertions.assertEquals("[2001:db8:85a3::8a2e:370:7334]", address.getUriHost());
        Assertions.assertEquals(8080, address.getPort());
        Assertions.assertEquals("[2001:db8:85a3::8a2e:370:7334]:8080", address.getAddress());

        address = Address.parse("2001:db8:85a3::8a2e:370:7334");
        Assertions.assertEquals("2001:db8:85a3::8a2e:370:7334", address.getHost());
        Assertions.assertEquals("[2001:db8:85a3::8a2e:370:7334]", address.getUriHost());
        Assertions.assertNull(address.getPort());

        address = Address.parse("[2001:db8:85a3::8a2e:370:7334]");
        Assertions.assertEquals("2001:db8:85a3::8a2e:370:7334", address.getHost());
        Assertions.assertEquals("[2001:db8:85a3::8a2e:370:7334]", address.getUriHost());
        Assertions.assertNull(address.getPort());

        address = Address.parse("[2001:db8:85a3::8a2e:370:7334]:8080");
        Assertions.assertEquals("2001:db8:85a3::8a2e:370:7334", address.getHost());
        Assertions.assertEquals("[2001:db8:85a3::8a2e:370:7334]", address.getUriHost());
        Assertions.assertEquals(8080, address.getPort());
        Assertions.assertEquals("[2001:db8:85a3::8a2e:370:7334]:8080", address.getAddress());
    }


}
