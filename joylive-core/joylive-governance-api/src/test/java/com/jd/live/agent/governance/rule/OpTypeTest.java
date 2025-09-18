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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link OpType} enum.
 * Tests all operation types including edge cases and various input combinations.
 *
 * @author Test Author
 * @since 1.0.0
 */
public class OpTypeTest {

    // Test data for different scenarios
    private static final List<String> EMPTY_LIST = Collections.emptyList();
    private static final List<String> NULL_LIST = null;
    private static final List<String> SINGLE_ITEM = Collections.singletonList("test");
    private static final List<String> MULTIPLE_ITEMS = Arrays.asList("test1", "test2", "test3");

    // EQUAL operation tests
    @Test
    @DisplayName("EQUAL operation - empty lists test")
    public void testEqualWithEmptyLists() {
        assertFalse(OpType.EQUAL.match(EMPTY_LIST, EMPTY_LIST));
        assertFalse(OpType.EQUAL.match(NULL_LIST, NULL_LIST));
        assertFalse(OpType.EQUAL.match(EMPTY_LIST, SINGLE_ITEM));
        assertFalse(OpType.EQUAL.match(SINGLE_ITEM, EMPTY_LIST));
        assertFalse(OpType.EQUAL.match(NULL_LIST, SINGLE_ITEM));
        assertFalse(OpType.EQUAL.match(SINGLE_ITEM, NULL_LIST));
    }

    @Test
    @DisplayName("EQUAL operation - single element test")
    public void testEqualWithSingleElement() {
        List<String> same = Collections.singletonList("test");
        List<String> different = Collections.singletonList("other");

        assertTrue(OpType.EQUAL.match(same, same));
        assertTrue(OpType.EQUAL.match(SINGLE_ITEM, same));
        assertFalse(OpType.EQUAL.match(SINGLE_ITEM, different));
    }

    @Test
    @DisplayName("EQUAL operation - multiple elements test")
    public void testEqualWithMultipleElements() {
        List<String> same = Arrays.asList("test1", "test2", "test3");
        List<String> sameReordered = Arrays.asList("test2", "test1", "test3");
        List<String> different = Arrays.asList("test1", "test2", "other");
        List<String> differentSize = Arrays.asList("test1", "test2");

        assertTrue(OpType.EQUAL.match(MULTIPLE_ITEMS, same));
        assertTrue(OpType.EQUAL.match(MULTIPLE_ITEMS, sameReordered));
        assertFalse(OpType.EQUAL.match(MULTIPLE_ITEMS, different));
        assertFalse(OpType.EQUAL.match(MULTIPLE_ITEMS, differentSize));
    }

    // NOT_EQUAL operation tests
    @Test
    @DisplayName("NOT_EQUAL operation - empty lists test")
    public void testNotEqualWithEmptyLists() {
        assertFalse(OpType.NOT_EQUAL.match(EMPTY_LIST, EMPTY_LIST));
        assertFalse(OpType.NOT_EQUAL.match(NULL_LIST, NULL_LIST));
        assertTrue(OpType.NOT_EQUAL.match(EMPTY_LIST, SINGLE_ITEM));
        assertTrue(OpType.NOT_EQUAL.match(SINGLE_ITEM, EMPTY_LIST));
        assertTrue(OpType.NOT_EQUAL.match(NULL_LIST, SINGLE_ITEM));
        assertTrue(OpType.NOT_EQUAL.match(SINGLE_ITEM, NULL_LIST));
    }

    @Test
    @DisplayName("NOT_EQUAL operation - single element test")
    public void testNotEqualWithSingleElement() {
        List<String> same = Collections.singletonList("test");
        List<String> different = Collections.singletonList("other");

        assertFalse(OpType.NOT_EQUAL.match(same, same));
        assertFalse(OpType.NOT_EQUAL.match(SINGLE_ITEM, same));
        assertTrue(OpType.NOT_EQUAL.match(SINGLE_ITEM, different));
    }

