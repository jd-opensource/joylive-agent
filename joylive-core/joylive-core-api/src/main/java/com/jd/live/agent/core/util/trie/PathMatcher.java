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
package com.jd.live.agent.core.util.trie;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A class that provides functionality to add and match paths with variables and static segments.
 *
 * @param <T> The type of the value associated with each path.
 */
public class PathMatcher<T> {
    private static final String VARIABLE = ":";
    private final TrieNode<T> root;
    private final char delimiter;

    public PathMatcher() {
        this(PathType.URL.getDelimiter());
    }

    public PathMatcher(char delimiter) {
        this.delimiter = delimiter;
        this.root = new TrieNode<>("");
    }

    /**
     * Adds a path with an associated value to the matcher.
     *
     * @param path  The path to add. Can contain variables in the form of {variableName}.
     * @param value The value associated with the path.
     */
    @SuppressWarnings("unchecked")
    public void addPath(String path, T value) {
        if (path == null || path.isEmpty()) {
            return;
        }
        final TrieNode<T>[] current = new TrieNode[]{root};
        preprocessPath(path, part -> {
            if (part.isEmpty()) {
                return true;
            }
            String variableName = null;
            if (isVariable(part)) {
                variableName = part.substring(1, part.length() - 1);
                part = VARIABLE;
            } else if (part.equals("*")) {
                part = VARIABLE;
            }
            TrieNode<T> parent = current[0];
            int level = parent.level + 1;
            current[0] = parent.getOrCreateChild(part);
            current[0].addVariable(variableName);
            current[0].level = level;
            parent.hasVariable = parent.hasVariable || VARIABLE.equals(part);
            return true;
        });
        current[0].isEnd = true;
        current[0].value = value;
    }

    /**
     * Matches a given path against the added paths and returns the best match result.
     *
     * @param path The path to match.
     * @return A MatchResult containing the matched value and variables, or null if no match is found.
     */
    public MatchResult<T> match(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        // TODO match the max length?
        // /user/*/a/b
        // /user/order/a
        // /user/order/a/b/c now match /user/order/a, maybe the best result is /user/*/a/b.
        MatchState<T> state = new MatchState<>(root);
        int count = preprocessPath(path, state::next);
        return state.getResult(count);
    }

    /**
     * Processes a given path by splitting it into parts based on a specified delimiter and applying a function to each part.
     *
     * @param path The path to be processed. If the path is exactly "/", a single empty string will be passed to the function.
     * @param func The function to be applied to each part of the path. The function should return {@code true} to continue processing,
     *             or {@code false} to terminate processing early.
     * @return The number of parts processed. If the function terminates early, the count will reflect the number of parts processed up to that point.
     */
    private int preprocessPath(String path, Function<String, Boolean> func) {
        if (path.equals("/")) {
            func.apply("");
            return 0;
        }

        int count = 0;
        int start = 0;
        int end;
        String part;

        while ((end = path.indexOf(delimiter, start)) != -1) {
            if (start != end) {
                count++;
                part = path.substring(start, end);
                if (!func.apply(part)) {
                    return count;
                }
            }
            start = end + 1;
        }

        if (start < path.length()) {
            count++;
            part = path.substring(start);
            func.apply(part);
        }
        return count;
    }

    /**
     * Checks if a path segment is a variable.
     *
     * @param part The path segment to check.
     * @return True if the segment is a variable, false otherwise.
     */
    private boolean isVariable(String part) {
        // Check if part starts with '{' and ends with '}'
        return part.length() > 1 && part.charAt(0) == '{' && part.charAt(part.length() - 1) == '}';
    }

    /**
     * A class that provides functionality to match paths with variables and static segments.
     *
     * @param <T> The type of the value associated with each path.
     */
    private static class TrieNode<T> {
        private final String name;
        private TrieNode<T> child;
        private Map<String, TrieNode<T>> children;
        private int size;
        private int level;
        private boolean hasVariable;
        private boolean isEnd;
        private List<String> variables;
        private T value;

        TrieNode(String name) {
            this.name = name;
        }

        public TrieNode<T> getOrCreateChild(String name) {
            TrieNode<T> result;
            if (children == null && child == null) {
                child = new TrieNode<>(name);
                size = 1;
                result = child;
            } else if (child != null) {
                children = new HashMap<>();
                children.put(child.name, child);
                child = null;
                result = children.computeIfAbsent(name, TrieNode::new);
                size = children.size();
            } else {
                result = children.computeIfAbsent(name, TrieNode::new);
                size = children.size();
            }
            return result;
        }

        public TrieNode<T> getChild(String name) {
            if (children == null) {
                return child == null || !child.name.equals(name) ? null : child;
            }
            return children.get(name);
        }

        public int size() {
            return size;
        }

        public void addVariable(String variable) {
            if (variable != null) {
                if (variables == null) {
                    variables = new ArrayList<>(1);
                }
                if (!variables.contains(variable)) {
                    variables.add(variable);
                }
            }
        }
    }

    private static class MatchState<T> {
        private final TrieNode<T> root;
        private TrieNode<T> current;
        private TrieNode<T> bestNode;
        private Map<String, String> variables;
        private Map<String, String> bestVariables;

        MatchState(TrieNode<T> root) {
            this.root = root;
            this.current = root;
        }

        public boolean next(String part) {
            int size = current.size();
            TrieNode<T> nextNode = size == 0 || (size == 1 && current.hasVariable) ? null : current.getChild(part);
            if (nextNode == null && current.hasVariable) {
                nextNode = current.getChild(VARIABLE);
                if (nextNode != null && nextNode.variables != null) {
                    if (variables == null) {
                        variables = new HashMap<>(1);
                    }
                    nextNode.variables.forEach(v -> variables.put(v, part));
                }
            }
            if (nextNode != null) {
                current = nextNode;
                if (nextNode.isEnd) {
                    bestNode = nextNode;
                    if (variables != null) {
                        if (bestVariables == null) {
                            bestVariables = new HashMap<>(variables);
                        } else {
                            bestVariables.clear();
                            bestVariables.putAll(variables);
                        }
                    }
                }
            }

            return nextNode != null;
        }

        public MatchResult<T> getResult(int level) {
            if (bestNode == null) {
                // Special case: if there is no specific match, check if the root node has a value
                return !root.isEnd ? null : new MatchResult<>(level == 0 ? PathMatchType.EQUAL : PathMatchType.PREFIX, root.value, null);
            } else {
                return new MatchResult<>(level == bestNode.level ? PathMatchType.EQUAL : PathMatchType.PREFIX, bestNode.value, bestVariables);
            }
        }
    }

    /**
     * A class that represents the result of a path match.
     *
     * @param <T> The type of the value associated with the matched path.
     */
    @Getter
    @AllArgsConstructor
    @ToString
    public static class MatchResult<T> {
        private final PathMatchType type;
        private final T value;
        private final Map<String, String> variables;

        public String getVariable(String name) {
            return name == null || variables == null ? null : variables.get(name);
        }
    }

}
