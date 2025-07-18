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

import java.util.Base64;

public class Base64Generator extends StringGenerator {

    public Base64Generator(String salt, int size) {
        super(salt, size);
    }

    @Override
    protected byte[] setupSalt(byte[] bytes, int size) {
        return super.setupSalt(Base64.getDecoder().decode(bytes), size);
    }
}
