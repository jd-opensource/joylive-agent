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
package com.jd.live.agent.demo.springcloud.v2022.consumer.controller;

import com.jd.live.agent.demo.response.LiveLocation;
import com.jd.live.agent.demo.response.LiveResponse;
import com.jd.live.agent.demo.response.LiveTrace;
import com.jd.live.agent.demo.response.LiveTransmission;
import com.jd.live.agent.demo.springcloud.v2022.consumer.service.FeignService;
import com.jd.live.agent.demo.springcloud.v2022.consumer.service.ReactiveService;
import com.jd.live.agent.demo.springcloud.v2022.consumer.service.RestService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EchoController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private RestService restService;

    @Autowired
    private FeignService feignService;

    @Autowired
    private ReactiveService reactiveService;

    @GetMapping({"/echo-rest/{str}", "/echo/{str}"})
    public LiveResponse echoRest(@PathVariable String str, HttpServletRequest request) {
        LiveResponse response = restService.echo(str);
        addTrace(request, response);
        return response;
    }

    @GetMapping("/echo-feign/{str}")
    public LiveResponse echoFeign(@PathVariable String str, HttpServletRequest request) {
        LiveResponse response = feignService.echo(str);
        addTrace(request, response);
        return response;
    }

    @GetMapping({"/echo-reactive/{str}"})
    public LiveResponse echoReactive(@PathVariable String str, HttpServletRequest request) {
        LiveResponse response = reactiveService.echo(str);
        addTrace(request, response);
        return response;
    }

    @GetMapping("/status-feign/{code}")
    public LiveResponse echoFeign(@PathVariable int code, HttpServletRequest request) {
        LiveResponse response = feignService.status(code);
        addTrace(request, response);
        return response;
    }

    @GetMapping({"/status-rest/{code}"})
    public LiveResponse statusRest(@PathVariable int code, HttpServletRequest request) {
        LiveResponse response = restService.status(code);
        addTrace(request, response);
        return response;
    }

    @GetMapping({"/status-reactive/{code}"})
    public LiveResponse statusReactive(@PathVariable int code, HttpServletRequest request) {
        LiveResponse response = reactiveService.status(code);
        addTrace(request, response);
        return response;
    }

    private void addTrace(HttpServletRequest request, LiveResponse response) {
        response.addFirst(new LiveTrace(applicationName, LiveLocation.build(),
                LiveTransmission.build("header", request::getHeader)));
    }

}
