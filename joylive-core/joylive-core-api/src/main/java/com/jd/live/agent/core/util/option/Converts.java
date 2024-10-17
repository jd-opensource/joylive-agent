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
package com.jd.live.agent.core.util.option;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

/**
 * Provides static methods for type conversion of various objects to specific data types.
 */
public abstract class Converts {

    /**
     * Converts an object to a comma-separated string if it is a CharSequence, Collection, or an array.
     *
     * @param value The object to be converted.
     * @return A comma-separated string representation of the object.
     */
    public static String getString(final Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof CharSequence) {
            return value.toString();
        } else if (value instanceof Collection) {
            StringBuilder builder = new StringBuilder();
            Collection<?> collection = (Collection<?>) value;
            int count = 0;
            for (Object item : collection) {
                if (count++ > 0) {
                    builder.append(',');
                }
                if (item != null) {
                    builder.append(item);
                }
            }
            return builder.toString();
        } else if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            StringBuilder builder = new StringBuilder();
            Object item;
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                item = Array.get(value, i);
                if (item != null) {
                    builder.append(item);
                }
            }
            return builder.toString();
        }
        return value.toString();

    }

    /**
     * Converts an object to a string or returns a default value if the conversion result is null or empty.
     *
     * @param value The object to be converted.
     * @param def   The default string to return if the result is null or empty.
     * @return The converted string or the default value.
     */
    public static String getString(final Object value, final String def) {
        String text = getString(value);
        if (text == null || text.isEmpty()) {
            return def;
        }
        return text;
    }

    /**
     * Converts an object to a Date using its long value representation or returns a default value.
     *
     * @param value The object to be converted.
     * @param def   The default Date to return if the conversion is not possible.
     * @return The converted Date or the default value.
     */
    public static Date getDate(final Object value, final Date def) {
        if (value == null) {
            return def;
        } else if (value instanceof Date) {
            return (Date) value;
        } else if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        } else if (value instanceof CharSequence) {
            String text = value.toString();
            try {
                return new Date((Long.parseLong(text)));
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * Converts an object to a Date using a specified SimpleDateFormat or returns a default value.
     *
     * @param value  The object to be converted.
     * @param format The SimpleDateFormat to use for the conversion.
     * @return The converted Date or the default value.
     */
    public static Date getDate(final Object value, final SimpleDateFormat format) {
        return getDate(value, format, null);
    }

    /**
     * Converts an object to a Date using a specified SimpleDateFormat or returns a default value.
     *
     * @param value  The object to be converted.
     * @param format The SimpleDateFormat to use for the conversion.
     * @param def    The default Date to return if the conversion is not possible.
     * @return The converted Date or the default value.
     */
    public static Date getDate(final Object value, final SimpleDateFormat format, final Date def) {
        if (value == null) {
            return def;
        } else if (value instanceof Date) {
            return (Date) value;
        } else if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        } else if (format == null) {
            return def;
        } else if (value instanceof CharSequence) {
            String text = value.toString();
            try {
                Date result = format.parse(text);
                return result == null ? def : result;
            } catch (ParseException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * Retrieves the single-precision floating-point value of the given object.
     *
     * @param value The object from which to retrieve the float value.
     * @return The float value of the object, or {@code null} if the object is {@code null}.
     */
    public static Float getFloat(final Object value) {
        return getFloat(value, null);
    }

    /**
     * Retrieves the single-precision floating-point value of the given object, with a default value.
     *
     * @param value The object from which to retrieve the float value.
     * @param def   The default value to return if the object does not represent a float.
     * @return The float value of the object, or the default value if the object cannot be converted to a float.
     */
    public static Float getFloat(final Object value, final Float def) {
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof CharSequence || value instanceof Character) {
            String text = value.toString();
            if (text.isEmpty()) {
                return def;
            }
            try {
                return Float.parseFloat(text);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * Retrieves the double-precision floating-point value of the given object.
     *
     * @param value The object from which to retrieve the double value.
     * @return The double value of the object, or {@code null} if the object is {@code null}.
     */
    public static Double getDouble(final Object value) {
        return getDouble(value, null);
    }

    /**
     * Retrieves the double-precision floating-point value of the given object, with a default value.
     *
     * @param value The object from which to retrieve the double value.
     * @param def   The default value to return if the object does not represent a double.
     * @return The double value of the object, or the default value if the object cannot be converted to a double.
     */
    public static Double getDouble(final Object value, final Double def) {
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof CharSequence || value instanceof Character) {
            String text = value.toString();
            if (text.isEmpty()) {
                return def;
            }
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * Retrieves the long integer value of the given object.
     *
     * @param value The object from which to retrieve the long value.
     * @return The long value of the object, or {@code null} if the object is {@code null}.
     */
    public static Long getLong(final Object value) {
        return getLong(value, null);
    }

    /**
     * Retrieves the long integer value of the given object, with a default value.
     *
     * @param value The object from which to retrieve the long value.
     * @param def   The default value to return if the object does not represent a long integer.
     * @return The long value of the object, or the default value if the object cannot be converted to a long integer.
     */
    public static Long getLong(final Object value, final Long def) {
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof CharSequence || value instanceof Character) {
            String text = value.toString();
            if (text.isEmpty()) {
                return def;
            }
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * Retrieves the integer value of the given object.
     *
     * @param value The object from which to retrieve the integer value.
     * @return The integer value of the object, or {@code null} if the object is {@code null}.
     */
    public static Integer getInteger(final Object value) {
        return getInteger(value, null);
    }

    /**
     * Retrieves the integer value of the given object, with a default value.
     *
     * @param value The object from which to retrieve the integer value.
     * @param def   The default value to return if the object does not represent an integer.
     * @return The integer value of the object, or the default value if the object cannot be converted to an integer.
     */
    public static Integer getInteger(final Object value, final Integer def) {
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof CharSequence || value instanceof Character) {
            String text = value.toString();
            if (text.isEmpty()) {
                return def;
            }
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * Retrieves the short integer value of the given object.
     *
     * @param value The object from which to retrieve the short value.
     * @return The short value of the object, or {@code null} if the object is {@code null}.
     */
    public static Short getShort(final Object value) {
        return getShort(value, null);
    }

    /**
     * Retrieves the short integer value of the given object, with a default value.
     *
     * @param value The object from which to retrieve the short value.
     * @param def   The default value to return if the object does not represent a short integer.
     * @return The short integer value of the object, or the default value if the object cannot be converted to a short integer.
     */
    public static Short getShort(final Object value, final Short def) {
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).shortValue();
        } else if (value instanceof CharSequence || value instanceof Character) {
            String text = value.toString();
            if (text.isEmpty()) {
                return def;
            }
            try {
                return Short.parseShort(text);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * Retrieves the byte value of the given object.
     *
     * @param value The object from which to retrieve the byte value.
     * @return The byte value of the object, or {@code null} if the object is {@code null}.
     */
    public static Byte getByte(final Object value) {
        return getByte(value, null);
    }

    /**
     * Retrieves the byte value of the given object, with a default value.
     *
     * @param value The object from which to retrieve the byte value.
     * @param def   The default value to return if the object does not represent a byte.
     * @return The byte value of the object, or the default value if the object cannot be converted to a byte.
     */
    public static Byte getByte(final Object value, final Byte def) {
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).byteValue();
        } else if (value instanceof CharSequence || value instanceof Character) {
            String text = value.toString();
            if (text.isEmpty()) {
                return def;
            }
            try {
                return Byte.parseByte(text);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * Retrieves the boolean value of the given object.
     *
     * @param value The object from which to retrieve the boolean value.
     * @return The boolean value of the object, or {@code null} if the object is {@code null}.
     */
    public static Boolean getBoolean(final Object value) {
        return getBoolean(value, null);
    }

    /**
     * Retrieves the boolean value of the given object, with a default value.
     *
     * @param value The object from which to retrieve the boolean value.
     * @param def   The default value to return if the object does not represent a boolean.
     * @return The boolean value of the object, or the default value if the object cannot be converted to a boolean.
     */
    public static Boolean getBoolean(final Object value, final Boolean def) {
        if (value == null) {
            return def;
        } else if (value instanceof Number) {
            return ((Number) value).longValue() != 0;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Character) {
            return ((Character) value) != '0';
        } else if (value instanceof CharSequence) {
            String text = value.toString();
            if ("true".equalsIgnoreCase(text)) {
                return true;
            } else if ("false".equalsIgnoreCase(text)) {
                return false;
            }
            try {
                return Long.parseLong(text) != 0;
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    /**
     * Retrieves the long natural number value of the given object, with a default value.
     *
     * @param value The object from which to retrieve the long natural number value.
     * @param def   The default value to return if the object does not represent a long natural number.
     * @return The long natural number value of the object, or the default value if the object cannot be converted to a long natural number.
     */
    public static Long getNatural(final Object value, final Long def) {
        Long result = getLong(value, null);
        return result == null || result < 0 ? def : result;
    }

    /**
     * Retrieves the integer natural number value of the given object, with a default value.
     *
     * @param value The object from which to retrieve the integer natural number value.
     * @param def   The default value to return if the object does not represent an integer natural number.
     * @return The integer natural number value of the object, or the default value if the object cannot be converted to an integer natural number.
     */
    public static Integer getNatural(final Object value, final Integer def) {
        Integer result = getInteger(value, null);
        return result == null || result < 0 ? def : result;
    }

    /**
     * Retrieves the short natural number value of the given object, with a default value.
     *
     * @param value The object from which to retrieve the short natural number value.
     * @param def   The default value to return if the object does not represent a short natural number.
     * @return The short natural number value of the object, or the default value if the object cannot be converted to a short natural number.
     */
    public static Short getNatural(final Object value, final Short def) {
        Short result = getShort(value, null);
        return result == null || result < 0 ? def : result;
    }

    /**
     * Retrieves the byte value representing a natural number (non-negative) from the given object, with a default value.
     *
     * @param value The object from which to retrieve the byte value.
     * @param def   The default value to return if the object does not represent a natural number.
     * @return The natural number byte value of the object, or the default value if the object is not a natural number.
     */
    public static Byte getNatural(final Object value, final Byte def) {
        Byte result = getByte(value, null);
        return result == null || result < 0 ? def : result;
    }

    /**
     * Retrieves the long integer value representing a positive number from the given object, with a default value.
     *
     * @param value The object from which to retrieve the long value.
     * @param def   The default value to return if the object does not represent a positive number.
     * @return The positive long integer value of the object, or the default value if the object is not a positive number.
     */
    public static Long getPositive(final Object value, final Long def) {
        Long result = getLong(value, null);
        return result == null || result <= 0 ? def : result;
    }

    /**
     * Retrieves the integer value representing a positive number from the given object, with a default value.
     *
     * @param value The object from which to retrieve the integer value.
     * @param def   The default value to return if the object does not represent a positive number.
     * @return The positive integer value of the object, or the default value if the object is not a positive number.
     */
    public static Integer getPositive(final Object value, final Integer def) {
        Integer result = getInteger(value, null);
        return result == null || result <= 0 ? def : result;
    }

    /**
     * Retrieves the short integer value representing a positive number from the given object, with a default value.
     *
     * @param value The object from which to retrieve the short value.
     * @param def   The default value to return if the object does not represent a positive number.
     * @return The positive short integer value of the object, or the default value if the object is not a positive number.
     */
    public static Short getPositive(final Object value, final Short def) {
        Short result = getShort(value, null);
        return result == null || result <= 0 ? def : result;
    }

    /**
     * Retrieves the byte value representing a positive number from the given object, with a default value.
     *
     * @param value The object from which to retrieve the byte value.
     * @param def   The default value to return if the object does not represent a positive number.
     * @return The positive byte value of the object, or the default value if the object is not a positive number.
     */
    public static Byte getPositive(final Object value, final Byte def) {
        Byte result = getByte(value, null);
        return result == null || result <= 0 ? def : result;
    }

    /**
     * Retrieves the byte value representing a positive number from the given object, with a default value.
     *
     * @param value The object from which to retrieve the byte value.
     * @param def   The default value to return if the object does not represent a positive number.
     * @return The positive byte value of the object, or the default value if the object is not a positive number.
     */
    public static Double getPositive(final Object value, final Double def) {
        Double result = getDouble(value, null);
        return result == null || result <= 0 ? def : result;
    }

}
