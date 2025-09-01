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
package com.jd.live.agent.core.inject.jbind.supplier;

import com.jd.live.agent.core.inject.InjectComponent.AbstractInjectComponent;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
public class JSource extends AbstractInjectComponent {

    private final Object current;
    private final Object parent;
    private final Object root;
    private final String path;
    @Setter
    private boolean updated;

    public JSource(Object current, Object parent, Object root, String path, Map<String, Object> components) {
        super(components);
        this.current = current;
        this.parent = parent;
        this.root = root;
        this.path = path;
    }

    public boolean cascade() {
        return parent != null && parent != root;
    }

    public JSource build(String key) {
        return new JSource(current, parent, root, path == null || path.isEmpty() ? key : path + "." + key, components);
    }
}
