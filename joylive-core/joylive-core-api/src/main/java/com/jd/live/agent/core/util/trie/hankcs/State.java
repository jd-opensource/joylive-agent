/*
 * AhoCorasickDoubleArrayTrie Project
 *      https://github.com/hankcs/AhoCorasickDoubleArrayTrie
 *
 * Copyright 2008-2016 hankcs <me@hankcs.com>
 * You may modify and redistribute as long as this attribution remains.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jd.live.agent.core.util.trie.hankcs;

import java.util.*;

/**
 * Represents a state within a finite state machine, particularly used in pattern matching algorithms like Aho-Corasick.
 * Each state supports various functions:
 * <ul>
 *     <li>success: Successfully transition to another state.</li>
 *     <li>failure: If no direct transition exists for the given character, transition to a shallower node.</li>
 *     <li>emits: Indicates a successful match of a pattern string.</li>
 * </ul>
 * The root node behaves slightly differently in terms of failure; its failure function effectively moves to the next state based on the string path,
 * unlike other nodes that have a designated failure state.
 *
 * @author Robert Bor
 */
class State {

    /**
     * The depth of this state, equivalent to the length of the matched pattern string.
     */
    protected final int depth;

    /**
     * The failure function, pointing to the state to transition to if the current state doesn't match.
     */
    private State failure = null;

    /**
     * A set of pattern strings (identified by integers) that this state can emit, indicating a match.
     */
    private Set<Integer> emits = null;

    /**
     * The success table (goto function), dictating transitions based on the next character.
     */
    private final Map<Character, State> success = new TreeMap<>();

    /**
     * The index of this state in a double array data structure, if applicable.
     */
    private int index;

    /**
     * Constructs a root state with a depth of 0.
     */
    State() {
        this(0);
    }

    /**
     * Constructs a new state with the specified depth. The depth represents the length of the pattern string that has been matched up to this state.
     *
     * @param depth The depth of the state, which is also the number of characters matched so far in the pattern string.
     */
    State(int depth) {
        this.depth = depth;
    }

    /**
     * Retrieves the depth of the state. The depth indicates how many characters of the pattern string have been matched to reach this state.
     *
     * @return The depth of the state.
     */
    public int getDepth() {
        return this.depth;
    }

    /**
     * Adds a matched pattern to this state. The state corresponds to the pattern string that has been matched.
     * Patterns are identified by an integer (keyword), which can be an index or identifier for the pattern.
     *
     * @param keyword The identifier for the matched pattern string to be added to the current state.
     */
    public void addEmit(int keyword) {
        if (this.emits == null) {
            this.emits = new TreeSet<Integer>(Collections.reverseOrder());
        }
        this.emits.add(keyword);
    }

    /**
     * Retrieves the largest value identifier present in the state.
     *
     * @return The largest value id associated with this state.
     */
    public Integer getLargestValueId() {
        if (emits == null || emits.isEmpty()) return null;

        return emits.iterator().next();
    }

    /**
     * Adds a collection of matched pattern identifiers to this state.
     * Each pattern is represented by an integer, which may serve as an index or identifier.
     *
     * @param emits The collection of identifiers for the matched patterns to be added to this state.
     */
    public void addEmit(Collection<Integer> emits) {
        for (int emit : emits) {
            addEmit(emit);
        }
    }

    /**
     * Retrieves the collection of pattern strings represented by this state.
     * Each pattern string is associated with an integer identifier which is returned in the collection.
     *
     * @return A collection of integers representing the identifiers of the pattern strings.
     */
    public Collection<Integer> emit() {
        return this.emits == null ? Collections.<Integer>emptyList() : this.emits;
    }

    /**
     * Checks if this state is a terminal state.
     * A state is considered terminal if it has a depth greater than 0 and contains matched pattern identifiers.
     *
     * @return true if the state is terminal (acceptable), false otherwise.
     */
    public boolean isAcceptable() {
        return this.depth > 0 && this.emits != null;
    }

    /**
     * Retrieves the failure state of this node.
     *
     * @return The failure state of the current node.
     */
    public State failure() {
        return this.failure;
    }

    /**
     * Sets the failure state for this node.
     *
     * @param failState The failure state to be associated with this node.
     * @param fail      An array where the index of the current node's failure state will be recorded.
     */
    public void setFailure(State failState, int[] fail) {
        this.failure = failState;
        fail[index] = failState.index;
    }

    /**
     * Transitions to the next state based on the given character.
     *
     * @param character       The character to transition on.
     * @param ignoreRootState If true, the root state should be ignored, typically when the method is called by the root state itself; otherwise false.
     * @return The resulting state after the transition.
     */
    private State nextState(Character character, boolean ignoreRootState) {
        State nextState = this.success.get(character);
        if (!ignoreRootState && nextState == null && this.depth == 0) {
            nextState = this;
        }
        return nextState;
    }

    /**
     * Transitions to the next state based on the specified character. If the transition fails at the root node, it returns itself, ensuring that it never returns null.
     *
     * @param character The character used for the state transition.
     * @return The next state after the transition.
     */
    public State nextState(Character character) {
        return nextState(character, false);
    }

    /**
     * Transitions to the next state based on the specified character. If the transition fails at any node, it returns null.
     *
     * @param character The character used for the state transition.
     * @return The next state after the transition, or null if the transition fails.
     */
    public State nextStateIgnoreRootState(Character character) {
        return nextState(character, true);
    }

    public State addState(Character character) {
        State nextState = nextStateIgnoreRootState(character);
        if (nextState == null) {
            nextState = new State(this.depth + 1);
            this.success.put(character, nextState);
        }
        return nextState;
    }

    public Collection<State> getStates() {
        return this.success.values();
    }

    public Collection<Character> getTransitions() {
        return this.success.keySet();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("State{");
        sb.append("depth=").append(depth);
        sb.append(", ID=").append(index);
        sb.append(", emits=").append(emits);
        sb.append(", success=").append(success.keySet());
        sb.append(", failureID=").append(failure == null ? "-1" : failure.index);
        sb.append(", failure=").append(failure);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Retrieves the goto table.
     *
     * @return The state table mapping characters to their respective next states.
     */
    public Map<Character, State> getSuccess() {
        return success;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}