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
package com.jd.live.agent.governance.policy.service.fault;

import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithIdGen;
import com.jd.live.agent.governance.rule.ConditionalMatcher;
import com.jd.live.agent.governance.rule.RelationType;
import com.jd.live.agent.governance.rule.tag.TagCondition;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * Fault injection policy
 *
 * @since 1.4.0
 */
@Getter
@Setter
public class FaultInjectionPolicy extends PolicyId implements PolicyInheritWithIdGen<FaultInjectionPolicy>,
        ConditionalMatcher<TagCondition>, Serializable {

    private String name;

    private String type;

    private List<TagCondition> conditions;

    private RelationType relationType;

    private int percent;

    private long delayTimeMs;

    private int errorCode;

    private String errorMsg;

    public FaultInjectionPolicy() {
    }

    public FaultInjectionPolicy(String name) {
        this.name = name;
    }

    @Override
    public void supplement(FaultInjectionPolicy source) {
        if (source == null) {
            return;
        }
        if (type == null) {
            type = source.type;
        }
        if (conditions == null) {
            conditions = source.conditions;
        }
        if (relationType == null) {
            relationType = source.relationType;
        }
        if (percent == 0) {
            percent = source.percent;
        }
        if (delayTimeMs == 0) {
            delayTimeMs = source.delayTimeMs;
        }
        if (errorCode == 0) {
            errorCode = source.errorCode;
        }
        if (errorMsg == null) {
            errorMsg = source.errorMsg;
        }
    }
}
