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
package com.jd.live.agent.demo.springcloud.v2021.order.aspect;

import com.jd.live.agent.demo.springcloud.v2021.order.servcice.UserService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class UserValidationAspect {

    private final UserService userService;

    public UserValidationAspect(UserService userService) {
        this.userService = userService;
    }

    @Before("execution(* com.jd.live.agent.demo.springcloud.v2021.order.controller.OrderController.*(..))")
    public void validateUser(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String userCode = (String) args[0];
        if (userCode == null || !userService.userExists(userCode)) {
            throw new IllegalArgumentException("User does not exist or userId is missing");
        }
    }
}