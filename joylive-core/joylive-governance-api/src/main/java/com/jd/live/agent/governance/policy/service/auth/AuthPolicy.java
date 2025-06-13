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

import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithId;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static com.jd.live.agent.core.util.StringUtils.choose;

/**
 * Auth policy
 *
 * @since 1.2.0
 */
public class AuthPolicy extends PolicyId implements PolicyInheritWithId<AuthPolicy>, Serializable {
    /**
     * The type of the auth policy.
     */
    @Getter
    @Setter
    private String type;

    @Getter
    @Setter
    private Map<String, String> params;

    private volatile transient TokenPolicy tokenPolicy;

    private volatile transient JWTPolicy jwtPolicy;

    public AuthPolicy() {
    }

    public AuthPolicy(String type, Map<String, String> params) {
        this.type = type;
        this.params = params;
    }

    @Override
    public void supplement(AuthPolicy source) {
        if (source == null) {
            return;
        }
        if (type == null) {
            type = source.type;
        }
        if (params == null && source.params != null) {
            params = new HashMap<>(source.params);
        }
    }

    public String getParameter(String key) {
        return params == null || key == null ? null : params.get(key);
    }

    public String getParameter(String key, String defaultValue) {
        return choose(getParameter(key), defaultValue);
    }

    public TokenPolicy getTokenPolicy() {
        if (tokenPolicy == null) {
            synchronized (this) {
                if (tokenPolicy == null) {
                    tokenPolicy = new TokenPolicy(params);
                }
            }
        }
        return tokenPolicy;
    }

    public JWTPolicy getJwtPolicy() {
        if (jwtPolicy == null) {
            synchronized (this) {
                if (jwtPolicy == null) {
                    this.jwtPolicy = new JWTPolicy(params);
                }
            }
        }
        return jwtPolicy;
    }
}
