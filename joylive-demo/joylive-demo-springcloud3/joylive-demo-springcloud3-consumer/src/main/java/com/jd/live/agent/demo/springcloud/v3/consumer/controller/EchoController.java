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
package com.jd.live.agent.demo.springcloud.v3.consumer.controller;

import com.jd.live.agent.demo.springcloud.v3.consumer.service.FeignService;
import com.jd.live.agent.demo.springcloud.v3.consumer.service.RestService;
import com.jd.live.agent.demo.util.EchoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
public class EchoController {

    @Resource
    private RestService restService;

    @Resource
    private FeignService feignService;

    @GetMapping({"/echo-rest/{str}","/echo/{str}"})
    public String echoRest(@PathVariable String str, HttpServletRequest request) {
        String message = restService.echo(str);
        return new EchoResponse("spring-consumer", "header", request::getHeader, message).toString();
    }

    @GetMapping("/echo-feign/{str}")
    public String echoFeign(@PathVariable String str, HttpServletRequest request) {
        String message = feignService.echo(str);
        return new EchoResponse("spring-consumer", "header", request::getHeader, message).toString();
    }

}