    @Test
    @DisplayName("NOT_EQUAL operation - multiple elements test")
    public void testNotEqualWithMultipleElements() {
        List<String> same = Arrays.asList("test1", "test2", "test3");
        List<String> sameReordered = Arrays.asList("test2", "test1", "test3");
        List<String> different = Arrays.asList("test1", "test2", "other");
        List<String> differentSize = Arrays.asList("test1", "test2");

        assertFalse(OpType.NOT_EQUAL.match(MULTIPLE_ITEMS, same));
        assertFalse(OpType.NOT_EQUAL.match(MULTIPLE_ITEMS, sameReordered));
        assertTrue(OpType.NOT_EQUAL.match(MULTIPLE_ITEMS, different));
        assertTrue(OpType.NOT_EQUAL.match(MULTIPLE_ITEMS, differentSize));
    }

    // IN operation tests
    @Test
    @DisplayName("IN operation - empty lists test")
    public void testInWithEmptyLists() {
        assertFalse(OpType.IN.match(EMPTY_LIST, EMPTY_LIST));
        assertFalse(OpType.IN.match(NULL_LIST, NULL_LIST));
        assertFalse(OpType.IN.match(EMPTY_LIST, SINGLE_ITEM));
        assertFalse(OpType.IN.match(SINGLE_ITEM, EMPTY_LIST));
        assertFalse(OpType.IN.match(NULL_LIST, SINGLE_ITEM));
        assertFalse(OpType.IN.match(SINGLE_ITEM, NULL_LIST));
    }

    @Test
    @DisplayName("IN operation - single element test")
    public void testInWithSingleElement() {
        List<String> same = Collections.singletonList("test");
        List<String> different = Collections.singletonList("other");

        assertTrue(OpType.IN.match(same, same));
        assertTrue(OpType.IN.match(SINGLE_ITEM, same));
        assertFalse(OpType.IN.match(SINGLE_ITEM, different));
    }

    @Test
    @DisplayName("IN operation - multiple elements test")
    public void testInWithMultipleElements() {
        List<String> sources = Arrays.asList("test1", "test2");
        List<String> targetsContaining = Arrays.asList("test1", "other1", "other2");
        List<String> targetsNotContaining = Arrays.asList("other1", "other2", "other3");
        List<String> targetsPartialContaining = Arrays.asList("test1", "test3", "other");

        assertTrue(OpType.IN.match(sources, targetsContaining));
        assertFalse(OpType.IN.match(sources, targetsNotContaining));
        assertTrue(OpType.IN.match(sources, targetsPartialContaining));
    }

    // NOT_IN operation tests
    @Test
    @DisplayName("NOT_IN operation - empty lists test")
    public void testNotInWithEmptyLists() {
        assertTrue(OpType.NOT_IN.match(EMPTY_LIST, EMPTY_LIST));
        assertTrue(OpType.NOT_IN.match(NULL_LIST, NULL_LIST));
        assertTrue(OpType.NOT_IN.match(EMPTY_LIST, SINGLE_ITEM));
        assertTrue(OpType.NOT_IN.match(SINGLE_ITEM, EMPTY_LIST));
        assertTrue(OpType.NOT_IN.match(NULL_LIST, SINGLE_ITEM));
        assertTrue(OpType.NOT_IN.match(SINGLE_ITEM, NULL_LIST));
    }

    @Test
    @DisplayName("NOT_IN operation - single element test")
    public void testNotInWithSingleElement() {
        List<String> same = Collections.singletonList("test");
        List<String> different = Collections.singletonList("other");

        assertFalse(OpType.NOT_IN.match(same, same));
        assertFalse(OpType.NOT_IN.match(SINGLE_ITEM, same));
        assertTrue(OpType.NOT_IN.match(SINGLE_ITEM, different));
    }

    @Test
    @DisplayName("NOT_IN operation - multiple elements test")
    public void testNotInWithMultipleElements() {
        List<String> sources = Arrays.asList("test1", "test2");
        List<String> targetsContaining = Arrays.asList("test1", "other1", "other2");
        List<String> targetsNotContaining = Arrays.asList("other1", "other2", "other3");
        List<String> targetsPartialContaining = Arrays.asList("test1", "test3", "other");

        assertFalse(OpType.NOT_IN.match(sources, targetsContaining));
        assertTrue(OpType.NOT_IN.match(sources, targetsNotContaining));
        assertFalse(OpType.NOT_IN.match(sources, targetsPartialContaining));
    }

