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
package com.jd.live.agent.core.util.trie.hanks;

import com.jd.live.agent.bootstrap.util.Inclusion;
import com.jd.live.agent.core.util.trie.hankcs.AhoCorasickDoubleArrayTrie;
import com.jd.live.agent.core.util.trie.hankcs.AhoCorasickPredicateFactory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HankcsTest {

    private static AhoCorasickDoubleArrayTrie<Boolean> trie;

    private static final Set<String> names = new HashSet<>(Arrays.asList("x-live-space-id", "x-live-rule-id", "x-live-uid", "x-lane-space-id", "x-lane-code"));
    private static final Set<String> prefixes = new HashSet<>(Arrays.asList("x-live-", "x-service-", "x-lane-"));
    private static final Inclusion defaultInclusion = Inclusion.builder().names(names).prefixes(prefixes).build();
    private static final Inclusion hanksInclusion = Inclusion.builder().factory(AhoCorasickPredicateFactory.INSTANCE).names(names).prefixes(prefixes).build();

    // Test data
    private static final List<String> testCases = Arrays.asList(
            "x-live-space-id",  // exact match
            "x-live-extra",     // prefix match
            "invalid-header1",  // no match
            "invalid-header2",  // no match
            "invalid-header3",  // no match
            "invalid-header4",  // no match
            "x-service-123"     // prefix match
    );

    @Test
    void testCorrectness() {
        testCases.forEach(testCase -> {
            boolean defaultResult = defaultInclusion.test(testCase);
            boolean hanksResult = hanksInclusion.test(testCase);
            assertEquals(defaultResult, hanksResult, () ->
                    "Mismatch for case: " + testCase);
        });
    }

    @Test
    void testDefaultPerformance() {
        // Warmup
        for (int i = 0; i < 1000; i++) {
            testCases.forEach(defaultInclusion::test);
        }

        // Benchmark
        long start = System.nanoTime();
        int iterations = 100_000;
        for (int i = 0; i < iterations; i++) {
            testCases.forEach(defaultInclusion::test);
        }
        long duration = System.nanoTime() - start;

        System.out.printf("Default impl: %d ops in %d ms (%.2f ops/ms)%n",
                iterations * testCases.size(),
                duration / 1_000_000,
                (iterations * testCases.size()) / (duration / 1_000_000.0));
    }

    @Test
    void testHanksPerformance() {
        // Warmup
        for (int i = 0; i < 1000; i++) {
            testCases.forEach(hanksInclusion::test);
        }

        // Benchmark
        long start = System.nanoTime();
        int iterations = 100_000;
        for (int i = 0; i < iterations; i++) {
            testCases.forEach(hanksInclusion::test);
        }
        long duration = System.nanoTime() - start;

        // For small rule sets, the performance is lower than the default prefix matching implementation.
        System.out.printf("Aho-Corasick impl: %d ops in %d ms (%.2f ops/ms)%n",
                iterations * testCases.size(),
                duration / 1_000_000,
                (iterations * testCases.size()) / (duration / 1_000_000.0));
    }


}
