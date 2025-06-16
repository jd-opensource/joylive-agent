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
package com.jd.live.agent.governance.policy.service.auth;

import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.governance.invoke.auth.Authenticate;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class TokenPolicy {

    public static final String KEY_TOKEN = "token";

    public static final String KEY_TOKEN_KEY = "token.key";

    @Getter
    @Setter
    private String key;

    @Getter
    @Setter
    private String token;

    private volatile String base64Token;

    public TokenPolicy() {
    }

    public TokenPolicy(String key, String token) {
        this.key = key;
        this.token = token;
    }

    public TokenPolicy(Map<String, String> map) {
        Option option = MapOption.of(map);
        this.key = option.getString(KEY_TOKEN_KEY, Authenticate.KEY_AUTH);
        this.token = option.getString(KEY_TOKEN);
    }

    public boolean isValid() {
        return token != null && !token.isEmpty();
    }

    public String getBase64Token() {
        if (base64Token == null) {
            synchronized (this) {
                if (base64Token == null) {
                    base64Token = token == null || token.isEmpty()
                            ? ""
                            : new String(Base64.getEncoder().encode(token.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
                }
            }
        }
        return base64Token;
    }

}
