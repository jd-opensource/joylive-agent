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
package com.jd.live.agent.governance.security.generator;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.config.CipherConfig;
import com.jd.live.agent.governance.security.CipherGenerator;
import com.jd.live.agent.governance.security.CipherGeneratorFactory;
import com.jd.live.agent.governance.security.CipherGeneratorType;

@Extension("random")
public class RandomGeneratorFactory implements CipherGeneratorFactory {

    public static final CipherGeneratorFactory INSTANCE = new RandomGeneratorFactory();

    @Override
    public CipherGenerator create(CipherConfig config, CipherGeneratorType type) {
        return new RandomGenerator(config.getProperty(CipherConfig.CIPHER_RANDOM_ALGORITHM), type.getSize(config));
    }
}
