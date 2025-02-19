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
package com.jd.live.agent.demo.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CpuBusyUtil {

    private static final int cpuCores = Runtime.getRuntime().availableProcessors();

    private static final ExecutorService executor = Executors.newFixedThreadPool(cpuCores);

    public static void multiThreadBusyCompute(long durationMillis) {
        multiThreadBusyCompute(durationMillis, cpuCores);
    }

    public static void multiThreadBusyCompute(long durationMillis, int threadCount) {
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> busyCompute(durationMillis));
        }
    }

    public static double busyCompute(long durationMillis) {
        long startTime = System.nanoTime();
        long durationNanos = durationMillis * 1_000_000;

        double result = 0;
        while (System.nanoTime() - startTime < durationNanos) {
            for (int i = 0; i < 1000; i++) {
                result += Math.log(i + 1) * Math.sqrt(i);
            }
        }
        return result;
    }
}
