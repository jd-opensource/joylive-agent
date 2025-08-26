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
package com.jd.live.agent.plugin.application.springboot.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.security.Cipher;
import com.jd.live.agent.governance.security.CipherDetector;
import com.jd.live.agent.governance.security.CipherFactory;
import com.jd.live.agent.governance.security.detector.DefaultCipherDetector;

public class AbstractPropertyResolverInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractPropertyResolverInterceptor.class);

    private final Cipher cipher;

    private final CipherDetector detector;

    public AbstractPropertyResolverInterceptor(GovernanceConfig config, CipherFactory cipherFactory) {
        this.cipher = cipherFactory.create(config.getCipherConfig());
        this.detector = new DefaultCipherDetector(config.getCipherConfig());
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        if (cipher == null) {
            return;
        }
        Object value = ctx.getArgument(0);
        if (value instanceof String) {
            String text = (String) value;
            if (detector.isEncrypted(text)) {
                try {
                    value = cipher.decrypt(detector.unwrap(text));
                    ctx.setArgument(0, value);
                } catch (Exception e) {
                    logger.error("Error occurs while decoding config, caused by {}", e.getMessage(), e);
                }
            }
        }
    }
}
