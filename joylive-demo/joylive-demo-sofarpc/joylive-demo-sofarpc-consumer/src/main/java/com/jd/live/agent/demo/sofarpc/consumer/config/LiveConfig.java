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
package com.jd.live.agent.demo.sofarpc.consumer.config;

import com.jd.live.agent.demo.util.LiveTransmission;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Getter
@Setter
@Component
@ImportResource("classpath:sofa-rpc.xml")
@ConfigurationProperties(prefix = "live")
public class LiveConfig {

    private String liveSpaceId;
    private String ruleId;
    private String uid;
    private String laneSpaceId;
    private String laneCode;

    public void transmit(BiConsumer<String, String> consumer) {
        accept(consumer, LiveTransmission.X_LIVE_SPACE_ID, liveSpaceId);
        accept(consumer, LiveTransmission.X_LIVE_RULE_ID, ruleId);
        accept(consumer, LiveTransmission.X_LIVE_UID, uid);
        accept(consumer, LiveTransmission.X_LANE_SPACE_ID, laneSpaceId);
        accept(consumer, LiveTransmission.X_LANE_CODE, laneCode);
    }

    private void accept(BiConsumer<String, String> consumer, String key, String value) {
        if (consumer != null && key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            consumer.accept(key, value);
        }
    }
}
