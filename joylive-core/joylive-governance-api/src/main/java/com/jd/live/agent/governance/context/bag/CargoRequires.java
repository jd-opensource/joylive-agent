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

import java.util.Arrays;
import java.util.HashSet;
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

    private final String[] names;

    private final Set<String> nameSet;

    private final String[] prefixes;

    public CargoRequires(List<CargoRequire> requires) {
        int size = requires == null ? 0 : requires.size();
        switch (size) {
            case 0:
                names = new String[0];
                nameSet = new HashSet<>();
                prefixes = new String[0];
                break;
            case 1:
                CargoRequire req = requires.get(0);
                names = req.getNames() == null ? new String[0] : req.getNames();
                nameSet = names == null ? new HashSet<>() : new HashSet<>(Arrays.asList(req.getNames()));
                prefixes = req.getPrefixes() == null ? new String[0] : req.getPrefixes();
                break;
            default:
                nameSet = new HashSet<>();
                Set<String> prefixNames = new HashSet<>();
                for (CargoRequire require : requires) {
                    if (require.getNames() != null) {
                        nameSet.addAll(Arrays.asList(require.getNames()));
                    }
                    if (require.getPrefixes() != null) {
                        prefixNames.addAll(Arrays.asList(require.getPrefixes()));
                    }
                }
                names = nameSet.toArray(new String[0]);
                prefixes = prefixNames.toArray(new String[0]);
        }
    }

    @Override
    public String[] getNames() {
        return names;
    }

    @Override
    public String[] getPrefixes() {
        return prefixes;
    }

    @Override
    public boolean match(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (nameSet.contains(name)) {
            return true;
        }
        for (String v : prefixes) {
            if (name.startsWith(v))
                return true;
        }
        return false;
    }
}
