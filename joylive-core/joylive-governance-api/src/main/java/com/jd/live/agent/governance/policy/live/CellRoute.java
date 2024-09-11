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

import com.jd.live.agent.core.parser.json.JsonAlias;
import com.jd.live.agent.governance.policy.AccessMode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Setter
@Getter
@ToString
public class CellRoute {

    public static final int PRIORITY_WHITELIST = 3;

    public static final int PRIORITY_PREFIX = 2;

    public static final int PRIORITY_LOCAL = 1;

    public static final int PRIORITY_NORMAL = 0;

    @Getter
    @Setter
    private String code;

    @JsonAlias("whitelist")
    private Set<String> allows;

    @JsonAlias("prefix")
    private Set<String> prefixes;

    @JsonAlias("ratio")
    private int weight;

    @Getter
    @Setter
    private AccessMode accessMode = AccessMode.READ_WRITE;

    private transient Cell cell;

    public boolean isAllow(String name) {
        return name != null && allows != null && !allows.isEmpty() && allows.contains(name);
    }

    public boolean isPrefix(String name) {
        if (name != null && !name.isEmpty() && prefixes != null && !prefixes.isEmpty()) {
            // TODO Use prefix trie
            for (String p : prefixes) {
                if (name.startsWith(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return weight <= 0 && (allows == null || allows.isEmpty()) && (prefixes == null || prefixes.isEmpty());
    }

    public boolean isLocal(Cell local) {
        return cell != null && cell == local;
    }

    public int getPriority(String variable, Cell cell) {
        if (isAllow(variable)) {
            return PRIORITY_WHITELIST;
        } else if (isPrefix(variable)) {
            return PRIORITY_PREFIX;
        } else if (isLocal(cell)) {
            return PRIORITY_LOCAL;
        } else {
            return PRIORITY_NORMAL;
        }
    }
}
