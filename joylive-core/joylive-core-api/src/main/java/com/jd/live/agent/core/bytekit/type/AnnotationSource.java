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

import java.util.List;

/**
 * The {@code AnnotationSource} interface defines a contract for retrieving annotations
 * associated with an element of the Java program, such as a class, method, or field.
 * It allows for the inspection of annotations without the need for the element to be
 * instantiated or accessed via reflection.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface AnnotationSource {

    /**
     * Returns a list of annotations that are directly present on the annotated element.
     * This method ignores inherited annotations. The returned list does not include
     * annotations that are inherited from the class's superclass or from interfaces.
     *
     * @return a List of {@link AnnotationDesc} objects representing the annotations declared
     *         directly on the annotated element
     */
    List<AnnotationDesc> getDeclaredAnnotations();
}
