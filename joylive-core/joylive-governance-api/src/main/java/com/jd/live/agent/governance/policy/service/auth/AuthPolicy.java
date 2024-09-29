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
import com.jd.live.agent.governance.policy.PolicyInherit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Auth policy
 *
 * @since 1.2.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthPolicy extends PolicyId implements PolicyInherit.PolicyInheritWithId<AuthPolicy>, Serializable {

    /**
     * The type of the auth policy.
     */
    private String type;

    private Map<String, String> params;

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
        return params == null || key == null ? defaultValue : params.getOrDefault(key, defaultValue);
    }
}
