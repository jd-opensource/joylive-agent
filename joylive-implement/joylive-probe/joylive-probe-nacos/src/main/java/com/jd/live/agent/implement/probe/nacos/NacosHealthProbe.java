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
package com.jd.live.agent.implement.probe.nacos;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.ObjectReader.StringReader;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.http.HttpResponse;
import com.jd.live.agent.core.util.http.HttpStatus;
import com.jd.live.agent.governance.probe.HealthProbe;

import java.io.IOException;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.CHAR_COMMA;
import static com.jd.live.agent.core.util.StringUtils.split;
import static com.jd.live.agent.core.util.http.HttpUtils.get;
import static com.jd.live.agent.governance.config.GovernanceConfig.CONFIG_PROBE_NACOS;

@Injectable
@Extension(HealthProbe.NACOS)
public class NacosHealthProbe implements HealthProbe {

    @Config(CONFIG_PROBE_NACOS)
    private NacosConfig config = new NacosConfig();

    private transient String lastPath;

    @Override
    public boolean test(String address) {
        List<URI> uris = toList(split(address, CHAR_COMMA), URI::parse);
        for (URI uri : uris) {
            String[] paths = getPaths();
            for (String path : paths) {
                try {
                    uri = URI.builder().scheme(uri.getScheme()).host(uri.getHost()).port(uri.getPort()).path(path).build();
                    HttpResponse<String> response = get(uri.toString(),
                            c -> {
                                c.setConnectTimeout(config.getConnectTimeout());
                                c.setReadTimeout(config.getReadTimeout());
                            }, new StringReader<>());
                    if (response.getStatus() == HttpStatus.OK) {
                        lastPath = path;
                        if (config.match(response.getData())) {
                            return true;
                        }
                    }
                } catch (IOException ignore) {
                }
            }

        }
        return false;
    }

    @Override
    public String type() {
        return HealthProbe.NACOS;
    }

    private String[] getPaths() {
        String[] paths = config.getPaths();
        if (lastPath != null) {
            for (int i = 0; i < paths.length; i++) {
                if (lastPath.equals(paths[i])) {
                    if (i > 0) {
                        paths = paths.clone();
                        paths[i] = paths[0];
                        paths[0] = lastPath;
                    }
                    break;
                }
            }
        }
        return paths;
    }
}
