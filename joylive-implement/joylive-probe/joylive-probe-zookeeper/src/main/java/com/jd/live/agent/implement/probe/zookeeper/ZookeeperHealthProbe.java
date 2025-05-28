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
package com.jd.live.agent.implement.probe.zookeeper;

import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.SocketDetector;
import com.jd.live.agent.core.util.SocketDetector.SocketListener;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.probe.HealthProbe;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.split;
import static com.jd.live.agent.governance.config.GovernanceConfig.CONFIG_PROBE_ZOOKEEPER;
import static com.jd.live.agent.implement.probe.zookeeper.ZookeeperConfig.COMMAND_SRVR;

@Injectable
@Extension(HealthProbe.ZOOKEEPER)
public class ZookeeperHealthProbe implements HealthProbe, ExtensionInitializer {

    @Config(CONFIG_PROBE_ZOOKEEPER)
    private ZookeeperConfig config = new ZookeeperConfig();

    private SocketDetector detector;

    @Override
    public void initialize() {
        detector = new SocketDetector(config.getConnectTimeout(), 2181, new ZookeeperSocketListener(config));
    }

    @Override
    public boolean test(String address) {
        List<URI> uris = toList(split(address, ','), URI::parse);
        int threshold = uris.size() / 2 + 1;
        int win = 0;
        int fail = 0;
        for (URI uri : uris) {
            if (detector.test(uri.getHost(), uri.getPort())) {
                if (++win >= threshold) {
                    break;
                }
            } else if (++fail >= threshold) {
                break;
            }
        }
        return win >= threshold;
    }

    /**
     * ZooKeeper socket listener implementation.
     */
    private static class ZookeeperSocketListener implements SocketListener {

        private final ZookeeperConfig config;

        ZookeeperSocketListener(ZookeeperConfig config) {
            this.config = config;
        }

        @Override
        public void send(OutputStream out) throws IOException {
            String command = config.getCommand();
            command = command == null || command.isEmpty() ? COMMAND_SRVR : command;
            command = command.charAt(command.length() - 1) == '\n' ? command : command + "\n";
            out.write(command.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        @Override
        public boolean receive(InputStream in) throws IOException {
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
            }
            String response = config.getResponse();
            return response == null || response.isEmpty() || builder.toString().contains(response);
        }
    }
}
