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
package com.jd.live.agent.governance.policy.live;

import com.jd.live.agent.core.parser.json.DeserializeConverter;
import com.jd.live.agent.core.parser.json.JsonAlias;
import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.core.util.trie.Path;
import com.jd.live.agent.core.util.trie.PathMatchType;
import com.jd.live.agent.governance.policy.HttpScope;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.live.converter.ScopeDeserializer;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

import static com.jd.live.agent.governance.policy.live.LiveVariableRule.QUERY_VARIABLE;

public class LivePath extends PolicyId implements Path {

    @Getter
    @Setter
    private String path;

    @Getter
    @Setter
    private PathMatchType matchType = PathMatchType.PREFIX;

    @Getter
    @Setter
    @JsonAlias("unitRuleId")
    private String ruleId;

    @Getter
    @Setter
    @JsonAlias("unitRuleName")
    private String ruleName;

    @Getter
    @Setter
    @JsonAlias("variableAccessor")
    private boolean customVariableSource;

    @Getter
    @Setter
    private String variable;

    @Getter
    @Setter
    @JsonAlias("accessor")
    private String variableSource;

    @Getter
    @Setter
    private boolean bizVariableEnabled;

    @Getter
    @Setter
    private String bizVariableName;

    @Getter
    @Setter
    @DeserializeConverter(ScopeDeserializer.class)
    private HttpScope bizVariableScope;

    @Getter
    @Setter
    private List<LiveVariableRule> bizVariableRules;

    private final transient Cache<String, LiveVariableRule> variableRuleCache = new MapCache<>(new ListBuilder<>(() -> bizVariableRules, LiveVariableRule::getValue));

    public LiveVariableRule getVariableRule(String value) {
        return variableRuleCache.get(value);
    }

    protected void supplementVariable() {
        if (bizVariableRules != null) {
            for (LiveVariableRule variableRule : bizVariableRules) {
                variableRule.supplement(() -> uri.parameter(QUERY_VARIABLE, variableRule.getValue()));
            }
        }

    }

    public void cache() {
        getVariableRule("");
    }
}
