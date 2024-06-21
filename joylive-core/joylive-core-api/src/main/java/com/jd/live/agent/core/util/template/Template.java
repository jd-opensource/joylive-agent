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
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A template engine that processes a template string with variable placeholders.
 * Variables are denoted by the pattern ${anything}.
 */
public class Template implements Evaluator {

    /**
     * Regular expression pattern for matching variable placeholders.
     */
    private static final Pattern PATTERN = Pattern.compile("\\$\\{(.*?)}");

    /**
     * The original template source string.
     */
    @Getter
    private final String source;

    /**
     * A list of evaluators that represent the parsed template sections.
     */
    private final List<Evaluator> sections = new ArrayList<>();

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
        this.variables = parse(source, sections);
    }

    /**
     * Parses the template expression to count variables and create sections for evaluation.
     *
     * @param expression the template expression to parse
     * @param sections the list to which the parsed sections will be added
     * @return the count of variables in the expression
     */
    private int parse(String expression, List<Evaluator> sections) {
        int count = 0;
        if (expression != null && !expression.isEmpty()) {
            Matcher matcher = PATTERN.matcher(expression);
            int start = 0;
            while (matcher.find()) {
                if (matcher.start() > start) {
                    sections.add(new StringSection(expression.substring(start, matcher.start())));
                }
                count++;
                sections.add(new VariableSection(expression.substring(matcher.start(), matcher.end()), matcher.group(1)));
                start = matcher.end();
            }
            if (start < expression.length()) {
                sections.add(new StringSection(expression.substring(start)));
            }
        }
        return count;
    }

    /**
     * Evaluates the template with the given context, replacing variables with their corresponding values.
     *
     * @param context a map of variable names to their values
     * @return the evaluated template string
     */
    public String evaluate(Map<String, Object> context) {
        StringBuilder builder = new StringBuilder(capacity <= 0 ? 16 : capacity);
        for (Evaluator section : sections) {
            builder.append(section.evaluate(context));
        }
        return builder.toString();
    }

    /**
     * An evaluator for variable sections of the template.
     */
    private static class VariableSection implements Evaluator {

        private final ValuePath getter;

        private final String expression;

        private String prefix;

        private String suffix;

        VariableSection(String expression, String variable) {
            this.expression = expression;
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
            this.getter = new ValuePath(variable);
        }

        @Override
        public String evaluate(Map<String, Object> context) {
            Object object = getter.get(context);
            String result = object == null ? null : object.toString();
            if (result == null || result.isEmpty()) {
                return "";
            } else if (prefix == null && suffix == null) {
                return result;
            } else if (prefix != null && suffix != null) {
                return prefix + result + suffix;
            } else if (prefix != null) {
                return prefix + result;
            } else {
                return result + suffix;
            }
        }
    }

    /**
     * An evaluator for static string sections of the template.
     */
    private static class StringSection implements Evaluator {

        private final String value;

        StringSection(String value) {
            this.value = value;
        }

        @Override
        public String evaluate(Map<String, Object> context) {
            return value;
        }
    }

}
