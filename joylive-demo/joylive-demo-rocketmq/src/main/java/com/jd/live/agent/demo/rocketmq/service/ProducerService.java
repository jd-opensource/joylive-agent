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
package com.jd.live.agent.demo.rocketmq.service;

import com.jd.live.agent.demo.response.LiveResponse;
import com.jd.live.agent.demo.rocketmq.config.MqConfig;
import com.jd.live.agent.demo.service.HelloService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ProducerService implements HelloService {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private MqConfig mqConfig;

    @Override
    public LiveResponse echo(String str) {
        return rocketMQTemplate.sendAndReceive(mqConfig.getTopic(), str, LiveResponse.class, 10000);
    }

    @Override
    public LiveResponse status(int code) {
        return rocketMQTemplate.sendAndReceive(mqConfig.getTopic(), code, LiveResponse.class, 10000);
    }
}
