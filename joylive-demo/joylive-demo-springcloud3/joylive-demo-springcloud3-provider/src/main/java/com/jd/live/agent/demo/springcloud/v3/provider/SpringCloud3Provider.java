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
package com.jd.live.agent.demo.springcloud.v3.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@EnableDiscoveryClient
@SpringBootApplication
public class SpringCloud3Provider {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloud3Provider.class, args);
    }

    @RestController
    class EchoController {

        @GetMapping("/echo/{message}")
        public String echo(@PathVariable String message,
                           @RequestHeader(value = "x-live-space-id", required = false) String liveSpaceId,
                           @RequestHeader(value = "x-live-rule-id", required = false) String ruleId,
                           @RequestHeader(value = "x-live-uid", required = false) String uid,
                           @RequestHeader(value = "x-lane-space-id", required = false) String laneSpaceId,
                           @RequestHeader(value = "x-lane-code", required = false) String laneCode) {
            return new StringBuilder().append("spring-provider:").append(message).append('\n').
                    append("  header:{").
                    append("x-live-space-id=").append(liveSpaceId).
                    append(", x-live-rule-id=").append(ruleId).
                    append(", x-live-uid=").append(uid).
                    append(", x-lane-space-id=").append(laneSpaceId).
                    append(", x-lane-code=").append(laneCode).
                    append("}\n").
                    append("  location:{").
                    append("liveSpaceId=").append(System.getProperty("x-live-space-id")).
                    append(", unit=").append(System.getProperty("x-live-unit")).
                    append(", cell=").append(System.getProperty("x-live-cell")).
                    append(", laneSpaceId=").append(System.getProperty("x-lane-space-id")).
                    append(", lane=").append(System.getProperty("x-lane-code")).
                    append("}\n").toString();
        }
    }

}
