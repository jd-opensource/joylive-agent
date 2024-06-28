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
package com.jd.live.agent.demo.springcloud.v3.order.controller;

import com.jd.live.agent.demo.response.LiveLocation;
import com.jd.live.agent.demo.response.LiveResponse;
import com.jd.live.agent.demo.response.LiveTrace;
import com.jd.live.agent.demo.response.LiveTransmission;
import com.jd.live.agent.demo.springcloud.v3.order.entity.Order;
import com.jd.live.agent.demo.springcloud.v3.order.servcice.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Value("${spring.application.name}")
    private String applicationName;

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public LiveResponse getOrderById(@RequestParam("user") Long userId,
                                     @PathVariable Long id,
                                     HttpServletRequest request) {
        LiveResponse response = new LiveResponse(orderService.getById(id));
        response.addFirst(new LiveTrace(applicationName, LiveLocation.build(),
                LiveTransmission.build("header", request::getHeader)));
        return response;
    }

    @GetMapping
    public LiveResponse getOrdersByUserId(@RequestParam Long userId,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          HttpServletRequest request) {
        LiveResponse response = new LiveResponse(orderService.getOrdersByUserId(userId, page, size));
        response.addFirst(new LiveTrace(applicationName, LiveLocation.build(),
                LiveTransmission.build("header", request::getHeader)));
        return response;
    }

    @PostMapping
    public LiveResponse createOrder(@RequestParam("user") Long userId,
                                    @RequestBody Order order,
                                    HttpServletRequest request) {
        order.setUserId(userId);
        boolean saved = orderService.save(order);
        LiveResponse response = saved ?
                new LiveResponse(LiveResponse.SUCCESS, "SUCCESS") :
                new LiveResponse(LiveResponse.ERROR, "ERROR");
        response.addFirst(new LiveTrace(applicationName, LiveLocation.build(),
                LiveTransmission.build("header", request::getHeader)));
        return response;
    }

    @PutMapping("/{id}")
    public LiveResponse updateOrder(@RequestParam("user") Long userId,
                                    @PathVariable Long id,
                                    @RequestBody Order order,
                                    HttpServletRequest request) {
        order.setId(id);
        boolean updated = orderService.updateById(order);
        LiveResponse response = updated ?
                new LiveResponse(LiveResponse.SUCCESS, "SUCCESS") :
                new LiveResponse(LiveResponse.NOT_FOUND, "NOT_FOUND");
        response.addFirst(new LiveTrace(applicationName, LiveLocation.build(),
                LiveTransmission.build("header", request::getHeader)));
        return response;
    }

    @DeleteMapping("/{id}")
    public LiveResponse deleteOrder(@RequestParam("user") Long userId,
                                    @PathVariable Long id,
                                    HttpServletRequest request) {
        boolean removed = orderService.removeById(id);
        LiveResponse response = removed ?
                new LiveResponse(LiveResponse.SUCCESS, "SUCCESS") :
                new LiveResponse(LiveResponse.NOT_FOUND, "NOT_FOUND");
        response.addFirst(new LiveTrace(applicationName, LiveLocation.build(),
                LiveTransmission.build("header", request::getHeader)));
        return response;
    }
}
