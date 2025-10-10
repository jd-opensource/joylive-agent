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
package com.jd.live.agent.implement.parser.jackson;

import com.jd.live.agent.core.parser.json.CaseInsensitiveSetJsonConverter;
import com.jd.live.agent.core.parser.json.DeserializeConverter;

import java.util.Set;

public class Person {

    private String name;

    private int age;

    @DeserializeConverter(SexConverter.class)
    private Sex sex;

    @DeserializeConverter(CaseInsensitiveSetJsonConverter.class)
    private Set<String> aliases;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }

    public boolean containsAlias(String alias) {
        return alias != null && aliases != null && aliases.contains(alias);
    }
}
