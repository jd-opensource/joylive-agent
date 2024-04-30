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
package com.jd.live.agent.core.bytekit.type;

/**
 * The {@code AnnotationDesc} interface provides a description of a single annotation instance.
 * It allows for the inspection of the annotation type and potentially other details of the
 * annotation without the need to use reflection directly.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface AnnotationDesc {

    /**
     * Retrieves the type of the annotation that this descriptor represents.
     *
     * @return a {@link TypeDesc} object representing the annotation type
     */
    TypeDesc getAnnotationType();

}
