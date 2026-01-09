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

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A class that provides functionality to add and match paths with variables and static segments.
 *
 * @param <T> The type of the value associated with each path.
 */
public class PathMatcher<T> {
    private static final String VARIABLE = ":";
    private static final String PATH_DELIMITER = "/";
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
        split(path, matcher -> {
            if (!matcher.isEmpty()) {
                Part part = parsePart(matcher.part);
                TrieNode<T> parent = current[0];
                TrieNode<T> child = parent.getOrCreateChild(part.value);
                child.addVariable(part);
                current[0] = child;
                parent.variableChild = parent.variableChild || part.variable;
            }
            matcher.success();
        });
        current[0].end = true;
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
        // /user/*/b/a match /user/order/b/a/c
        // /user/order/a match /user/order/a/b/c
        MatchState<T> state = new MatchState<>(root);
        int count = split(path, state::match);
        return state.getResult(count);
    }

    /**
     * Splits a path and processes segments via callback.
     *
     * <p>For "/" path: processes single empty segment.
     * For other paths: splits by delimiter and processes each segment sequentially.
     *
     * @param path input path to process ("/" gets special handling)
     * @param consumer callback that receives each segment in a Matcher
     * @return number of successfully processed segments
     */
    private int split(String path, Consumer<Matcher> consumer) {
        if (path.equals(PATH_DELIMITER)) {
            consumer.accept(new Matcher("", 0, Matched.SUCCESS, 0));
            return 0;
        }

        int count = 0;
        // ignore the first '/'
        int start = 0;
        int end;
        int max = path.length() - 1;

        Matcher matcher = new Matcher();
        while (true) {
            end = path.indexOf(delimiter, start);
            if (end == start) {
                start = end + 1;
                continue;
            }
            matcher.part = end == -1 ? path.substring(start) : path.substring(start, end);
            matcher.start = start;
            matcher.level = ++count;
            consumer.accept(matcher);
            switch (matcher.matched) {
                case SUCCESS:
                    if (end == -1 || end == max) {
                        return count;
                    }
                    start = end + 1;
                    break;
                case FAILED:
                    return count;
                case RESET:
                default:
                    start = matcher.start;
                    count = matcher.level;

            }
        }
    }

    /**
     * Parses string into a {@link Part}, detecting variable patterns:
     * - "*" → empty variable
     * - "{name}" → variable with content
     * - Other strings → literal part
     *
     * @param part String to parse
     * @return Part with variable flag set appropriately
     */
    private Part parsePart(String part) {
        int length = part.length();
        if (length == 1 && part.equals("*")) {
            return new Part(VARIABLE, true, null);
        } else if (length > 1 && part.charAt(0) == '{' && part.charAt(length - 1) == '}') {
            // Check if part starts with '{' and ends with '}'
            return new Part(VARIABLE, true, part.substring(1, length - 1));
        }
        return new Part(part, false, null);
    }

    /**
     * Tracks matching state for path segments during trie traversal.
     */
    private static class Matcher {

        private String part;

        private int start;

        private Matched matched;

        private int level;

        Matcher() {
        }

        Matcher(String part, int start, Matched matched, int level) {
            this.part = part;
            this.start = start;
            this.matched = matched;
            this.level = level;
        }

        /**
         * @return true if no segment remains to match
         */
        public boolean isEmpty() {
            return part == null || part.isEmpty();
        }

        /**
         * Marks current match as successful
         */
        public void success() {
            matched = Matched.SUCCESS;
        }

        /**
         * Marks current match as failed
         */
        public void fail() {
            matched = Matched.FAILED;
        }

        /**
         * Resets matcher for new matching attempt
         *
         * @param start new starting index
         * @param level new recursion depth
         */
        public void reset(int start, int level) {
            matched = Matched.RESET;
            this.start = start;
            this.level = level;
        }
    }

    /**
     * Represents a parsed variable with its name.
     */
    private static class Part {

        private final String value;

        private final boolean variable;

        private final String variableName;

        Part(String value, boolean variable, String variableName) {
            this.value = value;
            this.variable = variable;
            this.variableName = variableName;
        }
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
        // {name} or *
        private boolean variableChild;
        private boolean end;
        private String variable;
        private List<String> variables;
        private T value;

        TrieNode(String name) {
            this.name = name;
        }

        TrieNode(String name, int level) {
            this.name = name;
            this.level = level;
        }

        /**
         * Gets or creates a child node with the given name.
         * <p>
         * Efficiently handles three cases:
         * - No children exist (creates single child)
         * - Single child exists (converts to map if name differs)
         * - Multiple children exist (uses map lookup)
         *
         * @param name Name of the child node to get/create
         * @return Existing or newly created child node
         * @implNote Maintains size counter and optimizes storage
         */
        public TrieNode<T> getOrCreateChild(String name) {
            TrieNode<T> result;
            if (children == null && child == null) {
                child = new TrieNode<>(name, level + 1);
                size = 1;
                result = child;
            } else if (child != null) {
                if (child.name.equals(name)) {
                    // the same child, return directly
                    result = child;
                } else {
                    // multiple children, create a map to store them
                    children = new HashMap<>();
                    children.put(child.name, child);
                    child = null;
                    result = children.computeIfAbsent(name, n -> new TrieNode<>(n, level + 1));
                    size = children.size();
                }
            } else {
                result = children.computeIfAbsent(name, n -> new TrieNode<>(n, level + 1));
                size = children.size();
            }
            return result;
        }

        /**
         * Gets the child node with the specified name.
         *
         * @param name the name of the child node to retrieve
         * @return the matching child node, or {@code null} if no match is found
         */
        public TrieNode<T> getChild(String name) {
            if (children == null) {
                return child == null || !child.name.equals(name) ? null : child;
            }
            return children.get(name);
        }

        public int size() {
            return size;
        }

        /**
         * Adds a variable part to this node's variable collection.
         * <p>
         * Optimizes storage for common cases:
         * - First variable: stored directly
         * - Second variable: converts to list
         * - Subsequent variables: appends to list
         *
         * @param part The variable part to add (ignored if null or non-variable)
         * @implNote Deduplicates variables in list mode
         */
        public void addVariable(Part part) {
            if (part != null && part.variable && part.variableName != null) {
                // In most cases, there is only one variable
                if (variable == null && variables == null) {
                    variable = part.variableName;
                } else if (variable != null) {
                    if (!part.variableName.equals(variable)) {
                        variables = new ArrayList<>();
                        variables.add(variable);
                        variables.add(part.variableName);
                        variable = null;
                    }
                } else if (!variables.contains(part.variableName)) {
                    variables.add(part.variableName);
                }
            }
        }

        /**
         * Checks whether this node contains any variable(s).
         *
         * @return {@code true} if either a single variable or multiple variables exist,
         * {@code false} otherwise
         */
        public boolean hasVariable() {
            return variable != null || variables != null && !variables.isEmpty();
        }

        /**
         * Applies the given action to all variables in this node.
         *
         * @param consumer Action to execute for each variable name
         */
        public void variable(Consumer<String> consumer) {
            if (variable != null) {
                consumer.accept(variable);
            } else if (variables != null) {
                variables.forEach(consumer);
            }
        }
    }

    /**
     * Represents a path in a trie with optional variable bindings.
     * <p>
     * Tracks current node, path variables, and provides matching functionality.
     * Supports branching paths through {@code stack} parameter during matching.
     */
    private static class TreePath<T> {

        private TrieNode<T> node;

        private int start;

        private Map<String, String> variables;

        private TrieNode<T> candidate;

        private Map<String, String> candidateVariables;

        TreePath(TrieNode<T> node) {
            this.node = node;
        }

        TreePath(TrieNode<T> node, int start, Map<String, String> variables) {
            this.node = node;
            this.start = start;
            this.variables = variables;
        }

        /**
         * Attempts to match the next path segment against current trie node.
         *
         * @param matcher  contains current segment and matching state
         * @param supplier provides storage for alternative paths
         * @return true if segment matched (either literally or as variable)
         */
        public boolean match(Matcher matcher, Supplier<Deque<TreePath<T>>> supplier) {
            TrieNode<T> current = node;
            int size = current.size();
            if (size == 0) {
                return false;
            }
            TrieNode<T> child = size > 1 || !current.variableChild ? current.getChild(matcher.part) : null;
            if (child != null) {
                node = child;
                // add path to stack before candidate check
                addPath(current, matcher, supplier);
                candidate(child);
                return true;
            } else if (node.variableChild) {
                child = node.getChild(VARIABLE);
                if (child != null) {
                    child.variable(v -> {
                        if (variables == null) {
                            variables = new HashMap<>(4);
                        }
                        variables.put(v, matcher.part);
                    });
                    node = child;
                    candidate(child);
                    return true;
                }
            }
            return false;
        }

        /**
         * Sets current best match candidate if the node is a valid endpoint.
         *
         * @param child potential endpoint node to evaluate
         */
        private void candidate(TrieNode<T> child) {
            if (!child.end) {
                return;
            }
            candidate = child;
            if (variables != null) {
                if (candidateVariables == null) {
                    candidateVariables = variables;
                } else {
                    candidateVariables.putAll(variables);
                }
                variables = null;
            }
        }

        /**
         * Registers alternative matching paths for variable path segments.
         *
         * @param node     current trie node being evaluated
         * @param matcher  contains current matching state and segment
         * @param supplier provides stack for storing alternative paths
         */
        private void addPath(TrieNode<T> node, Matcher matcher, Supplier<Deque<TreePath<T>>> supplier) {
            TrieNode<T> child;
            if (node.variableChild) {
                // another candidate path
                child = node.getChild(VARIABLE);
                if (child != null) {
                    Map<String, String> newVariables = variables != null ? new HashMap<>(variables) : (child.hasVariable() ? new HashMap<>(4) : null);
                    child.variable(v -> newVariables.put(v, matcher.part));
                    supplier.get().add(new TreePath<>(child, matcher.start + matcher.part.length() + 1, newVariables));
                }
            }
        }

    }

    /**
     * Internal state tracker for trie path matching, handling:
     * - Node traversal
     * - Variable capture
     * - Best-match tracking
     */
    private static class MatchState<T> {
        private final TrieNode<T> root;
        private TreePath<T> current;
        private Deque<TreePath<T>> stack;

        MatchState(TrieNode<T> root) {
            this.root = root;
            this.current = new TreePath<>(root);
        }

        /**
         * Attempts to match the current path segment using trie navigation.
         *
         * @param matcher tracks matching state and segment position
         */
        public void match(Matcher matcher) {
            if (current.match(matcher, this::getStack)) {
                matcher.success();
            } else if (current.candidate == null && stack != null && !stack.isEmpty()) {
                current = stack.pop();
                matcher.reset(current.start, current.node.level);
            } else {
                matcher.fail();
            }
        }

        /**
         * Generates the final match result after traversal.
         * <p>
         * Returns:
         * - {@code null} if no match found
         * - Result with {@code EQUAL} (full path match) or {@code PREFIX} (partial match)
         * - Captured variables (if any)
         *
         * @param level Depth of the current path (0 = root)
         * @return Match result, or null if unmatched
         */
        public MatchResult<T> getResult(int level) {
            TrieNode<T> candidate = current.candidate;
            if (candidate == null) {
                // Special case: if there is no specific match, check if the root node has a value
                return !root.end ? null : new MatchResult<>(level == 0 ? PathMatchType.EQUAL : PathMatchType.PREFIX, root.value, null);
            } else {
                return new MatchResult<>(level == candidate.level ? PathMatchType.EQUAL : PathMatchType.PREFIX, candidate.value, current.candidateVariables);
            }
        }

        private Deque<TreePath<T>> getStack() {
            if (stack == null) {
                stack = new LinkedList<>();
            }
            return stack;
        }
    }

    /**
     * Represents the possible matching states during path resolution.
     */
    private enum Matched {
        /**
         * Path segment was successfully matched
         */
        SUCCESS,

        /**
         * No matching path segment found
         */
        FAILED,

        /**
         * Matching state was reset for new attempt
         */
        RESET
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
