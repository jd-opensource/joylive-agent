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
package com.jd.live.agent.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.jd.live.agent.core.Constants.*;

public class ConstantsTest {

    @Test
    void testDefaultGrup() {
        Assertions.assertTrue(DEFAULT_GROUP_PREDICATE.test(null));
        Assertions.assertTrue(DEFAULT_GROUP_PREDICATE.test(""));
        Assertions.assertTrue(DEFAULT_GROUP_PREDICATE.test(DEFAULT_GROUP));
        Assertions.assertFalse(DEFAULT_GROUP_PREDICATE.test("test"));

        Assertions.assertTrue(DEFAULT_GROUP_BIPREDICATE.test(null, Constants.DEFAULT_GROUP));
        Assertions.assertTrue(DEFAULT_GROUP_BIPREDICATE.test("", Constants.DEFAULT_GROUP));
        Assertions.assertTrue(DEFAULT_GROUP_BIPREDICATE.test(DEFAULT_GROUP, Constants.DEFAULT_GROUP));
        Assertions.assertFalse(DEFAULT_GROUP_BIPREDICATE.test("test", Constants.DEFAULT_GROUP));

        Assertions.assertTrue(Constants.SAME_GROUP_PREDICATE.test(null, ""));
        Assertions.assertTrue(Constants.SAME_GROUP_PREDICATE.test(null, null));
        Assertions.assertTrue(Constants.SAME_GROUP_PREDICATE.test("", ""));
        Assertions.assertTrue(Constants.SAME_GROUP_PREDICATE.test("", null));
        Assertions.assertTrue(Constants.SAME_GROUP_PREDICATE.test(null, DEFAULT_GROUP));
        Assertions.assertTrue(Constants.SAME_GROUP_PREDICATE.test("", DEFAULT_GROUP));
        Assertions.assertFalse(Constants.SAME_GROUP_PREDICATE.test("test", DEFAULT_GROUP));
    }

}
