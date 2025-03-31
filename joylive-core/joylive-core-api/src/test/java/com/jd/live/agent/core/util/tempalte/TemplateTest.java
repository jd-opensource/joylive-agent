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
package com.jd.live.agent.core.util.tempalte;

import com.jd.live.agent.core.util.template.Template;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.jd.live.agent.core.util.template.Template.context;
import static com.jd.live.agent.core.util.template.Template.evaluate;

public class TemplateTest {

    @Test
    void testServerPort1() {
        String expression = "${P:${r[1,2]}}";
        Assertions.assertEquals("8080", evaluate(expression, false, "P", "8080"));
    }

    @Test
    void testTemplate() {
        Template template = Template.parse("Value: ${a:prefix-${b:${c:e}-suffix}} hello");
        Assertions.assertEquals("Value: A hello", template.evaluate(context("a", "A", "b", "B", "c", "C"), false));
        Assertions.assertEquals("Value: prefix-B hello", template.evaluate(context("b", "B", "c", "C"), false));
        Assertions.assertEquals("Value: prefix-C-suffix hello", template.evaluate(context("c", "C"), false));
        Assertions.assertEquals("Value: prefix-e-suffix hello", template.evaluate(false));
    }

    @Test
    void testSpecialKey() {
        String expression = "${$a:}";
        Assertions.assertEquals("A", evaluate(expression, false, "$a", "A"));
    }

    @Test
    void testIllegal() {
        String expression = "{$a:${b:${c:c}-suffix}";
        Assertions.assertEquals("{$a:c-suffix", evaluate(expression, false, "$a", "A"));
    }

    @Test
    void testServerPort() {
        String expression = "server.port=${SERVER_PORT:${random.int[11000,11999]}}";
        Template template = Template.parse(expression);
        Assertions.assertEquals("server.port=8080", template.evaluate(context("SERVER_PORT", "8080"), false));
        Assertions.assertEquals(expression, template.evaluate(context(), false));
    }

    @Test
    void testPrefix() {
        Template template = Template.parse("${topic}${'_unit_'unit}${'_lane_'lane}");
        Assertions.assertEquals("order_unit_unit1", template.evaluate(context("topic", "order", "unit", "unit1"), true));
        Assertions.assertEquals("order_unit_unit1${'_lane_'lane}", template.evaluate(context("topic", "order", "unit", "unit1"), false));
    }

    @Test
    void testRegistry() {
        String expression = "${A:${B}:${C:8848}}";
        Assertions.assertEquals("localhost:8848", evaluate(expression, false, "B", "localhost"));
    }
}
