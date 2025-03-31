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
package com.jd.live.agent.core.util.template;

import com.jd.live.agent.core.util.type.ValuePath;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A template engine that processes a template string with variable placeholders.
 * Variables are denoted by the pattern ${anything}.
 */
public class Template implements Evaluator {

    /**
     * The original template source string.
     */
    @Getter
    private final String source;

    /**
     * A list of evaluators that represent the parsed template sections.
     */
    private final List<Section> sections = new ArrayList<>();

    /**
     * The capacity for the StringBuilder used in evaluation.
     */
    private final int capacity;

    /**
     * The count of variables found in the template.
     */
    @Getter
    private final int variables;

    /**
     * Constructs a new Template with the given source string.
     *
     * @param source the template source string
     */
    public Template(String source) {
        this(source, 0);
    }

    /**
     * Constructs a new Template with the given source string and capacity.
     *
     * @param source   the template source string
     * @param capacity the initial capacity for the StringBuilder
     */
    public Template(String source, int capacity) {
        this.source = source;
        this.capacity = capacity;
        this.variables = source == null ? 0 : parse(source.toCharArray(), 0, source.length(), sections, false);
    }

    @Override
    public Object evaluate(Object context) {
        return evaluate(context, true);
    }

    /**
     * Evaluates the template using the provided context. If {@code nullable} is true,
     * missing variables are treated as null; otherwise, they are treated as empty strings.
     *
     * @param context  the object containing variable values
     * @param nullable whether to allow null values for missing variables
     * @return the evaluated result
     */
    public Object evaluate(Object context, boolean nullable) {
        return doEvaluate(new EvalContext(context, nullable), sections);
    }

    /**
     * Evaluates the given expression using the provided context, allowing null values by default.
     *
     * @param expression the expression to evaluate
     * @param context    the object containing variable values
     * @return the evaluated result
     */
    public static Object evaluate(String expression, Object context) {
        return evaluate(expression, context, true);
    }

    /**
     * Evaluates the given expression using the provided context, with optional null value handling.
     *
     * @param expression the expression to evaluate
     * @param context    the object containing variable values
     * @param nullable   whether to allow null values for missing variables
     * @return the evaluated result
     */
    public static Object evaluate(String expression, Object context, boolean nullable) {
        return parse(expression).evaluate(context, nullable);
    }

    /**
     * Evaluates the given expression using the provided key-value pairs, allowing null values by default.
     *
     * @param expression the expression to evaluate
     * @param args       the key-value pairs (e.g., "key1", "value1", "key2", "value2")
     * @return the evaluated result
     */
    public static String evaluate(String expression, String... args) {
        return evaluate(expression, true, args);
    }

    /**
     * Evaluates the given expression using the provided key-value pairs, with optional null value handling.
     *
     * @param expression the expression to evaluate
     * @param nullable   whether to allow null values for missing variables
     * @param args       the key-value pairs (e.g., "key1", "value1", "key2", "value2")
     * @return the evaluated result
     */
    public static String evaluate(String expression, boolean nullable, String... args) {
        Map<String, Object> context = context(args);
        Object result = parse(expression).evaluate(context, nullable);
        return result == null ? null : result.toString();
    }

    /**
     * Creates a context map from alternating key-value pairs.
     * <p>
     * Example usage: {@code context("name", "value", "id", 123)} creates
     * a map with two entries.
     *
     * @param args alternating key-value pairs (keys must be Strings, values can be any Object).
     *             If null or odd-length array, trailing elements are ignored.
     * @return a new map containing the key-value pairs (never null)
     */
    public static Map<String, Object> context(String... args) {
        Map<String, Object> context = new HashMap<>();
        if (args != null) {
            int len = args.length / 2;
            for (int i = 0; i < len; i++) {
                context.put(args[i * 2], args[i * 2 + 1]);
            }
        }
        return context;
    }

    /**
     * Parses the given expression into a {@link Template} object.
     *
     * @param expression the string expression to parse
     * @return a new {@link Template} instance
     */
    public static Template parse(String expression) {
        return new Template(expression);
    }