    // REGULAR operation tests
    @Test
    @DisplayName("REGULAR operation - empty lists test")
    public void testRegularWithEmptyLists() {
        assertFalse(OpType.REGULAR.match(EMPTY_LIST, EMPTY_LIST));
        assertFalse(OpType.REGULAR.match(NULL_LIST, NULL_LIST));
        assertFalse(OpType.REGULAR.match(EMPTY_LIST, SINGLE_ITEM));
        assertFalse(OpType.REGULAR.match(SINGLE_ITEM, EMPTY_LIST));
        assertFalse(OpType.REGULAR.match(NULL_LIST, SINGLE_ITEM));
        assertFalse(OpType.REGULAR.match(SINGLE_ITEM, NULL_LIST));
    }

    @Test
    @DisplayName("REGULAR operation - single element test")
    public void testRegularWithSingleElement() {
        List<String> pattern = Collections.singletonList("test.*");
        List<String> matchingTarget = Collections.singletonList("test123");
        List<String> nonMatchingTarget = Collections.singletonList("other123");

        assertTrue(OpType.REGULAR.match(pattern, matchingTarget));
        assertFalse(OpType.REGULAR.match(pattern, nonMatchingTarget));
    }

    @Test
    @DisplayName("REGULAR operation - multiple elements test")
    public void testRegularWithMultipleElements() {
        List<String> patterns = Arrays.asList("test.*", "\\d+");
        List<String> allMatching = Arrays.asList("test123", "456");
        List<String> partialMatching = Arrays.asList("test123", "abc");
        List<String> noneMatching = Arrays.asList("abc", "def");

        assertTrue(OpType.REGULAR.match(patterns, allMatching));
        assertFalse(OpType.REGULAR.match(patterns, partialMatching));
        assertFalse(OpType.REGULAR.match(patterns, noneMatching));
    }

