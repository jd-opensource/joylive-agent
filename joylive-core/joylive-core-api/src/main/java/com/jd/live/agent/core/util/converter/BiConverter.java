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
package com.jd.live.agent.core.util.converter;

/**
 * A converter that transforms two input values into a result.
 *
 * @param <P1> the type of first input parameter
 * @param <P2> the type of second input parameter
 * @param <V> the type of conversion result
 */
public interface BiConverter<P1, P2, V> {

    /**
     * Converts the given pair of values into a result.
     *
     * @param p1 first input value
     * @param p2 second input value
     * @return the conversion result
     */
    V convert(P1 p1, P2 p2);
}
