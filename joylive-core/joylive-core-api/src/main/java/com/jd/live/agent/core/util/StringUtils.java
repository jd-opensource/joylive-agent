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
package com.jd.live.agent.core.util;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * StringUtils
 */
public class StringUtils {

    /**
     * A character is a comma.
     */
    public static final char CHAR_COMMA = ',';

    /**
     * A character is a equal.
     */
    public static final char CHAR_EQUAL = '=';

    /**
     * A character is a pipe.
     */
    public static final char CHAR_AMPERSAND = '|';

    /**
     * A predicate that tests if a character is a comma.
     */
    public static final Predicate<Character> COMMA = o -> o == ',';

    /**
     * A predicate that tests if a character is a comma or pipe.
     */
    public static final Predicate<Character> PIPE_COMMA = o -> {
        switch (o) {
            case ',':
            case '|':
                return true;
            default:
                return false;
        }
    };

    /**
     * A predicate that tests if a character is a comma or semicolon.
     */
    public static final Predicate<Character> SEMICOLON_COMMA = o -> {
        switch (o) {
            case ',':
            case ';':
                return true;
            default:
                return false;
        }
    };

    /**
     * A predicate that tests if a character is a comma or semicolon or line break.
     */
    public static final Predicate<Character> SEMICOLON_COMMA_LINE = o -> {
        switch (o) {
            case ',':
            case ';':
            case '\n':
                return true;
            default:
                return false;
        }
    };

    /**
     * A predicate that tests if a character is a comma, semicolon, or whitespace.
     */
    public static final Predicate<Character> SEMICOLON_COMMA_WHITESPACE = o -> {
        switch (o) {
            case ',':
            case ';':
                return true;
            default:
                return Character.isWhitespace(o);
        }
    };

    /**
     * The empty String {@code ""}.
     */
    public static final String EMPTY = "";

