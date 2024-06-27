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

import com.jd.live.agent.demo.springcloud.v3.order.entity.Order;
import com.jd.live.agent.demo.springcloud.v3.order.servcice.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public Order getOrderById(@RequestParam Long userId, @PathVariable Long id) {
        return orderService.getById(id);
    }

    @GetMapping("/user/{userId}")
    public List<Order> getOrdersByUserId(@PathVariable Long userId) {
        return orderService.getOrdersByUserId(userId);
    }

    @PostMapping
    public boolean createOrder(@RequestParam Long userId, @RequestBody Order order) {
        return orderService.save(order);
    }

    @PutMapping("/{id}")
    public boolean updateOrder(@RequestParam Long userId, @PathVariable Long id, @RequestBody Order order) {
        order.setId(id);
        return orderService.updateById(order);
    }

    @DeleteMapping("/{id}")
    public boolean deleteOrder(@RequestParam Long userId, @PathVariable Long id) {
        return orderService.removeById(id);
    }
}
