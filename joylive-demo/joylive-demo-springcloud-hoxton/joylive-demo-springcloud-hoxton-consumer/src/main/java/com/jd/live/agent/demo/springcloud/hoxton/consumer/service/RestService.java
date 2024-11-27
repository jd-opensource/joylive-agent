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
package com.jd.live.agent.demo.springcloud.hoxton.consumer.service;

import com.jd.live.agent.demo.response.LiveResponse;
import com.jd.live.agent.demo.service.HelloService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@Service
public class RestService implements HelloService {

    @Resource
    private RestTemplate restTemplate;

    @Override
    public LiveResponse echo(String str) {
        return restTemplate.getForObject("http://service-provider/echo/" + str, LiveResponse.class);
    }

    @Override
    public LiveResponse status(int code) {
        return restTemplate.getForObject("http://service-provider/status/" + code, LiveResponse.class);
    }
}
