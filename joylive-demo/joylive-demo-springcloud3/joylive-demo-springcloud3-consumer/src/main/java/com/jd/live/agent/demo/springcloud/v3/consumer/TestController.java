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
package com.jd.live.agent.demo.springcloud.v3.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@RestController
public class TestController {

    @Autowired
    @LoadBalanced
    private WebClient.Builder webClientBuilder;

    @Autowired
    @Qualifier("restTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private SpringCloud3Consumer.EchoService echoService;

    @Autowired
    private DiscoveryClient discoveryClient;

    @GetMapping("/echo-webclient/{str}")
    public Mono<String> webclient(@PathVariable String str) {
        return webClientBuilder.build().get().uri("http://service-provider/echo/" + str)
                .acceptCharset(StandardCharsets.UTF_8)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class);
    }

    @GetMapping("/echo-rest/{str}")
    public String rest(@PathVariable String str,
                       @RequestHeader(value = "x-live-space-id", required = false) String liveSpaceId,
                       @RequestHeader(value = "x-live-rule-id", required = false) String ruleId,
                       @RequestHeader(value = "x-live-uid", required = false) String uid,
                       @RequestHeader(value = "x-lane-space-id", required = false) String laneSpaceId,
                       @RequestHeader(value = "x-lane-code", required = false) String laneCode) {
        String message = restTemplate.getForObject("http://service-provider/echo/" + str,
                String.class);
        return new StringBuilder("spring-consumer:\n").
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
                append("}\n\n").
                append(message).toString();
    }

    @GetMapping("/echo-feign/{str}")
    public String feign(@PathVariable String str,
                        @RequestHeader(value = "x-live-space-id", required = false) String liveSpaceId,
                        @RequestHeader(value = "x-live-rule-id", required = false) String ruleId,
                        @RequestHeader(value = "x-live-uid", required = false) String uid,
                        @RequestHeader(value = "x-lane-space-id", required = false) String laneSpaceId,
                        @RequestHeader(value = "x-lane-code", required = false) String laneCode) {
        String message = echoService.echo(str);
        return new StringBuilder("spring-consumer:\n").
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
                append("}\n\n").
                append(message).toString();
    }

    @GetMapping("/services/{service}")
    public Object client(@PathVariable String service) {
        return discoveryClient.getInstances(service);
    }

    @GetMapping("/services")
    public Object services() {
        return discoveryClient.getServices();
    }

}
