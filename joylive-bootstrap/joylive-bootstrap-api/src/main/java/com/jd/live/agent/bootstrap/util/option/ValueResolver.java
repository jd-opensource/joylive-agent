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
package com.jd.live.agent.bootstrap.util.option;

/**
 * A utility class to resolve values, potentially expressed as variables with a specific syntax,
 * using a provided ValueSupplier.
 */
public class ValueResolver {

    // Prefix identifying the start of a variable expression.
    public static final String EL_PREFIX = "${";

    // Suffix identifying the end of a variable expression.
    public static final String EL_SUFFIX = "}";

    // String representation of a null value.
    public static final String NULL = "null";

    // The ValueSupplier used to resolve variables.
    private final ValueSupplier option;

    /**
     * Constructs a new ValueResolver with the specified ValueSupplier.
     *
     * @param option The ValueSupplier to be used for resolving variables.
     */
    public ValueResolver(ValueSupplier option) {
        this.option = option;
    }

    /**
     * Parses the given value, resolving any variable expressions it contains.
     *
     * @param value The String value potentially containing variable expressions.
     * @return The resolved Object after processing the value.
     */
    public Object parse(String value) {
        Object result = value;
        if (isVariable(result)) {
            // Handle variable, e.g., ${ENV_1:123}
            String variable = value.substring(2, value.length() - 1);
            if (!variable.isEmpty()) {
                result = doParse(variable);
                if (isVariable(result)) {
                    // Handle nested variable expressions
                    result = parse((String) result);
                }
            }
        }
        return result;
    }

    /**
     * Determines if the given value is a variable expression that needs to be resolved.
     *
     * @param value The value to check.
     * @return True if the value is a variable expression, otherwise false.
     */
    private boolean isVariable(Object value) {
        if (value instanceof String) {
            String text = (String) value;
            return text.startsWith(EL_PREFIX) && text.endsWith(EL_SUFFIX);
        }
        return false;
    }

    /**
     * Parses a variable expression to resolve its value using the ValueSupplier or a default value.
     *
     * @param variable The variable expression to resolve.
     * @return The resolved value of the variable or the default value if the variable is not found.
     */
    private Object doParse(String variable) {
        // Extract the key and optional default value from the variable expression.
        String key = variable;
        String defaultValue = null;
        int pos = variable.indexOf(':');
        if (pos > 0) {
            key = variable.substring(0, pos);
            defaultValue = variable.substring(pos + 1);
            if (NULL.equals(defaultValue)) {
                defaultValue = null;
            }
        }
        // Attempt to get the value from the environment using the ValueSupplier.
        Object result = option.getObject(key);
        if (result == null) {
            // Use the default value if the variable is not found.
            result = defaultValue;
        } else if (defaultValue != null && result instanceof String && ((String) result).isEmpty()) {
            // Use the default value if the resolved value is an empty string.
            result = defaultValue;
        }
        return result;
    }
}

