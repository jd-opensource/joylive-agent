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

import com.jd.live.agent.governance.invoke.loadbalance.Candidate;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * A utility class for random selection based on weighted probabilities.
 */
public class RandomWeight {

    /**
     * Randomly selects an element from a list based on weights determined by a weight function.
     *
     * @param targets    The list of elements to select from.
     * @param weightFunc A function that provides the weight for each element.
     * @param <T>        The generic type of the elements.
     * @return The selected element, or null if the list is empty or only contains elements with non-positive weights.
     */
    public static <T> T choose(List<T> targets, Function<T, Integer> weightFunc) {
        Candidate<T> candidate = elect(targets, weightFunc);
        return candidate == null ? null : candidate.getTarget();
    }

    /**
     * Randomly selects an element from a list based on weights determined by a weight function, given the total weight.
     *
     * @param targets    The list of elements to select from.
     * @param weightFunc A function that provides the weight for each element.
     * @param weights    The total weight of all elements.
     * @param <T>        The generic type of the elements.
     * @return The selected element, or null if the list is empty or only contains elements with non-positive weights.
     */
    public static <T> T choose(List<T> targets, Function<T, Integer> weightFunc, int weights) {
        int size = targets == null ? 0 : targets.size();
        switch (size) {
            case 0:
                return null;
            case 1:
                return targets.get(0);
            default:
                if (weights > 0) {
                    int weight = 0;
                    int random = ThreadLocalRandom.current().nextInt(weights);
                    for (T target : targets) {
                        weight += Math.max(weightFunc.apply(target), 0);
                        if (weight >= random) {
                            return target;
                        }
                    }
                }
                return targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
        }
    }

    /**
     * Randomly selects an element from a list based on weights determined by a weight function.
     *
     * @param targets    The list of elements to select from.
     * @param weightFunc A function that provides the weight for each element.
     * @param <T>        The generic type of the elements.
     * @return The elected candidate, or null if the list is empty or null
     */
    public static <T> Candidate<T> elect(List<T> targets, Function<T, Integer> weightFunc) {
        int size = targets == null ? 0 : targets.size();
        switch (size) {
            case 0:
                return null;
            case 1:
                return new Candidate<>(targets.get(0), 0);
            default:
                int totalWeight = 0;
                int halfWeight = 0;
                int half = size / 2;
                int pos = 0;

                boolean uniformWeights = true;
                int firstWeight = -1;
                int weight;

                // Calculate total weight and check for uniform weights
                for (T target : targets) {
                    weight = Math.max(weightFunc.apply(target), 0);
                    totalWeight += weight;
                    if (++pos == half) {
                        halfWeight = totalWeight;
                    }
                    if (pos == 1) {
                        firstWeight = weight;
                    } else if (weight != firstWeight) {
                        uniformWeights = false;
                    }
                }

                // If weights are uniform or total weight is zero, select randomly
                if (uniformWeights || totalWeight <= 0) {
                    int index = ThreadLocalRandom.current().nextInt(targets.size());
                    return new Candidate<>(targets.get(index), index);
                }

                // Select based on weight
                int random = ThreadLocalRandom.current().nextInt(totalWeight);
                int start = random >= halfWeight ? half : 0;
                int cumulativeWeight = start == 0 ? 0 : halfWeight;
                List<T> halfTargets = start == 0 ? targets : targets.subList(start, targets.size());
                for (T target : halfTargets) {
                    cumulativeWeight += Math.max(weightFunc.apply(target), 0);
                    if (cumulativeWeight >= random) {
                        return new Candidate<>(target, start);
                    }
                    start++;
                }
                // Fallback, though this should not be reached
                int fallbackIndex = ThreadLocalRandom.current().nextInt(targets.size());
                return new Candidate<>(targets.get(fallbackIndex), fallbackIndex);
        }
    }
}