    /**
     * Parses the template expression to count variables and create sections for evaluation.
     * This method iterates through the characters of the expression once, identifying
     * variable positions and default values, and constructs the corresponding sections.
     * It supports nested variables, mixed static-dynamic values, and handles unclosed
     * variable expressions (e.g., ${a:) as static text.
     *
     * @param chars    the character array of the expression
     * @param start    the start index of the expression to parse
     * @param end      the end index of the expression to parse
     * @param sections the list to which the parsed sections will be added
     * @return the count of variables in the expression
     */
    private int parse(char[] chars, int start, int end, List<Section> sections, boolean defMode) {
        if (chars == null || start >= end) {
            return 0;
        }

        int varCount = 0;
        int i = start;
        int sectionStart = start; // Start position of the current section

        while (i < end) {
            if (chars[i] == '$' && i + 1 < end && chars[i + 1] == '{') {
                // Found the start of a valid variable: ${
                if (sectionStart < i) {
                    // Add the preceding static text as a StringSection
                    sections.add(new StringSection(new Position(sectionStart, i), new String(chars, sectionStart, i - sectionStart)));
                }

                // Parse the variable
                i += 2; // Skip ${
                int varStart = i;
                // Find the end of the variable
                int[] varIndices = parseVariable(chars, i, end);
                int varEnd = varIndices[0];
                int defStart = varIndices[1];
                if (varEnd == -1) {
                    // Unclosed variable expression, treat the entire ${... as static text
                    sections.add(new StringSection(new Position(varStart - 2, end), new String(chars, varStart - 2, end - varStart + 2)));
                    sectionStart = end; // Update the start position to the end
                    break;
                }

                // Build the ExpressionSection and add it to the list
                ExpressionPosition expPos = new ExpressionPosition(varStart - 2, varEnd,
                        new Position(varStart, defStart == -1 ? varEnd - 1 : defStart - 1),
                        defStart == -1 ? null : parseDefaultValue(chars, defStart, varEnd));
                sections.add(build(expPos, chars, varStart - 2, varEnd));
                varCount++;
                // Update the start position for the next section
                sectionStart = varEnd;
                // Skip }
                i = varEnd;
            } else if (chars[i] == '$') {
                // Found an illegal variable expression (e.g., $a or $ without {), treat as static text
                if (sectionStart < i) {
                    // Add the preceding static text as a StringSection
                    sections.add(new StringSection(new Position(sectionStart, i), new String(chars, sectionStart, i - sectionStart)));
                }
                // Add the $ as a static character
                sections.add(new StringSection(new Position(i, i + 1), "$"));
                i++;
                sectionStart = i; // Update the start position for the next section
            } else if (defMode && chars[i] == '}') {
                end = i;
                break;
            } else {
                i++;
            }
        }

        // Add any remaining static text as a StringSection
        if (sectionStart < end) {
            sections.add(new StringSection(new Position(sectionStart, end), new String(chars, sectionStart, end - sectionStart)));
        }

        return varCount;
    }

    /**
     * Parses a variable within the given character array, handling nested variables and optional default values.
     *
     * @param chars      the character array to parse
     * @param startIndex the starting index for parsing
     * @param end        the end index (exclusive) for parsing
     * @return an array containing two integers:
     * - The first integer is the index after the end of the parsed variable, or {@code -1} if unclosed.
     * - The second integer is the start index of the default value (e.g., after ':'), or {@code -1} if no default value exists.
     */
    private int[] parseVariable(char[] chars, int startIndex, int end) {
        int i = startIndex;
        int[] pos = new int[]{-1, -1};
        int[] nestPos;

        while (i < end) {
            if (chars[i] == '$' && i + 1 < end && chars[i + 1] == '{') {
                // Skip ${
                i += 2;
                // Parse the nested variable
                nestPos = parseVariable(chars, i, end);
                i = nestPos[0];
                if (i == -1) {
                    // Unclosed nested variable, treat as static text
                    return nestPos;
                }
            } else if (chars[i] == '}') {
                // End of the current variable
                pos[0] = i + 1;
                return pos;
            } else if (chars[i] == ':') {
                // Start of the default value
                if (pos[1] == -1) {
                    pos[1] = i + 1;
                }
                i++;
            } else {
                i++;
            }
        }

        return pos;
    }

