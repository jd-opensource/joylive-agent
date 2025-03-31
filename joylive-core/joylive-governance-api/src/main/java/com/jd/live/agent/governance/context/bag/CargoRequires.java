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
 * <p>
 * This class combines the requirements from a list of {@link CargoRequire} instances into a single set of names
 * and prefixes to match against. It is useful when multiple sets of criteria are needed to determine the cargos to be included.
 * </p>
 */
public class CargoRequires implements CargoRequire {

    private final Inclusion inclusion;

    public CargoRequires(List<CargoRequire> requires) {
        int size = requires == null ? 0 : requires.size();
        switch (size) {
            case 0:
                inclusion = new Inclusion();
                break;
            case 1:
                CargoRequire req = requires.get(0);
                inclusion = new Inclusion(req.getNames(), req.getPrefixes());
                break;
            default:
                inclusion = new Inclusion();
                for (CargoRequire require : requires) {
                    if (require.getNames() != null) {
                        inclusion.addNames(require.getNames());
                    }
                    if (require.getPrefixes() != null) {
                        inclusion.addPrefixes(require.getPrefixes());
                    }
                }
        }
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
