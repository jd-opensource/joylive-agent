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
package com.jd.live.agent.core.bootstrap;

import com.jd.live.agent.bootstrap.util.option.ValueSupplier;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.jd.live.agent.core.util.CollectionUtils.modifiedMap;

@Getter
public class AppEnv implements ValueSupplier {

    public static final String COMPONENT_APP_ENV = "AppEnv";

    private final Map<String, Object> envs;

    private final Map<String, String> remotes = new HashMap<>();

    @Setter
    private boolean environment;

    public AppEnv(Map<String, Object> envs) {
        this.envs = envs;
    }

    @Override
    public <T> T getObject(String key) {
        return key == null ? null : (T) envs.get(key);
    }

    public Object get(String key) {
        return key == null ? null : envs.get(key);
    }

    public String getString(String key) {
        return (String) get(key);
    }

    public void putIfAbsent(String key, Object value) {
        if (key != null) {
            envs.putIfAbsent(key, value);
        }
    }

    public void put(String key, Object value) {
        if (key != null) {
            envs.put(key, value);
        }
    }

    public void remove(String key) {
        if (key != null) {
            envs.remove(key);
        }
    }

    public void addRemotes(Map<String, String> env) {
        if (env != null) {
            remotes.putAll(env);
        }
    }

    public void addSystem() {
        if (environment && !remotes.isEmpty()) {
            try {
                Map<String, String> env = modifiedMap(System.getenv());
                remotes.forEach(env::putIfAbsent);
            } catch (Throwable e) {
            }
        }
    }

    public void ifPresentRemotes(Consumer<Map<String, String>> consumer) {
        if (consumer != null && !remotes.isEmpty()) {
            consumer.accept(remotes);
        }
    }

}
