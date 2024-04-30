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
package com.jd.live.agent.core.extension.test;

import com.jd.live.agent.core.extension.ExtensibleDesc;
import com.jd.live.agent.core.extension.ExtensionManager;
import com.jd.live.agent.core.extension.api.Byter;
import com.jd.live.agent.core.extension.byter.ByterTwo;
import com.jd.live.agent.core.extension.jplug.JExtensionManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * ExtensionManagerTest
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class ExtensionManagerTest {

    @Test
    public void testGetOrLoadExtensibleDesc() {
        ExtensionManager extensionManager = new JExtensionManager();
        ExtensibleDesc<Byter> spi1 = extensionManager.getOrLoadExtensible(Byter.class);
        ExtensibleDesc<Byter> spi2 = extensionManager.getOrLoadExtensible(Byter.class);
        Assertions.assertNotNull(spi1);
        Assertions.assertEquals(spi1, spi2);
    }

    @Test
    public void testGetOrLoadExtension() {
        ExtensionManager extensionManager = new JExtensionManager();
        Byter spi1 = extensionManager.getOrLoadExtension(Byter.class);
        Byter spi2 = extensionManager.getOrLoadExtension(Byter.class);
        Assertions.assertNotNull(spi1);
        Assertions.assertEquals(spi1.getClass(), ByterTwo.class);
        Assertions.assertNotEquals(spi1, spi2);
    }

    @Test
    public void testGet() {
        ExtensionManager extensionManager = new JExtensionManager();
        extensionManager.getOrLoadExtension(Byter.class);
        Byter byteOne = extensionManager.getExtension(Byter.class, "byterOne");
        Byter byteOneByProvider = extensionManager.getExtension(Byter.class, "byterOne@jd");
        Byter byteOneByType = extensionManager.getExtension("byter", "byterOne");
        Assertions.assertNotNull(byteOne);
        Assertions.assertEquals(byteOne, byteOneByProvider);
        Assertions.assertEquals(byteOne, byteOneByType);
    }
}
