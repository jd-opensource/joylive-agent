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
package com.jd.live.agent.core.security.cipher.jasypt;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.security.CipherConfig;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.security.CipherAlgorithm;
import com.jd.live.agent.core.security.CipherAlgorithmContext;
import com.jd.live.agent.core.security.CipherAlgorithmFactory;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;

@Extension("StandardPBE")
public class StandardPBECipherAlgorithmFactory implements CipherAlgorithmFactory {

    private static final Logger logger = LoggerFactory.getLogger(StandardPBECipherAlgorithmFactory.class);

    @Override
    public CipherAlgorithm create(CipherAlgorithmContext ctx) {
        CipherConfig config = ctx.getConfig();
        if (isEmpty(config.getPassword())) {
            logger.error("cipher password is empty, you can set it by environment variable: " + CipherConfig.ENV_CIPHER_PASSWORD);
            return null;
        }
        return new StandardPBECipherAlgorithm(ctx);
    }
}
