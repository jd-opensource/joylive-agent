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
package com.jd.live.agent.core.util.matcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class MatcherTest {

    @Test
    public void testDelimiterSegment() {

        PathMatcher.DelimiterSegment segment = new PathMatcher.DelimiterSegment('/');
        PathMatcher.Match match = segment.match("123456/456", 0);
        Assertions.assertFalse(match.isMatched());
        match = segment.find("123456/456", 0, false);
        Assertions.assertTrue(match.isMatched());
        Assertions.assertEquals(match.getIndex(), 6);
    }

    @Test
    public void testStringSegment() {
        PathMatcher.StringSegment segment = new PathMatcher.StringSegment("457", '/', true);
        PathMatcher.Match match = segment.match("123456123/123456", 0);
        Assertions.assertFalse(match.isMatched());
        match = segment.find("123457123/123456", 0, true);
        Assertions.assertFalse(match.isMatched());
        match = segment.find("123457123/123457", 0, false);
        Assertions.assertTrue(match.isMatched());
        Assertions.assertEquals(match.getIndex(), 13);
        segment.find("123457123/123457/", 0, false);
        Assertions.assertTrue(match.isMatched());
        Assertions.assertEquals(match.getIndex(), 13);
    }

    @Test
    public void testQuerySegment() {
        PathMatcher.QuerySegment segment = new PathMatcher.QuerySegment(3, '/', true);
        PathMatcher.Match match = segment.match("12", 0);
        Assertions.assertFalse(match.isMatched());
        match = segment.find("12/", 0, true);
        Assertions.assertFalse(match.isMatched());
        match = segment.find("123/123457", 0, false);
        Assertions.assertTrue(match.isMatched());
        Assertions.assertEquals(match.getIndex(), 0);
        match = segment.find("12/345", 0, false);
        Assertions.assertTrue(match.isMatched());
        Assertions.assertEquals(match.getIndex(), 3);
        match = segment.find("12/345/", 0, false);
        Assertions.assertTrue(match.isMatched());
        Assertions.assertEquals(match.getIndex(), 3);
        segment = new PathMatcher.QuerySegment(3, '/', false);
        match = segment.find("123/123457", 1, false);
        Assertions.assertTrue(match.isMatched());
        Assertions.assertEquals(match.getIndex(), 4);
    }

    @Test
    public void testAsteriskSegment() {
        PathMatcher.AsteriskSegment segment = new PathMatcher.AsteriskSegment('/', true);
        PathMatcher.Match match = segment.match("12/", 0);
        Assertions.assertTrue(match.isMatched());
        Assertions.assertEquals(match.getIndex(), 0);
        Assertions.assertEquals(match.getLength(), 0);
        match = segment.find("12/", 0, true);
        Assertions.assertTrue(match.isMatched());
        Assertions.assertEquals(match.getIndex(), 0);
        Assertions.assertEquals(match.getLength(), 2);
    }

    @Test
    public void testParse() {
        List<PathMatcher.Segment> segments = PathMatcher.parse("/abcd/??cde/*cf/**", '/');
        Assertions.assertEquals(segments.size(), 10);
    }
}
