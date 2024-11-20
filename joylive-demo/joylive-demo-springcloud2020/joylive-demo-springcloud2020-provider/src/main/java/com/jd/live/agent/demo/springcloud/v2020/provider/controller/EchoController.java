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
package com.jd.live.agent.demo.springcloud.v2020.provider.controller;

import com.jd.live.agent.demo.response.LiveLocation;
import com.jd.live.agent.demo.response.LiveResponse;
import com.jd.live.agent.demo.response.LiveTrace;
import com.jd.live.agent.demo.response.LiveTransmission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@RestController
public class EchoController {

    @Value("${spring.application.name}")
    private String applicationName;

    @GetMapping("/echo/{str}")
    public LiveResponse echo(@PathVariable String str, HttpServletRequest request) {
        try {
            String waitTime = Optional.ofNullable(System.getenv("RESPONSE_WAIT_TIME")).orElse("2000");
            Thread.sleep(Integer.parseInt(waitTime) + ThreadLocalRandom.current().nextInt(1000));
        } catch (InterruptedException ignore) {
        }
        LiveResponse response = new LiveResponse(str);
        configure(request, response);
        return response;
    }

    @RequestMapping(value = "/status/{code}", method = {RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST})
    public LiveResponse status(@PathVariable int code, HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(code);
        LiveResponse lr = new LiveResponse(code, null, code);
        configure(request, lr);
        return lr;
    }

    @RequestMapping(value = "/sleep/{millis}", method = {RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST})
    public LiveResponse sleep(@PathVariable int millis, HttpServletRequest request, HttpServletResponse response) throws InterruptedException {
        Thread.sleep(millis);
        LiveResponse lr = new LiveResponse(200, null, millis);
        configure(request, lr);
        return lr;
    }

    @RequestMapping(value = "/exception", method = {RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST})
    public LiveResponse exception(HttpServletRequest request, HttpServletResponse response) {
        throw new RuntimeException("RuntimeException happened!");
    }

    private void configure(HttpServletRequest request, LiveResponse response) {
        response.addFirst(new LiveTrace(applicationName, LiveLocation.build(),
                LiveTransmission.build("header", request::getHeader)));
    }

}
