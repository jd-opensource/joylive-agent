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
package com.jd.live.agent.demo.springcloud.v2021.order.servcice.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jd.live.agent.demo.springcloud.v2021.order.entity.Order;
import com.jd.live.agent.demo.springcloud.v2021.order.mapper.OrderMapper;
import com.jd.live.agent.demo.springcloud.v2021.order.servcice.OrderService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Override
    @Cacheable(value = "order", key = "#id")
    public Order getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    public List<Order> getOrdersByUserCode(String userCode, int page, int size) {
        Page<Order> orderPage = Page.of(page <= 0 ? 1 : page, size <= 0 ? 10 : size);
        return lambdaQuery().orderByDesc(Order::getId).eq(Order::getUserCode, userCode).list(orderPage);
    }

    @Override
    @CacheEvict(value = "order", key = "#order.id")
    public boolean save(Order order) {
        return super.save(order);
    }

    @Override
    @CacheEvict(value = "order", key = "#order.id")
    public boolean updateById(Order order) {
        return super.updateById(order);
    }

    @Override
    @CacheEvict(value = "order", key = "#id")
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }
}
