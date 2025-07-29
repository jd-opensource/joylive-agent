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
package com.jd.live.agent.core.util;

import java.util.concurrent.TimeUnit;

/**
 * High-performance time unit conversion utilities.
 * Optimized alternatives to {@link java.util.concurrent.TimeUnit} methods.
 */
public class TimeUtils {

    private static final long NANOS_PER_MICRO = 1000L;
    private static final long MICROS_PER_MILLI = 1000L;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long MINUTES_PER_HOUR = 60L;
    private static final long HOURS_PER_DAY = 24L;

    private static final long NANOS_PER_MILLI = NANOS_PER_MICRO * MICROS_PER_MILLI;
    private static final long NANOS_PER_SECOND = NANOS_PER_MILLI * MILLIS_PER_SECOND;
    private static final long MICROS_PER_SECOND = MICROS_PER_MILLI * MILLIS_PER_SECOND;
    private static final long MICROS_PER_MINUTE = MICROS_PER_SECOND * SECONDS_PER_MINUTE;
    private static final long MICROS_PER_HOUR = MICROS_PER_SECOND * SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
    private static final long MICROS_PER_DAY = MICROS_PER_SECOND * SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY;

    private TimeUtils() {
    }

    /**
     * Converts nanoseconds to microseconds (division operation)
     *
     * @param nanos duration in nanoseconds
     * @return duration in microseconds
     */
    public static long nanosToMicros(long nanos) {
        return nanos / NANOS_PER_MICRO;
    }

    /**
     * Converts microseconds to nanoseconds (multiplication operation)
     *
     * @param micros duration in microseconds
     * @return duration in nanoseconds
     */
    public static long microsToNanos(long micros) {
        return micros * NANOS_PER_MICRO;
    }

    /**
     * Converts milliseconds to microseconds
     *
     * @param millis duration in milliseconds
     * @return duration in microseconds
     */
    public static long millisToMicros(long millis) {
        return millis * MICROS_PER_MILLI;
    }

    /**
     * Converts seconds to microseconds
     *
     * @param seconds duration in seconds
     * @return duration in microseconds
     */
    public static long secondsToMicros(long seconds) {
        return seconds * MICROS_PER_SECOND;
    }

    /**
     * Optimized TimeUnit conversion to microseconds
     *
     * @param duration time duration
     * @param unit     source time unit
     * @return duration in microseconds
     * @see java.util.concurrent.TimeUnit#toMicros(long)
     */
    public static long toMicros(long duration, TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:
                return duration / NANOS_PER_MICRO;
            case MICROSECONDS:
                return duration;
            case MILLISECONDS:
                return duration * MICROS_PER_MILLI;
            case SECONDS:
                return duration * MICROS_PER_SECOND;
            case MINUTES:
                return duration * MICROS_PER_MINUTE;
            case HOURS:
                return duration * MICROS_PER_HOUR;
            case DAYS:
                return duration * MICROS_PER_DAY;
            default:
                // fallback to TimeUnit implementation
                return unit.toMicros(duration);
        }
    }
}
