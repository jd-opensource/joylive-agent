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
package com.jd.live.agent.core.inject;

import com.jd.live.agent.core.config.AgentConfig;
import com.jd.live.agent.core.extension.ExtensionManager;
import com.jd.live.agent.core.extension.jplug.JExtensionManager;
import com.jd.live.agent.core.parser.ConfigParser;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.option.CascadeOption;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class InjectorsTest {

    private static ExtensionManager extensionManager;
    private static Injection defaultInjectors;

    private static Injection embedInjectors;

    @BeforeAll
    public static void initialize() {
        System.setProperty("RE_TRANSFORM_ENABLED", "true");
        extensionManager = new JExtensionManager();
        InjectorFactory injectorFactory = extensionManager.getOrLoadExtension(InjectorFactory.class);
        defaultInjectors = injectorFactory.create(extensionManager);
        embedInjectors = injectorFactory.create(extensionManager, null, null, true);
    }

    @Test
    public void testCascade() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> agentMap = new HashMap<>();
        map.put("agent", agentMap);
        Map<String, Object> enhanceMap = new HashMap<>();
        agentMap.put("enhance", enhanceMap);
        enhanceMap.put("javaVersion", "[,1.8);[1.8.0_61,]");
        enhanceMap.put("reTransformEnabled", "${RE_TRANSFORM_ENABLED:false}");
        List<String> excludeClasses = Arrays.asList("xxx", "yyy", "ddd");
        enhanceMap.put("excludeTypes", excludeClasses);

        AgentConfig config = new AgentConfig();
        defaultInjectors.inject(new CascadeOption(map), config);
        Assertions.assertEquals("[,1.8);[1.8.0_61,]", config.getEnhanceConfig().getJavaVersion());
        Assertions.assertNotNull(config.getEnhanceConfig());
        Assertions.assertNotNull(config.getEnhanceConfig().getExcludeTypes());
        Assertions.assertEquals(3, config.getEnhanceConfig().getExcludeTypes().size());
    }

    @Test
    public void testNoneCascade() {
        List<String> excludeClasses = Arrays.asList("xxx", "yyy", "ddd");
        Map<String, Object> map = new HashMap<>();
        map.put("agent.enhance.javaVersion", "[,1.8);[1.8.0_61,]");
        map.put("agent.enhance.reTransformEnabled", "${RE_TRANSFORM_ENABLED:false}");
        map.put("agent.enhance.excludeTypes", excludeClasses);

        AgentConfig config = new AgentConfig();
        defaultInjectors.inject(new CascadeOption(map), config);
        Assertions.assertEquals("[,1.8);[1.8.0_61,]", config.getEnhanceConfig().getJavaVersion());
        Assertions.assertNotNull(config.getEnhanceConfig());
        Assertions.assertNotNull(config.getEnhanceConfig().getExcludeTypes());
        Assertions.assertEquals(3, config.getEnhanceConfig().getExcludeTypes().size());
    }

    @Test
    public void testGeneric() {
        ObjectParser objectWriter = extensionManager.getOrLoadExtension(ObjectParser.class, ObjectParser.YAML);
        ConfigParser configParser = extensionManager.getOrLoadExtension(ConfigParser.class, ObjectParser.YAML);

        AppleSeller seller = new AppleSeller();
        seller.setName("wonderful");
        seller.addGood(new Apple(100.0D, Color.RED));
        seller.addGood(new Apple(50, Color.GREEN));

        String value = toString(objectWriter, seller);
        System.out.println(value);
        Map<String, Object> map = configParser.parse(new StringReader(value));

        AppleSeller seller1 = new AppleSeller();
        embedInjectors.inject(map, seller1);

        String value1 = toString(objectWriter, seller1);
        System.out.println(value1);

        Assertions.assertEquals(value1, value);
    }

    private static String toString(ObjectParser objectWriter, Object object) {
        StringWriter stringWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
        objectWriter.write(bufferedWriter, object);
        return stringWriter.toString();
    }

    protected static class Fruit {
        private double price;

        Fruit() {
        }

        Fruit(double price) {
            this.price = price;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }
    }

    protected enum Color {
        RED, GREEN
    }

    protected static class Apple extends Fruit {

        private Color color;

        Apple() {
        }

        Apple(double price, Color color) {
            super(price);
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }
    }

    protected static class Seller<T> {

        List<T> goods;

        public List<T> getGoods() {
            return goods;
        }

        public void setGoods(List<T> goods) {
            this.goods = goods;
        }

        public void addGood(T good) {
            if (good != null) {
                if (goods == null) {
                    goods = new ArrayList<>();
                }
                goods.add(good);
            }
        }
    }

    protected static class AppleSeller extends Seller<Apple> {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