    /**
     * Checks if the given character sequence is empty (null or has a length of 0).
     *
     * @param cs The character sequence to check.
     * @return true if the character sequence is null or has a length of 0, false otherwise.
     */
    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * Checks if the given character sequence is blank (null, has a length of 0, or contains only whitespace characters).
     *
     * @param cs The character sequence to check.
     * @return true if the character sequence is null, has a length of 0, or contains only whitespace characters; false otherwise.
     */
    public static boolean isBlank(CharSequence cs) {
        int len;
        if (cs == null || (len = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the given character sequence is blank (null, has a length of 0, or contains only whitespace characters).
     *
     * @param cs The character sequence to check.
     * @return true if the character sequence is null, has a length of 0, or contains only whitespace characters; false otherwise.
     */
    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * Checks if two CharSequences are equal or if one of them is an empty string.
     *
     * @param cs1 the first CharSequence to compare
     * @param cs2 the second CharSequence to compare
     * @return true if the CharSequences are equal or if one of them is an empty string, false otherwise
     */
    public static boolean isEqualsOrEmpty(CharSequence cs1, CharSequence cs2) {
        if (cs1 != null && cs1.length() > 0) {
            return cs1.equals(cs2);
        } else {
            return cs2 == null || cs2.length() == 0;
        }
    }

    /**
     * Appends a key-value pair to the provided StringBuilder with a specified separator.
     * If the value is null or empty, the behavior depends on the nullable flag.
     * If nullable is true, only the key is appended. Otherwise, nothing is appended.
     *
     * @param builder   the StringBuilder to which the key-value pair will be appended
     * @param separator the character used to separate key-value pairs in the StringBuilder
     * @param key       the key to be appended
     * @param value     the value to be appended
     * @param emptyable A flag indicating whether to append the key if the value is null or empty
     */
    public static void append(StringBuilder builder, char separator, String key, String value, boolean emptyable) {
        if (builder.length() > 0) {
            builder.append(separator);
        }
        if (value == null || value.isEmpty()) {
            if (emptyable) {
                builder.append(key);
            }
        } else {
            builder.append(key).append(CHAR_EQUAL).append(value);
        }

    }

    /**
     * Splits the provided source string into an array of strings, using the provided character as the delimiter.
     *
     * @param source The string to be split.
     * @param ch     The delimiter character.
     * @return An array of strings.
     */
    public static String[] split(final String source, final char ch) {
        return split(source, o -> o == ch, null);
    }

    /**
     * Splits the provided source string into an array of strings based on a predefined set of delimiters.
     *
     * @param source The string to be split.
     * @return An array of strings.
     */
    public static String[] split(final String source) {
        return split(source, SEMICOLON_COMMA_WHITESPACE, null);
    }

    /**
     * Splits the given source string based on a specified predicate logic.
     *
     * @param source    The source string to split.
     * @param predicate A character predicate that determines whether a character should be considered a splitting point.
     * @return An array of strings.
     */
    public static String[] split(final String source, final Predicate<Character> predicate) {
        return split(source, predicate, null);
    }

    /**
     * Splits the given source string based on a specified predicate and processes each segment using a provided handler function.
     * This method iterates over the characters of the source string and uses the predicate to decide where to split.
     * Once a segment is identified, it is processed by the handler function if provided, before being added to the result.
     *
     * @param source    The source string to be split.
     * @param predicate A predicate that determines whether a character should be considered a splitting point.
     * @param handler   A function that processes each extracted segment before adding it to the result. May be null.
     * @return An array of strings resulting.
     */
    public static String[] split(final String source,
                                 final Predicate<Character> predicate,
                                 final Function<String, String> handler) {
        List<String> targets = splitList(source, predicate, handler);
        return targets.isEmpty() ? new String[0] : targets.toArray(new String[0]);
    }

    /**
     * Splits the given string by the specified delimiter string.
     * This method supports splitting by both single character and multi-character delimiters.
     * If the delimiter is null or empty, it defaults to using a comma as the delimiter.
     * For single-character delimiters, it utilizes a more optimized path.
     *
     * @param value     The string to be split.
     * @param delimiter The delimiter string used for splitting the value. Can be a single character or multiple characters.
     * @return An array of strings.
     */
    public static String[] split(final String value, final String delimiter) {
        List<String> targets = splitList(value, delimiter);
        return targets.isEmpty() ? new String[0] : targets.toArray(new String[0]);
    }

    /**
     * Splits the provided source string into a list of strings, using the provided character as the delimiter.
     *
     * @param source The string to be split.
     * @param ch     The delimiter character.
     * @return A list of strings.
     */
    public static List<String> splitList(final String source, final char ch) {
        return splitList(source, o -> o == ch, null);
    }

    /**
     * Splits the provided source string into a list of strings based on a predefined set of delimiters.
     *
     * @param source The string to be split.
     * @return A list of strings.
     */
    public static List<String> splitList(final String source) {
        return splitList(source, SEMICOLON_COMMA_WHITESPACE, null);
    }

    /**
     * Splits the given source string based on a specified predicate logic.
     *
     * @param source    The source string to split.
     * @param predicate A character predicate that determines whether a character should be considered a splitting point.
     * @return A list of strings.
     */
    public static List<String> splitList(final String source, final Predicate<Character> predicate) {
        return splitList(source, predicate, null);
    }

    /**
     * Splits the given source string based on a specified predicate logic and applies a handler function to each resulting substring.
     *
     * @param source    The source string to split.
     * @param separator A character predicate that determines whether a character should be considered a splitting point.
     * @param converter A function that takes a substring as input and returns a processed substring.
     * @return A list of processed strings.
     */
    public static List<String> splitList(final String source,
                                         final Predicate<Character> separator,
                                         final Function<String, String> converter) {
        List<String> result = new ArrayList<>();
        splitList(source, separator, true, false, converter, result::add);
        return result;
    }

    /**
     * Splits the given source string into substrings using the specified predicate logic to determine the substring separation.
     * Each resulting substring can be trimmed, filtered based on emptiness, and optionally converted using a provided function
     * before being passed to a consumer function.
     *
     * @param source    The source string to be split.
     * @param separator A character predicate that determines whether a character should be considered a substring separator.
     * @param trim      A flag indicating whether to trim each substring before applying the converter and consumer functions.
     * @param emptyable A flag indicating whether to include empty substrings in the processing.
     * @param converter A function that converts each substring before it is passed to the consumer. If null, no conversion is applied.
     * @param consumer  A consumer function that takes a substring as input and performs an action.
     */
    public static void splitList(final String source,
                                 final Predicate<Character> separator,
                                 final boolean trim,
                                 final boolean emptyable,
                                 final Function<String, String> converter,
                                 final Consumer<String> consumer) {
        if (source == null || source.isEmpty() || separator == null || consumer == null) {
            return;
        }
        int start = -1;
        int end = -1;
        char ch;
        String part;
        int length = source.length();

        // Iterate over characters
        for (int i = 0; i < length; i++) {
            ch = source.charAt(i);
            // Check if the character matches the predicate
            if (separator.test(ch)) {
                // If there is a segment before this character
                if (start >= 0) {
                    part = source.substring(start, end + 1);
                    part = trim ? part.trim() : part;
                    if (emptyable || !part.isEmpty()) {
                        part = converter == null ? part : converter.apply(part);
                        consumer.accept(part);
                    }
                    start = -1;
                    end = -1;
                }
            } else {
                if (start == -1) {
                    start = i;
                }
                end = i;
            }
        }
        // Handle the last segment
        if (start >= 0) {
            part = source.substring(start, length);
            part = trim ? part.trim() : part;
            if (emptyable || !part.isEmpty()) {
                part = converter == null ? part : converter.apply(part);
                consumer.accept(part);
            }
        }
    }

    /**
     * Splits the given input string into a list of substrings, using the specified delimiter.
     *
     * @param value     The input string to be split.
     * @param delimiter The delimiter used to separate the substrings.
     * @return A list of substrings.
     */
    public static List<String> splitList(final String value, final String delimiter) {
        List<String> result = new ArrayList<>();
        splitList(value, delimiter, true, false, result::add);
        return result;
    }

    /**
     * Splits the given value string into a list of substrings using the specified delimiter to determine the substring separation,
     * and applies a consumer function to each resulting substring. The method also provides options to trim the substrings
     * and skip empty substrings.
     *
     * @param value     The value string to be split.
     * @param delimiter The delimiter string used to separate the substrings. If null or empty, a default delimiter (SEMICOLON_COMMA)
     *                  will be used.
     * @param trim      A flag indicating whether to trim each substring before applying the consumer function.
     * @param emptyable A flag indicating whether to include empty substrings in the processing.
     * @param consumer  A consumer function that takes a substring as input and performs an action.
     */
    public static void splitList(final String value,
                                 final String delimiter,
                                 final boolean trim,
                                 final boolean emptyable,
                                 final Consumer<String> consumer) {
        if (value == null || value.isEmpty() || consumer == null) {
            return;
        } else if (delimiter == null || delimiter.isEmpty()) {
            splitList(value, SEMICOLON_COMMA, trim, emptyable, null, consumer);
        } else if (delimiter.length() == 1) {
            char ch = delimiter.charAt(0);
            splitList(value, c -> ch == c, trim, emptyable, null, consumer);
        } else {
            int length = value.length();
            int maxPos = delimiter.length() - 1;
            int start = 0;
            int pos = 0;
            int end = 0;
            String part;
            for (int i = 0; i < length; i++) {
                if (value.charAt(i) == delimiter.charAt(pos)) {
                    if (pos++ == maxPos) {
                        if (end > start) {
                            part = value.substring(start, end + 1);
                            part = trim ? part.trim() : part;
                            if (emptyable || !part.isEmpty()) {
                                consumer.accept(part);
                            }
                        }
                        pos = 0;
                        start = i + 1;
                    }
                } else {
                    end = i;
                }
            }
            if (start < length) {
                part = value.substring(start, length);
                part = trim ? part.trim() : part;
                if (emptyable || !part.isEmpty()) {
                    consumer.accept(part);
                }
            }
        }
    }

    /**
     * Splits the given source string into a map of key-value pairs, using a predefined set of delimiters to determine the key-value separation.
     *
     * @param source The source string to be split.
     * @return A map of key-value pairs.
     */
    public static Map<String, String> splitMap(final String source) {
        return splitMap(source, SEMICOLON_COMMA);
    }

    /**
     * Splits the given source string into a map of key-value pairs, using the specified predicate logic to determine the key-value separation.
     *
     * @param source    The source string to be split.
     * @param predicate A character predicate that determines whether a character should be considered a key-value separator.
     * @return A map of key-value pairs.
     */
    public static Map<String, String> splitMap(final String source, final Predicate<Character> predicate) {
        Map<String, String> result = new HashMap<>();
        splitMap(source, predicate, true, (key, value) -> {
            result.put(key, value);
            return true;
        });
        return result;
    }

    /**
     * Splits a source string into key-value pairs based on a given separator predicate and applies a function to each pair.
     * The method processes the source string, identifies key-value pairs separated by an '=' character, and uses the separator
     * predicate to determine where each pair ends. If the trim flag is set to true, it trims whitespace from keys and values.
     * The function is applied to each key-value pair, and the method returns the count of pairs for which the function returns true.
     *
     * @param source    the source string containing key-value pairs
     * @param separator a predicate that determines the end of each key-value pair segment
     * @param trim      a flag indicating whether to trim whitespace from keys and values
     * @param function  a function to apply to each key-value pair, returning true if the pair should be counted
     * @return the count of key-value pairs for which the function returns true
     */
    public static int splitMap(final String source,
                               final Predicate<Character> separator,
                               final boolean trim,
                               final BiFunction<String, String, Boolean> function) {
        if (source == null || source.isEmpty() || function == null || separator == null) {
            return 0;
        }
        int counter = 0;
        int start = -1;
        int end = -1;
        char ch;
        int pos = -1;
        int length = source.length();
        String key;
        String value;
        // Iterate over characters
        for (int i = 0; i < length; i++) {
            ch = source.charAt(i);
            if (ch == CHAR_EQUAL && pos < 0) {
                pos = i;
            }
            // Check if the character matches the predicate
            if (separator.test(ch)) {
                // If there is a segment before this character
                if (start >= 0) {
                    if (pos > 0) {
                        key = source.substring(start, pos);
                        key = trim ? key.trim() : key;
                        value = source.substring(pos + 1, end + 1);
                        value = trim ? value.trim() : value;
                        if (!key.isEmpty()) {
                            counter += function.apply(key, value) ? 1 : 0;
                        }
                    }
                    start = -1;
                    end = -1;
                    pos = -1;
                }
            } else {
                if (start == -1) {
                    start = i;
                }
                end = i;
            }
        }
        // Handle the last segment
        if (start >= 0) {
            if (pos > 0) {
                key = source.substring(start, pos);
                key = trim ? key.trim() : key;
                value = source.substring(pos + 1, length);
                value = trim ? value.trim() : value;
                if (!key.isEmpty()) {
                    counter += function.apply(key, value) ? 1 : 0;
                }
            }
        }
        return counter;
    }

    /**
     * Joins an array of values into a single string with a specified separator.
     * This method will skip any empty values.
     *
     * @param values   The array of values to join.
     * @param separator The separator to use between each string.
     * @return A string that consists of the input values separated by the specified separator.
     */
    public static String join(String[] values, char separator) {
        if (values == null || values.length == 0) {
            return EMPTY;
        }
        return join(Arrays.asList(values), separator, (char) 0, (char) 0, false);
    }

    /**
     * Joins a collection of values into a single string with a specified separator.
     * This method will skip any empty values.
     *
     * @param values            The collection of values to join.
     * @param separator         The separator to use between each string.
     * @param prefix            The prefix to add at the beginning of the resulting string.
     * @param suffix            The suffix to add at the end of the resulting string.
     * @param singleSurrounding A flag to determine whether to include the prefix and suffix
     *                          when there is only one non-empty value. If true, the prefix and
     *                          suffix are included; if false, they are not.
     * @return A string that consists of the input values separated by the specified separator,
     * enclosed by the specified prefix and suffix. If all values are empty or the collection
     * is null or empty, an empty string is returned.
     */
    public static String join(Iterable<String> values, char separator, char prefix, char suffix, boolean singleSurrounding) {
        if (values == null) {
            return EMPTY;
        } else if (values instanceof List) {
            return joinList((List<String>) values, separator, prefix, suffix, singleSurrounding);
        } else if (values instanceof Collection) {
            return joinCollection((Collection<String>) values, separator, prefix, suffix, singleSurrounding);
        }
        return joinIterable(values, separator, prefix, suffix, singleSurrounding);
    }

    /**
     * Joins a list of values into a single string with a specified separator.
     * This method will skip any empty values.
     *
     * @param values            The list of values to join.
     * @param separator         The separator to use between each string.
     * @param prefix            The prefix to add at the beginning of the resulting string.
     * @param suffix            The suffix to add at the end of the resulting string.
     * @param singleSurrounding A flag to determine whether to include the prefix and suffix
     *                          when there is only one non-empty value. If true, the prefix and
     *                          suffix are included; if false, they are not.
     * @return A string that consists of the input values separated by the specified separator,
     * enclosed by the specified prefix and suffix. If all values are empty or the list
     * is null or empty, an empty string is returned.
     */
    private static String joinList(List<String> values, char separator, char prefix, char suffix, boolean singleSurrounding) {
        if (values.isEmpty()) {
            return EMPTY;
        } else if (values.size() == 1) {
            String value = values.get(0);
            return singleSurrounding ? prefix + value + suffix : value;
        }
        return joinIterable(values, separator, prefix, suffix, singleSurrounding);
    }

    /**
     * Joins a collection of values into a single string with a specified separator.
     * This method will skip any empty values.
     *
     * @param values            The collection of values to join.
     * @param separator         The separator to use between each string.
     * @param prefix            The prefix to add at the beginning of the resulting string.
     * @param suffix            The suffix to add at the end of the resulting string.
     * @param singleSurrounding A flag to determine whether to include the prefix and suffix
     *                          when there is only one non-empty value. If true, the prefix and
     *                          suffix are included; if false, they are not.
     * @return A string that consists of the input values separated by the specified separator,
     * enclosed by the specified prefix and suffix. If all values are empty or the collection
     * is null or empty, an empty string is returned.
     */
    private static String joinCollection(Collection<String> values, char separator, char prefix, char suffix, boolean singleSurrounding) {
        if (values.isEmpty()) {
            return EMPTY;
        } else if (values.size() == 1) {
            String value = values.iterator().next();
            return singleSurrounding ? prefix + value + suffix : value;
        }
        return joinIterable(values, separator, prefix, suffix, singleSurrounding);
    }

    /**
     * Joins an iterable of values into a single string with a specified separator.
     * This method will skip any empty values.
     *
     * @param values            The iterable of values to join.
     * @param separator         The separator to use between each string.
     * @param prefix            The prefix to add at the beginning of the resulting string.
     * @param suffix            The suffix to add at the end of the resulting string.
     * @param singleSurrounding A flag to determine whether to include the prefix and suffix
     *                          when there is only one non-empty value. If true, the prefix and
     *                          suffix are included; if false, they are not.
     * @return A string that consists of the input values separated by the specified separator,
     * enclosed by the specified prefix and suffix. If all values are empty or the iterable
     * is null or empty, an empty string is returned.
     */
    private static String joinIterable(Iterable<String> values, char separator, char prefix, char suffix, boolean singleSurrounding) {
        int left = prefix == 0 ? 0 : 1;
        int right = suffix == 0 ? 0 : 1;
        int counter = 0;
        StringBuilder sb = new StringBuilder();
        sb.append(left == 0 ? "" : prefix);
        for (String string : values) {
            if (!isEmpty(string)) {
                if (counter++ > 0) {
                    sb.append(separator);
                }
                sb.append(string);
            }
        }
        sb.append(right == 0 ? "" : suffix);
        switch (counter) {
            case 0:
                return EMPTY;
            case 1:
                return singleSurrounding ? sb.toString() : sb.substring(left, sb.length() - right);
            default:
                return sb.toString();
        }
    }

    /**
     * Concatenates a URL with a single path, handling edge cases for slashes.
     *
     * @param url  The base URL.
     * @param path The path to append.
     * @return The concatenated URL.
     */
    public static String url(String url, String path) {
        if (path == null || path.isEmpty()) {
            return url;
        } else if (url == null) {
            return path;
        } else if (url.endsWith("/")) {
            return url + (path.startsWith("/") ? path.substring(1) : path);
        } else {
            return url + (path.startsWith("/") ? path : "/" + path);
        }
    }

    /**
     * Concatenates a URL with multiple paths, handling edge cases for slashes.
     *
     * @param url   The base URL.
     * @param paths The paths to append.
     * @return The concatenated URL.
     */
    public static String url(String url, String... paths) {
        if (paths == null) {
            return url;
        }
        String result = url;
        for (String path : paths) {
            result = url(result, path);
        }
        return result;
    }
}