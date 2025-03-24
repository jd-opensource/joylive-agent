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
     * Randomly selects an element from a list based on weights determined by a weight function.
     *
     * @param targets    The list of elements to select from.
     * @param weightFunc A function that provides the weight for each element.
     * @param <T>        The generic type of the elements.
     * @return The elected candidate, or null if the list is empty or null
     */
    @SuppressWarnings("unchecked")
    public static <T> Candidate<T> elect(List<T> targets, Function<T, Integer> weightFunc) {
        Candidate<T>[] candidates = new Candidate[targets == null ? 0 : targets.size()];
        if (targets != null) {
            int index = 0;
            for (T target : targets) {
                candidates[index] = new Candidate<>(target, index, weightFunc.apply(target));
                index++;
            }
        }
        return elect(candidates);
    }

    /**
     * Elects a candidate from the provided array of candidates based on their weights.
     * This method implements a weighted random selection algorithm to choose a candidate.
     *
     * @param <T>        the type of the candidate
     * @param candidates the array of candidates to elect from
     * @return the elected candidate, or {@code null} if the candidates array is {@code null} or empty
     * @throws IllegalArgumentException if any candidate in the array is {@code null}
     */
    public static <T> Candidate<T> elect(Candidate<T>[] candidates) {
        int size = candidates == null ? 0 : candidates.length;
        switch (size) {
            case 0:
                return null;
            case 1:
                return candidates[0];
            default:
                int totalWeight = 0;
                int halfWeight = 0;
                int half = (int) Math.ceil(size * 1.0 / 2) - 1;

                boolean uniformWeights = true;
                int firstWeight = candidates[0].getWeight();

                // Calculate total weight and check for uniform weights
                Candidate<T> candidate;
                for (int i = 0; i < size; i++) {
                    candidate = candidates[i];
                    totalWeight += candidate.getWeight();
                    if (i == half) {
                        halfWeight = totalWeight;
                    }
                    if (uniformWeights && candidate.getWeight() != firstWeight) {
                        uniformWeights = false;
                    }
                }

                // If weights are uniform or total weight is zero, select randomly
                if (uniformWeights || totalWeight <= 0) {
                    int index = ThreadLocalRandom.current().nextInt(size);
                    return candidates[index];
                }

                // Select based on weight
                int random = ThreadLocalRandom.current().nextInt(totalWeight);
                int start = random >= halfWeight ? half : 0;
                int weight = start == 0 ? 0 : halfWeight;
                for (int i = start; i < size; i++) {
                    candidate = candidates[i];
                    weight += candidate.getWeight();
                    if (weight >= random) {
                        return candidate;
                    }
                }
                // Fallback, though this should not be reached
                return candidates[ThreadLocalRandom.current().nextInt(size)];
        }
    }
}

