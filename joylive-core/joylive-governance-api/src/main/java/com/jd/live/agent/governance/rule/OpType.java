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
package com.jd.live.agent.governance.rule;

import com.jd.live.agent.core.parser.json.JsonAlias;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * The OpType enum represents different types of operations that can be performed
 * for condition matching within a conditional interface such as {@link Conditional}.
 * Each operation type has a corresponding code, a description, and a method to determine
 * if a given argument matches the criteria defined by a list of values.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Getter
public enum OpType {

    /**
     * Represents an equality operation.
     */
    @JsonAlias("eq")
    EQUAL("eq", "equal") {
        @Override
        public boolean match(List<String> sources, List<String> targets) {
            int srcSize = sources == null ? 0 : sources.size();
            int targetSize = targets == null ? 0 : targets.size();
            if (srcSize == 0 || srcSize != targetSize) {
                return false;
            } else if (srcSize == 1) {
                // improve performance
                return sources.get(0).equals(targets.get(0));
            }
            for (String target : targets) {
                if (!sources.contains(target)) {
                    return false;
                }
            }
            return true;
        }
    },

    /**
     * Represents a not-equal operation.
     */
    @JsonAlias("ne")
    NOT_EQUAL("ne", "not equal") {
        @Override
        public boolean match(List<String> sources, List<String> targets) {
            int srcSize = sources == null ? 0 : sources.size();
            int targetSize = targets == null ? 0 : targets.size();
            if (srcSize != targetSize) {
                return true;
            } else if (srcSize == 0) {
                return false;
            } else if (srcSize == 1) {
                // improve performance
                return !sources.get(0).equals(targets.get(0));
            }
            for (String target : targets) {
                if (!sources.contains(target)) {
                    return true;
                }
            }
            return false;
        }
    },

    /**
     * Represents an operation to check if a value is not in a list.
     */
    @JsonAlias("nin")
    NOT_IN("nin", "not in") {
        @Override
        public boolean match(List<String> sources, List<String> targets) {
            int srcSize = sources == null ? 0 : sources.size();
            int targetSize = targets == null ? 0 : targets.size();
            if (srcSize == 0 || targetSize == 0) {
                return true;
            } else if (srcSize == 1 && targetSize == 1) {
                // improve performance
                return !sources.get(0).equals(targets.get(0));
            }
            for (String source : sources) {
                if (targets.contains(source)) {
                    return false;
                }
            }
            return true;
        }
    },

    /**
     * Represents an operation to check if a value is in a list.
     */
    @JsonAlias("in")
    IN("in", "in") {
        @Override
        public boolean match(List<String> sources, List<String> targets) {
            int srcSize = sources == null ? 0 : sources.size();
            int targetSize = targets == null ? 0 : targets.size();
            if (srcSize == 0 || targetSize == 0) {
                return false;
            } else if (srcSize == 1 && targetSize == 1) {
                // improve performance
                return sources.get(0).equals(targets.get(0));
            }
            for (String source : sources) {
                if (targets.contains(source)) {
                    return true;
                }
            }
            return false;
        }
    },

    /**
     * Represents a regular expression match operation.
     */
    @JsonAlias("regular")
    REGULAR("regular", "regular") {
        @Override
        public boolean match(List<String> sources, List<String> targets) {
            int srcSize = sources == null ? 0 : sources.size();
            int targetSize = targets == null ? 0 : targets.size();
            if (srcSize == 0 || targetSize == 0) {
                return false;
            } else if (srcSize == 1 && targetSize == 1) {
                // improve performance
                return PATTERNS.computeIfAbsent(sources.get(0), Pattern::compile).matcher(targets.get(0)).matches();
            }
            // match any source
            for (String target : targets) {
                if (!match(sources, target)) {
                    return false;
                }
            }
            return true;
        }

        private boolean match(List<String> sources, String target) {
            // match any source
            for (String source : sources) {
                if (PATTERNS.computeIfAbsent(source, Pattern::compile).matcher(target).matches()) {
                    return true;
                }
            }
            return false;
        }
    },

    /**
     * Represents a prefix match operation.
     */
    @JsonAlias("prefix")
    PREFIX("prefix", "prefix") {
        @Override
        public boolean match(List<String> sources, List<String> targets) {
            int srcSize = sources == null ? 0 : sources.size();
            int targetSize = targets == null ? 0 : targets.size();
            if (srcSize == 0 || targetSize == 0) {
                return false;
            } else if (srcSize == 1 && targetSize == 1) {
                // improve performance
                return targets.get(0).startsWith(sources.get(0));
            }
            // prefix with any source
            for (String target : targets) {
                if (!match(sources, target)) {
                    return false;
                }
            }
            return true;
        }

        private boolean match(List<String> sources, String target) {
            // prefix with any source
            for (String source : sources) {
                if (target.startsWith(source)) {
                    return true;
                }
            }
            return false;
        }
    };

    private static final Map<String, Pattern> PATTERNS = new ConcurrentHashMap<>();

    /**
     * The code associated with this operation type.
     */
    private final String code;

    /**
     * The description of this operation type.
     */
    private final String description;

    /**
     * Constructs an OpType with the specified code and description.
     *
     * @param code        the code for the operation type
     * @param description the human-readable description of the operation type
     */
    OpType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Determines if the specified argument matches the criteria defined by the list of values
     * for this operation type.
     *
     * @param values the list of values to match against
     * @param args   the list of arguments to test for a match
     * @return true if the argument matches the criteria, false otherwise
     */
    public boolean match(List<String> values, List<String> args) {
        return false;
    }

}

