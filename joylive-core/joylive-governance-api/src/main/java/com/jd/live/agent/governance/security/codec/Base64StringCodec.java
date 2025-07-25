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
package com.jd.live.agent.governance.security.codec;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.exception.CipherException;
import com.jd.live.agent.governance.security.StringCodec;

import java.util.Base64;

@Extension(Base64StringCodec.name)
public class Base64StringCodec implements StringCodec {

    public static final String name = "base64";

    public static final Base64StringCodec INSTANCE = new Base64StringCodec();

    @Override
    public String encode(byte[] data) throws CipherException {
        return Base64.getEncoder().encodeToString(data);
    }

    @Override
    public byte[] decode(String data) throws CipherException {
        try {
            return Base64.getDecoder().decode(data);
        } catch (Exception e) {
            throw new CipherException(e.getMessage(), e);
        }
    }
}
