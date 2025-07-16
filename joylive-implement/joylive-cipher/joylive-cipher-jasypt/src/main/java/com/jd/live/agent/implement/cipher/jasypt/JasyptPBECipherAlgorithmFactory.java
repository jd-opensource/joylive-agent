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
package com.jd.live.agent.implement.cipher.jasypt;

import com.jd.live.agent.governance.security.CipherAlgorithm;
import com.jd.live.agent.governance.security.CipherAlgorithmFactory;

import java.util.Map;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;

public class JasyptPBECipherAlgorithmFactory implements CipherAlgorithmFactory {

    @Override
    public CipherAlgorithm create(Map<String, String> config) {
        JasyptConfig cfg = new JasyptConfig(config);
        return !validate(cfg) ? null : new JasyptPBECipherAlgorithm(cfg);
    }

    @Override
    public String[] getNames() {
        return new String[]{"PBEWITHHMACSHA512ANDAES_256"};
    }

    private boolean validate(JasyptConfig config) {
        return !isEmpty(config.getPassword());
    }
}
