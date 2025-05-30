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
package com.jd.live.agent.plugin.transmission.jdkhttp.request;

import com.jd.live.agent.governance.request.HeaderWriter.MultiValueMapWriter;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

public class BuilderWriter extends MultiValueMapWriter {

    public BuilderWriter(Map<String, List<String>> map) {
        super(map);
    }

    /**
     * Creates a new instance of {@link BuilderWriter} using the names and values from the given builder object.
     *
     * @param builder the builder object from which to extract the names and values
     * @return a new instance of BuilderParser initialized with the names and values
     */
    @SuppressWarnings("unchecked")
    public static BuilderWriter of(Object builder) {
        Object target = getQuietly(getQuietly(builder, "headersBuilder"), "headersMap");
        if (target instanceof Map) {
            return new BuilderWriter((Map<String, List<String>>) target);
        }
        return new BuilderWriter(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

}
