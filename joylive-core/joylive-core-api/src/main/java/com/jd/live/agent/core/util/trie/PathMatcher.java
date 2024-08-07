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

import java.util.HashMap;
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
        this.root = new TrieNode<>();
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
            if (part.isEmpty()) return true;
            String variableName = null;
            if (isVariable(part)) {
                variableName = part.substring(1, part.length() - 1);
                part = VARIABLE;
            } else if (part.equals("*")) {
                part = VARIABLE;
            }
            TrieNode<T> parent = current[0];
            int level = parent.level + 1;
            current[0] = parent.getOrCreate(part);
            current[0].variableName = variableName;
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
    @SuppressWarnings("unchecked")
    public MatchResult<T> match(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        final TrieNode<T>[] current = new TrieNode[]{root};
        final TrieNode<T>[] bestNode = new TrieNode[]{null};
        final Map<String, String>[] variables = new Map[]{null};
        final Map<String, String>[] bestVariables = new Map[]{null};

        int count = preprocessPath(path, part -> {
            int size = current[0].size();
            TrieNode<T> nextNode = size == 0 || (size == 1 && current[0].hasVariable) ? null : current[0].get(part);
            if (nextNode == null && current[0].hasVariable) {
                nextNode = current[0].get(VARIABLE);
                if (nextNode != null && nextNode.variableName != null) {
                    if (variables[0] == null) {
                        variables[0] = new HashMap<>();
                    }
                    variables[0].put(nextNode.variableName, part);
                }
            }

            if (nextNode != null) {
                current[0] = nextNode;
                if (nextNode.isEnd) {
                    bestNode[0] = nextNode;
                    if (variables[0] != null) {
                        if (bestVariables[0] == null) {
                            bestVariables[0] = new HashMap<>();
                        }
                        bestVariables[0].clear();
                        bestVariables[0].putAll(variables[0]);
                    }
                }
            } else {
                return false;
            }
            return true;
        });

        if (bestNode[0] == null) {
            // Special case: if there is no specific match, check if the root node has a value
            return !root.isEnd ? null : new MatchResult<>(
                    count == 0 ? PathMatchType.EQUAL : PathMatchType.PREFIX,
                    root.value, null);
        } else {
            return new MatchResult<>(
                    count == bestNode[0].level ? PathMatchType.EQUAL : PathMatchType.PREFIX,
                    bestNode[0].value, bestVariables[0]);
        }
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

        while ((end = path.indexOf(delimiter, start)) != -1) {
            if (start != end) {
                count++;
                String part = path.substring(start, end);
                if (!func.apply(part)) {
                    return count;
                }
            }
            start = end + 1;
        }

        if (start < path.length()) {
            count++;
            String part = path.substring(start);
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
    static class TrieNode<T> {
        Map<String, TrieNode<T>> children = new HashMap<>();
        int level;
        boolean hasVariable;
        boolean isEnd;
        String variableName;
        T value;

        TrieNode<T> getOrCreate(String child) {
            return children.computeIfAbsent(child, k -> new TrieNode<>());
        }

        TrieNode<T> get(String child) {
            return children.get(child);
        }

        int size() {
            return children.size();
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
    }

}
