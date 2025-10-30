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
import com.jd.live.agent.governance.policy.PolicyId;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class TokenPolicy extends PolicyId implements AuthStrategy, Serializable {

    private static final String KEY_TOKEN = "token";
    private static final String KEY_TOKEN_KEY = "key";
    private static final String KEY_TOKEN_START_TIME = "startTime";
    private static final String KEY_TOKEN_END_TIME = "endTime";

    @Getter
    @Setter
    private String key;

    @Getter
    @Setter
    private String token;

    @Getter
    @Setter
    private long startTime;

    @Getter
    @Setter
    private long endTime;

    @Getter
    private transient boolean valid;

    private transient String encoded;

    public TokenPolicy() {
    }

    public TokenPolicy(String key, String token) {
        this.key = key;
        this.token = token;
        cache();
    }

    public TokenPolicy(Map<String, String> map) {
        Option option = MapOption.of(map);
        this.key = option.getString(KEY_TOKEN_KEY, Authenticate.KEY_AUTH);
        this.token = option.getString(KEY_TOKEN);
        this.startTime = option.getLong(KEY_TOKEN_START_TIME, 0L);
        this.endTime = option.getLong(KEY_TOKEN_END_TIME, Long.MAX_VALUE);
        cache();
    }

    public String getBase64Token() {
        return encoded;
    }

    public String getKeyOrDefault(final String defaultKey) {
        return key == null || key.isEmpty() ? defaultKey : key;
    }

    @Override
    public String getAuthScheme() {
        return "Basic";
    }

    public boolean isEffective(long time) {
        return valid && time >= startTime && time < endTime;
    }

    protected void cache() {
        endTime = endTime <= 0 ? Long.MAX_VALUE : endTime;
        valid = token != null && !token.isEmpty();
        encoded = token == null || token.isEmpty()
                ? ""
                : new String(Base64.getEncoder().encode(token.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }
}