    /**
     * Parses a default value string to handle nested variables and mixed static-dynamic values.
     * If the default value contains nested variables (e.g., prefix-${b:${c:e}-suffix}),
     * it recursively parses them and returns a Position object representing the global position in the source.
     *
     * @param chars the character array of the expression
     * @param start the start index of the default value
     * @param end   the end index of the default value
     * @return a Position object representing the parsed default value in the global context
     */
    private Position parseDefaultValue(char[] chars, int start, int end) {
        if (start >= end) {
            return null;
        }
        List<Section> sections = new ArrayList<>();
        parse(chars, start, end, sections, true); // Recursively parse nested variables

        int size = sections.size();
        Section first = size == 0 ? null : sections.get(0);

        if (size == 0) {
            return null;
        } else if (size == 1 && first instanceof StringSection) {
            // If the default value is a simple string, return a Position for it in the global context
            return first.getPosition();
        } else {
            // If the default value contains nested variables or mixed content,
            // return a DefaultValuePosition for the entire expression in the global context
            List<Position> positions = new ArrayList<>();
            for (Section section : sections) {
                positions.add(section.getPosition());
            }
            int defStart = positions.get(0).getStart();
            int defEnd = positions.get(positions.size() - 1).getEnd();
            return new DefaultValuePosition(defStart, defEnd, positions);
        }
    }

    /**
     * Builds a {@link Section} object based on the provided {@link Position} and character array.
     * This method handles different types of positions, including {@link ExpressionPosition}
     * and other types, to construct the appropriate section.
     *
     * @param position the {@link Position} object representing the location and type of the section
     * @param chars    the character array from which to extract values
     * @param start    the start index of the section in the character array
     * @param end      the end index of the section in the character array
     * @return a {@link Section} object representing the parsed section, or {@code null} if the
     * position is {@code null}
     */
    private Section build(Position position, char[] chars, int start, int end) {
        String value = position == null ? null : new String(chars, start, end - start);
        if (position == null) {
            return null;
        } else if (position instanceof ExpressionPosition) {
            ExpressionPosition vp = (ExpressionPosition) position;
            Position varPos = vp.variable;
            String variableValue = new String(chars, varPos.start, varPos.end - varPos.start);
            VariableSection varSection = new VariableSection(varPos, variableValue);
            Position defPos = vp.defaultValue;
            Section defSection = defPos == null ? null : build(defPos, chars, defPos.start, defPos.end);
            return new ExpressionSection(position, value, varSection, defSection);
        } else if (position instanceof DefaultValuePosition) {
            DefaultValuePosition dp = (DefaultValuePosition) position;
            List<Position> positions = dp.getPositions();
            List<Section> sections = null;
            if (positions != null) {
                sections = new ArrayList<>(positions.size());
                for (Position pos : positions) {
                    sections.add(build(pos, chars, pos.start, pos.end));
                }
            }
            return new DefaultValueSection(position, value, sections);
        } else {
            return new StringSection(position, value);
        }
    }

    /**
     * Evaluates a list of sections in the given context and returns the combined result.
     *
     * @param context  the evaluation context containing variables and state (never null)
     * @param sections the sections to evaluate (null treated as empty list)
     * @return evaluation result which can be:
     * <ul>
     *   <li>null for empty input or all null results</li>
     *   <li>direct section result for single section</li>
     *   <li>concatenated string for multiple sections</li>
     * </ul>
     */
    private static Object doEvaluate(EvalContext context, List<Section> sections) {
        int size = sections == null ? 0 : sections.size();
        switch (size) {
            case 0:
                return null;
            case 1:
                return sections.get(0).evaluate(context);
            default:
                int count = 0;
                Object value;
                StringBuilder builder = new StringBuilder(64);
                for (Section section : sections) {
                    value = section.evaluate(context);
                    if (value != null) {
                        count++;
                        builder.append(value);
                    }
                }
                return count == 0 ? null : builder.toString();
        }
    }

    @Getter
    @AllArgsConstructor
    private static class Position {

        protected int start;

        protected int end;

        public boolean isEmpty() {
            return end <= start;
        }

        public int getLength() {
            return end - start;
        }
    }

    @Getter
    private static class ExpressionPosition extends Position {

