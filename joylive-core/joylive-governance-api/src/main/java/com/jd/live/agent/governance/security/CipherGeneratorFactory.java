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
package com.jd.live.agent.governance.security;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.config.CipherConfig;

/**
 * Factory interface for producing {@link CipherGenerator} instances.
 */
@Extensible("SaltFactory")
public interface CipherGeneratorFactory {

    /**
     * Creates a new {@link CipherGenerator} of the specified type configured with the given parameters.
     *
     * @param config the cipher configuration (non-null)
     * @param type the generator type to create (non-null)
     * @return a new configured cipher generator instance (non-null)
     */
    CipherGenerator create(CipherConfig config, CipherGeneratorType type);
}
