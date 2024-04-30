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
package com.jd.live.agent.governance.rule.tag;

import com.jd.live.agent.core.util.tag.Tag;
import com.jd.live.agent.governance.rule.OpType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * The {@code TagCondition} class extends the {@code Tag} class to include an operation type and an optional type field.
 * It encapsulates the condition that must be met by a tag value for a match to occur.
 * <p>
 * This class utilizes Lombok's {@code @Getter} and {@code @Setter} annotations to automatically generate getter and
 * setter methods for its fields, reducing boilerplate code.
 * <p>
 * The {@code match} methods are used to determine if a given target value or a list of target values meet the condition
 * specified by this {@code TagCondition}.
 */
@Getter
@Setter
public class TagCondition extends Tag {

    private OpType opType = OpType.EQUAL; // The operation type for matching the condition.

    private String type; // An optional type field for additional condition criteria.

    /**
     * Default constructor for {@code TagCondition}.
     */
    public TagCondition() {
        super();
    }

    /**
     * Constructs a {@code TagCondition} with the specified key, value, and operation type.
     *
     * @param key    The key of the tag.
     * @param value  The list of values for the tag.
     * @param opType The operation type to be used for matching the condition.
     */
    public TagCondition(String key, List<String> value, OpType opType) {
        super(key, value);
        this.opType = opType;
    }

    /**
     * Constructs a {@code TagCondition} with the specified key, value, operation type, and type.
     *
     * @param key    The key of the tag.
     * @param value  The list of values for the tag.
     * @param opType The operation type to be used for matching the condition.
     * @param type   The type of the condition for additional matching criteria.
     */
    public TagCondition(String key, List<String> value, OpType opType, String type) {
        super(key, value);
        this.opType = opType;
        this.type = type;
    }

    /**
     * Sets the key for this tag condition.
     *
     * @param key The key to be set for the tag condition.
     */
    @Override
    public void setKey(String key) {
        super.setKey(key);
    }

    /**
     * Sets the values for this tag condition.
     *
     * @param values The list of values to be set for the tag condition.
     */
    @Override
    public void setValues(List<String> values) {
        super.setValues(values);
    }

    /**
     * Checks if the target value matches the tag condition.
     *
     * @param target The target value to be matched against the tag condition.
     * @return {@code true} if the target value satisfies the tag condition; {@code false} otherwise.
     */
    public boolean match(String target) {
        return opType.isMatch(values, target);
    }

    /**
     * Checks if any value in the list of target values matches the tag condition.
     *
     * @param targets The list of target values to be matched against the tag condition.
     * @return {@code true} if at least one value in the list satisfies the tag condition; {@code false} otherwise.
     */
    public boolean match(List<String> targets) {
        if (targets != null) {
            for (String target : targets) {
                if (opType.isMatch(values, target)) {
                    return true;
                }
            }
        }
        return false;
    }
}

