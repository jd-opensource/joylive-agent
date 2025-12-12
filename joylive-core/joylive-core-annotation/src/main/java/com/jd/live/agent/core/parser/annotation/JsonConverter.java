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
package com.jd.live.agent.core.parser.annotation;

/**
 * Functional interface for converting one type of object to another.
 * This is used for JSON conversion, where a source object of type S is converted to a target object of type T.
 * It can be implemented to specify custom conversion logic when working with JSON data.
 *
 * @param <S> The source type from which to convert.
 * @param <T> The target type to which to convert.
 */
@FunctionalInterface
public interface JsonConverter<S, T> {

    /**
     * Converts an object of type S to an object of type T.
     * Implement this method to provide custom conversion logic.
     *
     * @param source The source object to convert.
     * @return The converted object of type T.
     */
    T convert(S source);
}

