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
package com.jd.live.agent.governance.invoke.loadbalance.randomweight;

import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RandomWeightTest {

    @Test
    void testChoose() {

        List<Food> foods = Arrays.asList(new Food("apple", 1), new Food("watermelon", 10));
        Random random = new Random();
        int apples = 0;
        int watermelons = 0;
        for (int i = 0; i < 100000; i++) {
            Food food = RandomWeight.choose(foods, Food::getWeight, random);
            String name = food == null ? null : food.getName();
            if ("apple".equals(name)) {
                apples++;
            } else if ("watermelon".equals(name)) {
                watermelons++;
            }
        }
        Assertions.assertTrue(apples > 0);
        System.out.printf("apples: %d, watermelons:%d \n", apples, watermelons);
        double ratio = apples * 10.0 / watermelons;
        Assertions.assertTrue(ratio > 0.95 && ratio < 1.05);
    }


    @Getter
    private static class Food {

        private final String name;

        private final int weight;

        Food(String name, int weight) {
            this.name = name;
            this.weight = weight;
        }

    }

}
