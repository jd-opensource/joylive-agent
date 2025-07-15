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
package com.jd.live.agent.governance.context.bag;

import com.jd.live.agent.bootstrap.util.Inclusion;

import java.util.List;
import java.util.Set;

/**
 * Composite implementation of {@link CargoRequire} that aggregates multiple {@link CargoRequire} instances.
 */
public class CargoRequires implements CargoRequire {

    private final Inclusion inclusion;

    public CargoRequires(List<CargoRequire> requires) {
        inclusion = Inclusion.builder()
                .add(requires, (b, r) -> b.addNames(r.getNames()).addPrefixes(r.getPrefixes()))
                .build();
    }

    @Override
    public Set<String> getNames() {
        return inclusion.getNames();
    }

    @Override
    public Set<String> getPrefixes() {
        return inclusion.getPrefixes();
    }

    @Override
    public boolean test(String name) {
        return inclusion.test(name);
    }
}
