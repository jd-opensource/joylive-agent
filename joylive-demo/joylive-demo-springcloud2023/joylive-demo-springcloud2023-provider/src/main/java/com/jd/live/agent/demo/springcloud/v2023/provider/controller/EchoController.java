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
package com.jd.live.agent.demo.springcloud.v2023.provider.controller;

import com.jd.live.agent.demo.response.LiveLocation;
import com.jd.live.agent.demo.response.LiveResponse;
import com.jd.live.agent.demo.response.LiveTrace;
import com.jd.live.agent.demo.response.LiveTransmission;
import com.jd.live.agent.demo.springcloud.v2023.provider.config.EchoConfig;
import com.jd.live.agent.demo.util.CpuBusyUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ThreadLocalRandom;

@RestController
public class EchoController {

    @Value("${spring.application.name}")
    private String applicationName;

    private final EchoConfig config;

    private final static Logger logger = LoggerFactory.getLogger(EchoController.class);

    @Value("${echo.suffix}")
    private String echoSuffix;

    @Value("${mock.cpuPercent:0.2}")
    private double cpuPercent;

    public EchoController(@Value("${spring.application.name}") String applicationName, EchoConfig config) {
        this.applicationName = applicationName;
        this.config = config;
    }

    @GetMapping("/echo/{str}")
    public LiveResponse echo(@PathVariable String str, HttpServletRequest request) {
        int sleepTime = config.getSleepTime();
        if (sleepTime > 0) {
            if (config.getRandomTime() > 0) {
                sleepTime = sleepTime + ThreadLocalRandom.current().nextInt(config.getRandomTime());
            }
            CpuBusyUtil.busyCompute(sleepTime);
        }
        LiveResponse response = new LiveResponse(echoSuffix == null ? str : str + echoSuffix);
        configure(request, response);
        if (logger.isInfoEnabled()) {
            logger.info("echo str: {}, time: {}", str, System.currentTimeMillis());
        }
        return response;
    }

    @GetMapping("/status/{code}")
    public LiveResponse status(@PathVariable int code, HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(code);
        LiveResponse lr = new LiveResponse(code, null, code);
        configure(request, lr);
        return lr;
    }

    @RequestMapping(value = "/exception", method = {RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST})
    public LiveResponse exception(HttpServletRequest request, HttpServletResponse response) {
        throw new RuntimeException("RuntimeException happened!");
    }

    @RequestMapping(value = "/state/{code}/sleep/{time}", method = {RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST})
    public String state(@PathVariable int code, @PathVariable int time, HttpServletRequest request, HttpServletResponse response) throws InterruptedException {
        if (logger.isInfoEnabled()) {
            logger.info("state code: {}, sleep time: {}, date: {}", code, time, System.currentTimeMillis());
        }
        if (code <= 0) {
            throw new RuntimeException("RuntimeException happened!");
        }
        if (code > 600) {
            response.setStatus(500);
        } else {
            response.setStatus(code);
        }
        double result = 0;
        if (time > 0) {
            long cpuTime = (long) (time * cpuPercent);
            result = CpuBusyUtil.busyCompute(cpuTime);
            Thread.sleep(time - cpuTime);
        }
        LiveResponse lr = new LiveResponse(code, "code:" + code + ", result: " + result, code);
        configure(request, lr);
        return lr.toString();
    }

    private void configure(HttpServletRequest request, LiveResponse response) {
        response.addFirst(new LiveTrace(applicationName, LiveLocation.build(),
                LiveTransmission.build("header", request::getHeader)));
    }

}
