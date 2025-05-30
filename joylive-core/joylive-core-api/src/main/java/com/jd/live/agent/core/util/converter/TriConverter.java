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
 * Converts three input values of types P1, P2, P3 to a result of type V.
 *
 * @param <P1> the type of first input parameter
 * @param <P2> the type of second input parameter
 * @param <P3> the type of third input parameter
 * @param <V>  the type of conversion result
 */
public interface TriConverter<P1, P2, P3, V> {

    /**
     * Transforms three input values into a converted result.
     *
     * @param p1 first input value
     * @param p2 second input value
     * @param p3 third input value
     * @return the conversion result
     */
    V convert(P1 p1, P2 p2, P3 p3);
}