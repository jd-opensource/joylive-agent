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
package com.jd.live.agent.core.bootstrap.env.node;

import com.jd.live.agent.core.bootstrap.EnvSupplier;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Injectable
@Extension("NodeEnvSupplier")
public class NodeEnvSupplier implements EnvSupplier {

    @Override
    public void process(Map<String, Object> env) {
        String nodeName = (String) env.get("NODE_NAME");
        String nodeZones = (String) env.get("NODE_ZONES");
        if (nodeName == null || nodeZones == null || nodeName.isEmpty() || nodeZones.isEmpty()) {
            return;
        }
        Map<String, List<String>> zoneToNodesMap = Arrays.stream(nodeZones.split(";"))
                .map(nz -> nz.split(":", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> parts[0],
                        parts -> Arrays.asList(parts[1].split(","))
                ));
        List<String> nodeCells = zoneToNodesMap.get(nodeName);
        if (nodeCells != null && !nodeCells.isEmpty()) {
            env.put("NODE_CELL", nodeCells.get(0));
        }
    }
}