        private final Position variable;

        private final Position defaultValue;

        ExpressionPosition(int start, int end, Position variable, Position defaultValue) {
            super(start, end);
            this.variable = variable;
            this.defaultValue = defaultValue;
        }
    }

    @Getter
    private static class DefaultValuePosition extends Position {

        private final List<Position> positions;

        DefaultValuePosition(int start, int end, List<Position> positions) {
            super(start, end);
            this.positions = positions;
        }
    }

    @Getter
    @AllArgsConstructor
    private static class EvalContext {

        private final Object context;

        private final boolean nullable;

    }

    /**
     * An interface for section of the template.
     */
    private interface Section {

        Position getPosition();

        Object evaluate(EvalContext context);

        default boolean isEmpty() {
            Position position = getPosition();
            return position == null || position.isEmpty();
        }

    }

    /**
     * An evaluator for variable sections of the template.
     */
    private static class ExpressionSection implements Section {

        @Getter
        private final Position position;

        private final String value;

        private final Section variable;

        private final Section defaultValue;

        ExpressionSection(Position position, String value, Section variable, Section defaultValue) {
            this.position = position;
            this.value = value;
            this.variable = variable;
            this.defaultValue = defaultValue;
        }

        @Override
        public Object evaluate(EvalContext context) {
            boolean nullable = context.isNullable();
            Object result = variable == null ? null : variable.evaluate(context);
            if (result == null || (result instanceof String && ((String) result).isEmpty())) {
                Object candidate;
                if (defaultValue == null) {
                    candidate = null;
                } else {
                    context = context.isNullable() ? context : new EvalContext(context.getContext(), true);
                    candidate = defaultValue.evaluate(context);
                }
                if (candidate == null) {
                    return nullable ? result : value;
                }
                return candidate;
            }
            return result;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * An evaluator for static key sections of the template.
     */
    private static class VariableSection implements Section {

        @Getter
        private final Position position;

        private final String expression;

        private final String variable;

        private String prefix;

        private String suffix;

        private final ValuePath getter;

        VariableSection(Position position, String variable) {
            this.position = position;
            this.expression = variable;
            if (variable != null) {
                if (!variable.isEmpty() && variable.charAt(0) == '\'') {
                    int pos = variable.indexOf('\'', 1);
                    if (pos > 0) {
                        prefix = variable.substring(1, pos);
                        variable = variable.substring(pos + 1);
                    }
                }
                if (!variable.isEmpty() && variable.charAt(variable.length() - 1) == '\'') {
                    int pos = variable.lastIndexOf('\'', variable.length() - 2);
                    if (pos >= 0) {
                        suffix = variable.substring(pos + 1, variable.length() - 1);
                        variable = variable.substring(0, pos);
                    }
                }
            }
            this.variable = variable;
            this.getter = variable == null || variable.isEmpty() ? null : new ValuePath(variable);
        }

        @Override
        public Object evaluate(EvalContext context) {
            Object obj = getter == null ? null : getter.get(context.getContext());
            if (obj == null) {
                return null;
            } else if (prefix == null && suffix == null) {
                return obj;
            } else {
                String result = obj.toString();
                if (prefix != null && suffix != null) {
                    return prefix + result + suffix;
                } else if (prefix != null) {
                    return prefix + result;
                } else {
                    return result + suffix;
                }
            }
        }

        @Override
        public String toString() {
            return variable;
        }
    }

    /**
     * An evaluator for static default value sections of the template.
     */
    private static class DefaultValueSection implements Section {

        @Getter
        private final Position position;

        private final String expression;

        private final List<Section> sections;

        DefaultValueSection(Position position, String expression, List<Section> sections) {
            this.position = position;
            this.expression = expression;
            this.sections = sections;
        }

        @Override
        public Object evaluate(EvalContext context) {
            return doEvaluate(context, sections);
        }

        @Override
        public String toString() {
            return expression;
        }
    }

    /**
     * An evaluator for static string sections of the template.
     */
    private static class StringSection implements Section {

        @Getter
        private final Position position;

        private final String value;

        StringSection(Position position, String value) {
            this.position = position;
            this.value = value;
        }

        @Override
        public Object evaluate(EvalContext context) {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

}
