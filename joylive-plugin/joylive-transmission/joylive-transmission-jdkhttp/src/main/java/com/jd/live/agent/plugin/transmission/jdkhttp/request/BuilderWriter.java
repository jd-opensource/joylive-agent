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

import com.jd.live.agent.core.util.type.FieldPath;
import com.jd.live.agent.governance.request.HeaderWriter.MultiValueMapWriter;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BuilderWriter extends MultiValueMapWriter {

    private static final FieldPath path = new FieldPath("jdk.internal.net.http.HttpRequestBuilderImpl", "headersBuilder.headersMap");

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
        return new BuilderWriter((Map<String, List<String>>) path.get(builder, TreeMap::new));
    }

}