    @Test
    @DisplayName("REGULAR operation - complex regex patterns test")
    public void testRegularWithComplexPatterns() {
        List<String> emailPattern = Collections.singletonList("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        List<String> validEmail = Collections.singletonList("test@example.com");
        List<String> invalidEmail = Collections.singletonList("invalid-email");

        assertTrue(OpType.REGULAR.match(emailPattern, validEmail));
        assertFalse(OpType.REGULAR.match(emailPattern, invalidEmail));
    }

    // PREFIX operation tests
    @Test
    @DisplayName("PREFIX operation - empty lists test")
    public void testPrefixWithEmptyLists() {
        assertFalse(OpType.PREFIX.match(EMPTY_LIST, EMPTY_LIST));
        assertFalse(OpType.PREFIX.match(NULL_LIST, NULL_LIST));
        assertFalse(OpType.PREFIX.match(EMPTY_LIST, SINGLE_ITEM));
        assertFalse(OpType.PREFIX.match(SINGLE_ITEM, EMPTY_LIST));
        assertFalse(OpType.PREFIX.match(NULL_LIST, SINGLE_ITEM));
        assertFalse(OpType.PREFIX.match(SINGLE_ITEM, NULL_LIST));
    }

    @Test
    @DisplayName("PREFIX operation - single element test")
    public void testPrefixWithSingleElement() {
        List<String> prefix = Collections.singletonList("test");
        List<String> matchingTarget = Collections.singletonList("test123");
        List<String> exactMatch = Collections.singletonList("test");
        List<String> nonMatchingTarget = Collections.singletonList("other123");

        assertTrue(OpType.PREFIX.match(prefix, matchingTarget));
        assertTrue(OpType.PREFIX.match(prefix, exactMatch));
        assertFalse(OpType.PREFIX.match(prefix, nonMatchingTarget));
    }

    @Test
    @DisplayName("PREFIX operation - multiple elements test")
    public void testPrefixWithMultipleElements() {
        List<String> prefixes = Arrays.asList("test", "demo");
        List<String> allMatching = Arrays.asList("test123", "demo456");
        List<String> partialMatching = Arrays.asList("test123", "other");
        List<String> noneMatching = Arrays.asList("other1", "other2");

        assertTrue(OpType.PREFIX.match(prefixes, allMatching));
        assertFalse(OpType.PREFIX.match(prefixes, partialMatching));
        assertFalse(OpType.PREFIX.match(prefixes, noneMatching));
    }

    @Test
    @DisplayName("PREFIX operation - edge cases test")
    public void testPrefixEdgeCases() {
        List<String> emptyPrefix = Collections.singletonList("");
        List<String> anyTarget = Collections.singletonList("anything");

        assertTrue(OpType.PREFIX.match(emptyPrefix, anyTarget));

        List<String> longPrefix = Collections.singletonList("verylongprefix");
        List<String> shortTarget = Collections.singletonList("short");

        assertFalse(OpType.PREFIX.match(longPrefix, shortTarget));
    }

    // Parameterized tests for comprehensive coverage
    @ParameterizedTest
    @MethodSource("provideEqualTestCases")
    @DisplayName("EQUAL operation parameterized test")
    public void testEqualParameterized(List<String> sources, List<String> targets, boolean expected) {
        assertEquals(expected, OpType.EQUAL.match(sources, targets));
    }

    @ParameterizedTest
    @MethodSource("provideNotEqualTestCases")
    @DisplayName("NOT_EQUAL operation parameterized test")
    public void testNotEqualParameterized(List<String> sources, List<String> targets, boolean expected) {
        assertEquals(expected, OpType.NOT_EQUAL.match(sources, targets));
    }

    @ParameterizedTest
    @MethodSource("provideInTestCases")
    @DisplayName("IN operation parameterized test")
    public void testInParameterized(List<String> sources, List<String> targets, boolean expected) {
        assertEquals(expected, OpType.IN.match(sources, targets));
    }

    @ParameterizedTest
    @MethodSource("provideNotInTestCases")
    @DisplayName("NOT_IN operation parameterized test")
    public void testNotInParameterized(List<String> sources, List<String> targets, boolean expected) {
        assertEquals(expected, OpType.NOT_IN.match(sources, targets));
    }

    // Test data providers
    private static Stream<Arguments> provideEqualTestCases() {
        return Stream.of(
                Arguments.of(Arrays.asList("a"), Arrays.asList("a"), true),
                Arguments.of(Arrays.asList("a"), Arrays.asList("b"), false),
                Arguments.of(Arrays.asList("a", "b"), Arrays.asList("a", "b"), true),
                Arguments.of(Arrays.asList("a", "b"), Arrays.asList("b", "a"), true),
                Arguments.of(Arrays.asList("a", "b"), Arrays.asList("a", "c"), false),
                Arguments.of(Arrays.asList("a", "b"), Arrays.asList("a"), false),
                Arguments.of(Collections.emptyList(), Collections.emptyList(), false),
                Arguments.of(null, null, false)
        );
    }

    private static Stream<Arguments> provideNotEqualTestCases() {
        return Stream.of(
                Arguments.of(Arrays.asList("a"), Arrays.asList("a"), false),
                Arguments.of(Arrays.asList("a"), Arrays.asList("b"), true),
                Arguments.of(Arrays.asList("a", "b"), Arrays.asList("a", "b"), false),
                Arguments.of(Arrays.asList("a", "b"), Arrays.asList("b", "a"), false),
                Arguments.of(Arrays.asList("a", "b"), Arrays.asList("a", "c"), true),
                Arguments.of(Arrays.asList("a", "b"), Arrays.asList("a"), true),
                Arguments.of(Collections.emptyList(), Collections.emptyList(), false),
                Arguments.of(null, null, false)
        );
    }

    private static Stream<Arguments> provideInTestCases() {
        return Stream.of(
                Arguments.of(Arrays.asList("a"), Arrays.asList("a", "b"), true),
                Arguments.of(Arrays.asList("a"), Arrays.asList("b", "c"), false),
                Arguments.of(Arrays.asList("a", "b"), Arrays.asList("a", "c", "d"), true),
                Arguments.of(Arrays.asList("a", "b"), Arrays.asList("c", "d", "e"), false),
                Arguments.of(Collections.emptyList(), Arrays.asList("a"), false),
                Arguments.of(Arrays.asList("a"), Collections.emptyList(), false),
                Arguments.of(null, null, false)
        );
    }

    private static Stream<Arguments> provideNotInTestCases() {
        return Stream.of(
                Arguments.of(Arrays.asList("a"), Arrays.asList("a", "b"), false),
                Arguments.of(Arrays.asList("a"), Arrays.asList("b", "c"), true),
                Arguments.of(Arrays.asList("a", "b"), Arrays.asList("a", "c", "d"), false),
                Arguments.of(Arrays.asList("a", "b"), Arrays.asList("c", "d", "e"), true),
                Arguments.of(Collections.emptyList(), Arrays.asList("a"), true),
                Arguments.of(Arrays.asList("a"), Collections.emptyList(), true),
                Arguments.of(null, null, true)
        );
    }

}
