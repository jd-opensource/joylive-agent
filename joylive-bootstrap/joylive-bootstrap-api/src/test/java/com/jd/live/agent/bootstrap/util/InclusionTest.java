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
package com.jd.live.agent.bootstrap.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class InclusionTest {

    @Test
    void testInclusion() {

        Inclusion inclusion = new Inclusion();
        inclusion.addName("a");
        inclusion.addNames(Arrays.asList("b", "c"));
        inclusion.addPrefix("d");
        inclusion.addPrefixes(Arrays.asList("e", "f"));
        inclusion.addClassName("a.b.c");
        inclusion.addClassName("a.b.d*");
        inclusion.addClassName("a.b.e/");
        inclusion.addClassName("a.b.f$");
        inclusion.addClassName("a.b.g.");

        Assertions.assertTrue(inclusion.test("a"));
        Assertions.assertTrue(inclusion.test("b"));
        Assertions.assertTrue(inclusion.test("c"));
        Assertions.assertTrue(inclusion.test("d.e"));
        Assertions.assertFalse(inclusion.test("hhh"));
        Assertions.assertTrue(inclusion.test("a.b.c"));
        Assertions.assertFalse(inclusion.test("a.b.c.d"));
        Assertions.assertTrue(inclusion.test("a.b.d.e"));
        Assertions.assertTrue(inclusion.test("a.b.e/f"));
        Assertions.assertTrue(inclusion.test("a.b.f$a"));
        Assertions.assertTrue(inclusion.test("a.b.g.k"));
        Assertions.assertFalse(inclusion.test("a.b"));

    }
}
