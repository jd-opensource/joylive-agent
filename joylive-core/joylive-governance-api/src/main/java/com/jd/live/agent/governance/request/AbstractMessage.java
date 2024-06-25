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
package com.jd.live.agent.governance.request;

import com.jd.live.agent.core.Constants;

import java.util.function.Function;


/**
 * An abstract implementation of the {@link Message} interface.
 */
public abstract class AbstractMessage implements Message {

    /**
     * The topic of the message.
     */
    protected String topic;

    /**
     * A function to retrieve header values based on keys.
     */
    protected Function<String, String> func;

    /**
     * The live space ID of the message, lazily initialized.
     */
    protected String liveSpaceId;

    /**
     * The rule ID of the message, lazily initialized.
     */
    protected String ruleId;

    /**
     * The variable of the message, lazily initialized.
     */
    protected String variable;

    /**
     * The lane space ID of the message, lazily initialized.
     */
    protected String laneSpaceId;

    /**
     * The lane of the message, lazily initialized.
     */
    protected String lane;

    public AbstractMessage(String topic, Function<String, String> func) {
        this.topic = topic;
        this.func = func;
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public String getHeader(String key) {
        return key == null || func == null ? null : func.apply(key);
    }

    @Override
    public String getLiveSpaceId() {
        if (liveSpaceId == null) {
            liveSpaceId = getHeader(Constants.LABEL_LIVE_SPACE_ID);
        }
        return liveSpaceId;
    }

    @Override
    public String getRuleId() {
        if (ruleId == null) {
            ruleId = getHeader(Constants.LABEL_RULE_ID);
        }
        return ruleId;
    }

    @Override
    public String getVariable() {
        if (variable == null) {
            variable = getHeader(Constants.LABEL_VARIABLE);
        }
        return variable;
    }

    @Override
    public String getLaneSpaceId() {
        if (laneSpaceId == null) {
            laneSpaceId = getHeader(Constants.LABEL_LANE_SPACE_ID);
        }
        return laneSpaceId;
    }

    @Override
    public String getLane() {
        if (lane == null) {
            lane = getHeader(Constants.LABEL_LANE);
        }
        return lane;
    }
}
