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
package com.jd.live.agent.governance.policy.live;

import com.jd.live.agent.core.parser.json.DeserializeConverter;
import com.jd.live.agent.core.parser.json.JsonAlias;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.core.util.trie.PathMatchType;
import com.jd.live.agent.core.util.trie.PathMatcherTrie;
import com.jd.live.agent.core.util.trie.PathTrie;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.live.converter.LiveTypeDeserializer;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

public class LiveDomain extends PolicyId {

    public static final URI LIVE_ROOT = URI.builder().scheme("live").build();

    @Getter
    @Setter
    private String host;

    @Getter
    @Setter
    private String[] protocols;

    @Getter
    @Setter
    @JsonAlias("unitFlag")
    @DeserializeConverter(LiveTypeDeserializer.class)
    private LiveType liveType;

    @Getter
    @Setter
    private CorrectionType correctionType;

    @Getter
    @Setter
    @JsonAlias("subDomainEnabled")
    private boolean unitDomainEnabled;

    @Getter
    @Setter
    @JsonAlias("subDomains")
    private List<UnitDomain> unitDomains;

    @Getter
    @Setter
    private List<LivePath> paths;

    @Getter
    @Setter
    private Set<String> resources;

    @Getter
    @Setter
    @JsonAlias("forwardAddress")
    private String backend;

    private final transient Cache<String, UnitDomain> unitDomainCache = new MapCache<>(new ListBuilder<>(() -> unitDomains, UnitDomain::getUnit));

    private final transient PathTrie<LivePath> pathTrie = new PathMatcherTrie<>(() -> paths);

    public UnitDomain getUnitDomain(String unit) {
        return unitDomainCache.get(unit);
    }

    public LivePath getPath(String path) {
        return pathTrie.match(path, PathMatchType.PREFIX);
    }

    protected void supplement() {
        supplement(() -> LIVE_ROOT.host(host));
        if (paths != null) {
            for (LivePath livePath : paths) {
                String path = livePath.getPath() == null || livePath.getPath().isEmpty() ? "/" : livePath.getPath();
                livePath.setPath(path);
                livePath.supplement(() -> uri.path(path));
                livePath.supplementVariable();
            }
        }
    }

    public void cache() {
        supplement();
        getUnitDomain("");
        getPath("");
        if (paths != null) {
            paths.forEach(LivePath::cache);
        }
    }
}
