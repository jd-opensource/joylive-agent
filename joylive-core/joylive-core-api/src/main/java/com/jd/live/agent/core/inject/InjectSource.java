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
package com.jd.live.agent.core.inject;

import com.jd.live.agent.core.inject.InjectComponent.AbstractInjectComponent;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a source for injection, encapsulating the options and components involved in the injection process.
 * This class provides the functionality to manage and add components that can be injected.
 */
@Getter
public class InjectSource extends AbstractInjectComponent {

    /**
     * The options associated with this injection source. These options can define various
     * parameters or configurations used during the injection process.
     */
    private final Object option;

    /**
     * Constructs an {@code InjectSource} with the specified option and no initial components.
     *
     * @param option The option associated with this injection source.
     */
    public InjectSource(Object option) {
        this(option, null);
    }

    /**
     * Constructs an {@code InjectSource} with the specified option and initial components.
     *
     * @param option     The option associated with this injection source.
     * @param components The initial map of component names to their instances. It can be {@code null}.
     */
    public InjectSource(Object option, Map<String, Object> components) {
        super(components);
        this.option = option;
    }

    /**
     * Adds a component to this injection source. If the component name is valid and the component
     * is not {@code null}, it is added to the map of components. If the map does not exist, a new
     * one is created.
     *
     * @param name      The name of the component to add.
     * @param component The component instance to add.
     */
    public void add(String name, Object component) {
        if (name != null && !name.isEmpty() && component != null) {
            if (components == null) {
                components = new HashMap<>();
            }
            components.put(name, component);
        }
    }
}

