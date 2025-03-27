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
package com.jd.live.agent.governance.subscription.config;

import com.jd.live.agent.core.parser.ConfigParser;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

import static com.jd.live.agent.governance.policy.service.ServiceName.getUniqueName;

@Getter
@Setter
public class ConfigName {

    private String namespace;

    private String name;

    private String profile;

    private String format = ConfigParser.PROPERTIES;

    private transient String fullName;

    public ConfigName() {
    }

    public ConfigName(String namespace, String name, String profile) {
        this(namespace, name, profile, getFormat(name));
    }

    public ConfigName(String namespace, String name, String profile, String format) {
        this.namespace = namespace;
        this.name = name;
        this.profile = profile;
        this.format = format;
    }

    public boolean validate() {
        return name != null && !name.isEmpty();
    }

    public String getFormat() {
        if (format == null) {
            format = getFormat(name);
        }
        return format;
    }

    public static String getFormat(String name) {
        int index = name == null ? -1 : name.lastIndexOf(".");
        if (index == -1) {
            return ConfigParser.PROPERTIES;
        } else {
            return name.substring(index + 1);
        }
    }

    @Override
    public String toString() {
        if (fullName == null) {
            fullName = getUniqueName(namespace, name, profile);
        }
        return fullName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConfigName)) return false;
        ConfigName that = (ConfigName) o;
        return Objects.equals(namespace, that.namespace) && Objects.equals(name, that.name) && Objects.equals(profile, that.profile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, name, profile);
    }
}

