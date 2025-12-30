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

import java.util.HashMap;
import java.util.Map;

public class Ipv4Test {

    @Test
    void testIsIpv4() {
        Assertions.assertTrue(Ipv4.isIpv4("192.168.1.1"));
        Assertions.assertTrue(Ipv4.isIpv4("192.168.11.1"));
        Assertions.assertTrue(Ipv4.isIpv4("255.255.255.255"));
        Assertions.assertFalse(Ipv4.isIpv4("[255.255.255.255"));
        Assertions.assertFalse(Ipv4.isIpv4("256.255.255.255"));
        Assertions.assertFalse(Ipv4.isIpv4("256.255.255.2557"));
        Assertions.assertFalse(Ipv4.isIpv4("256.255.255"));
        Assertions.assertFalse(Ipv4.isIpv4("256.255.255."));
    }

    @Test
    void testHost() {
        Assertions.assertFalse(Ipv4.isHost("192.168.1.1"));
        Assertions.assertFalse(Ipv4.isHost("[2001:0db8:85a3:0000:0000:8a2e:0370:7334]"));
        Assertions.assertTrue(Ipv4.isHost("www.google.com"));
    }

    @Test
    void testClientIp() {

        Map<String, String> map = new HashMap<>();
        map.put("Forwarded", "for=192.168.1.1");
        Assertions.assertEquals("192.168.1.1", ClientIp.getIp(map::get));
        map.clear();
        map.put("Forwarded", "for=\"192.168.1.1:8888\"");
        Assertions.assertEquals("192.168.1.1", ClientIp.getIp(map::get));
        map.clear();
        map.put("Forwarded", "for=\"[2001:db8:cafe::17]:47011\"");
        Assertions.assertEquals("[2001:db8:cafe::17]", ClientIp.getIp(map::get));
        map.clear();
        map.put("Forwarded", "for=192.168.1.1;by=192.168.1.2");
        Assertions.assertEquals("192.168.1.1", ClientIp.getIp(map::get));
        map.clear();
        map.put("Forwarded", "by=192.168.1.2;for=192.168.1.1");
        Assertions.assertEquals("192.168.1.1", ClientIp.getIp(map::get));
        map.clear();
        map.put("Forwarded", "by=192.168.1.2;FOR=192.168.1.1;");
        Assertions.assertEquals("192.168.1.1", ClientIp.getIp(map::get));
        map.clear();
        map.put("Forwarded", ";for=192.168.1.1;");
        Assertions.assertEquals("192.168.1.1", ClientIp.getIp(map::get));
        map.clear();
        map.put("Forwarded", "for=;");
        Assertions.assertEquals("", ClientIp.getIp(map::get));
        map.clear();
        map.put("X-Forwarded-For", "192.168.1.1");
        Assertions.assertEquals("192.168.1.1", ClientIp.getIp(map::get));
        map.clear();
        Assertions.assertEquals("192.168.1.1", ClientIp.getIp(map::get, () -> "192.168.1.1"));
    }


}
