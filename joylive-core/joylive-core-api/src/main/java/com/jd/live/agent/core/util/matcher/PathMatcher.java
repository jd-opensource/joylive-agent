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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Deprecated
public class PathMatcher implements Matcher<String> {

    public static final char PATH_DELIMITER = '/';
    protected static final char CHAR_QUERY = '?';
    protected static final char CHAR_ASTERISK = '*';

    private final String pattern;

    private final char delimiter;

    private final Segment[] segments;

    public PathMatcher(String pattern) {
        this(pattern, PATH_DELIMITER);
    }

    public PathMatcher(String pattern, char delimiter) {
        this.pattern = pattern;
        this.delimiter = delimiter;
        this.segments = parse(pattern, delimiter).toArray(new Segment[0]);
    }

    public boolean match(String source) {
        if (source == null || source.isEmpty() || segments.length == 0) {
            return false;
        }

        Match match;
        int index = 0;
        boolean partial = true;
        boolean find = false;
        List<Match> wildcards = new LinkedList<>();
        for (Segment segment : segments) {
            match = !find ? segment.match(source, index) : segment.find(source, index, partial);
            if (match.isMatched()) {
                switch (match.getType()) {
                    case Segment.TYPE_STRING:
                    case Segment.TYPE_DELIMITER:
                    case Segment.TYPE_QUERY:
                        index = match.getIndex() + match.getLength();
                        break;
                    case Segment.TYPE_ASTERISK:
                        find = true;
                        break;
                    case Segment.TYPE_DOUBLE_ASTERISK:
                        find = true;
                        partial = false;
                        break;
                }
            } else if (!partial) {
                return false;
            } else {
                wildcards.add(match);
            }
        }
        if (!wildcards.isEmpty()) {
            while (true) {
                // TODO Return when what follows the first ** can no longer be matched
                for (Match wildcard : wildcards) {
                    index = wildcard.getLength();
                    wildcard.find(source, index, partial);
                    if (!wildcard.isMatched()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    protected static List<Segment> parse(String pattern, char delimiter) {
        List<Segment> result = new ArrayList<>();
        if (pattern != null && !pattern.isEmpty()) {
            char ch;
            int start = -1;
            int end = -1;
            int asterisk = 0;
            int queries = 0;
            for (int i = 0; i < pattern.length(); i++) {
                ch = pattern.charAt(i);
                if (ch == delimiter) {
                    if (asterisk > 0) {
                        result.add(asterisk > 1 ? new DoubleAsteriskSegment(true) : new AsteriskSegment(delimiter, true));
                        asterisk = 0;
                    } else if (queries > 0) {
                        result.add(new QuerySegment(queries, delimiter, true));
                        queries = 0;
                    } else if (end >= 0) {
                        result.add(new StringSegment(pattern.substring(start, end), delimiter, true));
                        end = -1;
                    }
                    result.add(new DelimiterSegment(delimiter));
                } else if (ch == CHAR_QUERY) {
                    if (asterisk > 0) {
                        result.add(asterisk > 1 ? new DoubleAsteriskSegment(false) : new AsteriskSegment(delimiter, false));
                        asterisk = 0;
                    } else if (end >= 0) {
                        result.add(new StringSegment(pattern.substring(start, end), delimiter,
                                i == pattern.length() - 1 || i < pattern.length() - 1 && pattern.charAt(i + 1) == delimiter));
                        end = -1;
                    }
                    queries++;
                } else if (ch == CHAR_ASTERISK) {
                    if (queries > 0) {
                        result.add(new QuerySegment(queries, delimiter, false));
                        queries = 0;
                    } else if (end >= 0) {
                        result.add(new StringSegment(pattern.substring(start, end), delimiter, false));
                        end = -1;
                    }
                    asterisk++;
                } else {
                    if (asterisk > 0) {
                        result.add(asterisk > 1 ? new DoubleAsteriskSegment(false) : new AsteriskSegment(delimiter, false));
                        asterisk = 0;
                    } else if (queries > 0) {
                        result.add(new QuerySegment(queries, delimiter, false));
                        queries = 0;
                    }
                    if (end < 0) {
                        start = i;
                    }
                    end = i;
                }
            }
            if (asterisk > 0) {
                result.add(asterisk > 1 ? new DoubleAsteriskSegment(true) : new AsteriskSegment(delimiter, true));
            } else if (queries > 0) {
                result.add(new QuerySegment(queries, delimiter, true));
            } else if (end >= 0) {
                result.add(new StringSegment(pattern.substring(start), delimiter, true));
            }
        }
        return result;
    }

    protected static class Match {
        private final Segment segment;

        private boolean matched;

        private int index;

        private int length;

        public Match(Segment segment, boolean matched, int index, int length) {
            this.segment = segment;
            this.matched = matched;
            this.index = index;
            this.length = length;
        }

        public Segment getSegment() {
            return segment;
        }

        public boolean isMatched() {
            return matched;
        }

        public int getIndex() {
            return index;
        }

        public int getLength() {
            return length;
        }

        public int getType() {
            return segment.getType();
        }

        public void find(String source, int index, boolean part) {
            Match result = segment.find(source, index, part);
            this.matched = result.matched;
            this.index = result.index;
            this.length = result.length;
        }
    }

    protected interface Segment {

        int TYPE_STRING = 0;

        int TYPE_DELIMITER = 1;

        int TYPE_QUERY = 2;

        int TYPE_ASTERISK = 3;

        int TYPE_DOUBLE_ASTERISK = 4;

        Match match(String source, int index);

        Match find(String source, int index, boolean partial);

        boolean isPartialEnd();

        int getType();

    }

    protected static class DelimiterSegment implements Segment {

        private final char delimiter;

        public DelimiterSegment(char delimiter) {
            this.delimiter = delimiter;
        }

        @Override
        public Match match(String source, int index) {
            boolean matched = index < source.length() && source.charAt(index) == delimiter;
            return new Match(this, matched, index, 1);
        }

        @Override
        public Match find(String source, int index, boolean partial) {
            int length = source.length();
            for (int i = index; i < length; i++) {
                if (source.charAt(i) == delimiter) {
                    return new Match(this, true, i, 1);
                }
            }
            return new Match(this, false, index, 1);
        }

        @Override
        public boolean isPartialEnd() {
            return false;
        }

        @Override
        public int getType() {
            return TYPE_DELIMITER;
        }
    }

    protected static class StringSegment implements Segment {

        private final String value;

        private final char delimiter;

        private final boolean partialEnd;

        private final int length;

        private final char firstChar;

        public StringSegment(String value, char delimiter, boolean partialEnd) {
            this.value = value;
            this.delimiter = delimiter;
            this.partialEnd = partialEnd;
            this.length = value.length();
            this.firstChar = value.charAt(0);
        }

        @Override
        public Match match(String source, int index) {
            boolean matched = index < source.length() && value.equals(source.substring(index));
            return new Match(this, matched, index, source.length());
        }

        @Override
        public Match find(String source, int index, boolean partial) {
            int srcLength = source.length();
            char srcCh;
            int i = index;
            int j;
            int max = srcLength - length;
            while (i <= max) {
                srcCh = source.charAt(i);
                if (srcCh == delimiter) {
                    if (partial) return new Match(this, false, index, length);
                } else if (srcCh == firstChar) {
                    j = 0;
                    while (++j < length) {
                        srcCh = source.charAt(i + j);
                        if (value.charAt(j) != srcCh) {
                            break;
                        }
                    }
                    if (j == length) {
                        if (!partialEnd || i + j == srcLength || source.charAt(i + j) == delimiter) {
                            return new Match(this, true, i, length);
                        }
                    } else if (srcCh == delimiter) {
                        i += j - 1;
                    }
                }
                i++;
            }
            return new Match(this, false, index, length);
        }

        @Override
        public boolean isPartialEnd() {
            return false;
        }

        @Override
        public int getType() {
            return TYPE_STRING;
        }
    }

    protected static class QuerySegment implements Segment {

        private final int count;

        private final char delimiter;

        private final boolean partialEnd;

        public QuerySegment(int count, char delimiter, boolean partialEnd) {
            this.count = count;
            this.delimiter = delimiter;
            this.partialEnd = partialEnd;
        }

        @Override
        public Match match(String source, int index) {
            int i = index;
            int max = source.length() - count;
            int cnt = 0;
            while (i <= max) {
                if (source.charAt(i++) == delimiter) {
                    return new Match(this, false, index, count);
                } else if (++cnt == count) {
                    return new Match(this, true, index, count);
                }
            }
            return new Match(this, false, index, count);
        }

        @Override
        public Match find(String source, int index, boolean partial) {
            int i = index;
            int length = source.length();
            int start = i;
            while (i < length) {
                if (source.charAt(i++) == delimiter) {
                    if (partialEnd && i - start - 1 >= count) {
                        return new Match(this, true, start, count);
                    } else if (partial) {
                        return new Match(this, false, index, count);
                    } else {
                        start = i;
                    }
                } else if ((i - start) == count && (!partialEnd || i == length)) {
                    return new Match(this, true, start, count);
                }
            }
            return new Match(this, false, index, count);
        }

        @Override
        public boolean isPartialEnd() {
            return partialEnd;
        }

        @Override
        public int getType() {
            return TYPE_QUERY;
        }
    }

    protected static class AsteriskSegment implements Segment {

        private final char delimiter;

        private final boolean partialEnd;

        public AsteriskSegment(char delimiter, boolean partialEnd) {
            this.delimiter = delimiter;
            this.partialEnd = partialEnd;
        }

        @Override
        public Match match(String source, int index) {
            return new Match(this, true, index, 0);
        }

        @Override
        public Match find(String source, int index, boolean partial) {
            int i = index;
            if (partialEnd) {
                while (i < source.length()) {
                    if (source.charAt(i) == delimiter) {
                        break;
                    }
                    i++;
                }
            }
            return new Match(this, true, index, i - index);
        }

        @Override
        public boolean isPartialEnd() {
            return partialEnd;
        }

        @Override
        public int getType() {
            return TYPE_ASTERISK;
        }
    }

    protected static class DoubleAsteriskSegment implements Segment {

        private final boolean partialEnd;

        public DoubleAsteriskSegment(boolean partialEnd) {
            this.partialEnd = partialEnd;
        }

        @Override
        public Match match(String source, int index) {
            return new Match(this, true, index, 0);
        }

        @Override
        public Match find(String source, int index, boolean partial) {
            return new Match(this, true, index, 0);
        }

        @Override
        public boolean isPartialEnd() {
            return partialEnd;
        }

        @Override
        public int getType() {
            return TYPE_DOUBLE_ASTERISK;
        }
    }
}
