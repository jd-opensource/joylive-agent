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
package com.jd.live.agent.governance.util;

/**
 * Represents the recovery ratio based on a specified duration and phase.
 * This class calculates the recovery ratio for a given duration within the specified recovery period.
 */
public class RecoverRatio {

    /**
     * The total duration in milliseconds for the recovery period.
     * Defaults to 15000 if a negative value is provided.
     */
    private final int durationMs;

    /**
     * The number of phases in the recovery period.
     * Defaults to 10 if a negative value is provided.
     */
    private final int phase;

    /**
     * The weight used in the recovery ratio calculation.
     * Defaults to 10000 if a non-positive value is provided.
     */
    private final int weight;

    /**
     * The duration of each phase in milliseconds, calculated as {@code phase / durationMs}.
     */
    private final double phaseMs;

    /**
     * The phase ratio, calculated as {@code weight / phase}.
     */
    private final double phaseRatio;

    /**
     * Constructs a new RecoverRatio with the specified duration and phase.
     * The weight defaults to 10000.
     *
     * @param durationMs The total duration in milliseconds for the recovery period.
     * @param phase      The number of phases in the recovery period.
     */
    public RecoverRatio(int durationMs, int phase) {
        this(durationMs, phase, 10000);
    }

    /**
     * Constructs a new RecoverRatio with the specified duration, phase, and weight.
     *
     * @param durationMs The total duration in milliseconds for the recovery period.
     *                   Defaults to 15000 if a negative value is provided.
     * @param phase      The number of phases in the recovery period.
     *                   Defaults to 10 if a negative value is provided.
     * @param weight     The weight used in the recovery ratio calculation.
     *                   Defaults to 10000 if a non-positive value is provided.
     */
    public RecoverRatio(int durationMs, int phase, int weight) {
        this.durationMs = durationMs <= 0 ? 15000 : durationMs;
        this.phase = phase <= 0 ? 10 : phase;
        this.weight = weight <= 0 ? 10000 : weight;
        this.phaseMs = this.phase / (double) this.durationMs;
        this.phaseRatio = ((double) weight) / this.phase;
    }

    /**
     * Calculates the recovery ratio for a given duration within the recovery period.
     *
     * @param duration The duration in milliseconds for which the recovery ratio is calculated.
     * @return The recovery ratio as a double value if the duration is less than the recovery period.
     * Returns {@code null} if the duration is greater than or equal to the recovery period.
     */
    public Double getRatio(long duration) {
        if (duration < durationMs) {
            // [0, phase)
            int part = (int) Math.floor(duration * phaseMs);
            return (part + 1) * phaseRatio / weight;
        }
        return null;
    }
}

