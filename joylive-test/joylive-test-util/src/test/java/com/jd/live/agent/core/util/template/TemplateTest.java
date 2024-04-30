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
package com.jd.live.agent.core.util.template;

import com.jd.live.agent.governance.policy.live.Unit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class TemplateTest {

    @Test
    public void test() {

        Map<String, Object> context = new HashMap<>();
        context.put("unit", "bj");
        context.put("cell", "lf");
        context.put("group", "test");

        Template template = new Template("${unit}-${cell}-${group}", 48);
        String value = template.evaluate(context);
        Assertions.assertEquals("bj-lf-test", value);

        Unit unit = new Unit();
        unit.setCode("bj");

        template = new Template("${unit.code}-${cell}-${}-c", 48);
        context.put("unit", unit);
        value = template.evaluate(context);
        Assertions.assertEquals("bj-lf-${}-c", value);

    }


}
