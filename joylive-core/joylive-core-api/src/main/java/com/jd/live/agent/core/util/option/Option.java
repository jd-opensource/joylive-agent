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

import com.jd.live.agent.bootstrap.util.option.ValueSupplier;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An interface that defines methods for retrieving configuration parameters.
 */
public interface Option extends ValueSupplier {

    /**
     * Retrieves the string parameter value for the specified key.
     *
     * @param key The name of the parameter.
     * @return The value of the parameter as a string.
     */
    String getString(String key);

    /**
     * Retrieves the string parameter value for the specified key, with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter is not found.
     * @return The value of the parameter as a string, or the default value if not found.
     */
    String getString(String key, String def);

    /**
     * Retrieves the date parameter value represented as the number of milliseconds from the epoch.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter is not found.
     * @return The value of the parameter as a date, or the default value if not found.
     */
    Date getDate(String key, Date def);

    /**
     * Retrieves the date parameter value using the specified date format.
     *
     * @param key    The name of the parameter.
     * @param format The date format to parse the date string.
     * @return The value of the parameter as a date.
     */
    Date getDate(String key, SimpleDateFormat format);

    /**
     * Retrieves the date parameter value using the specified date format, with a default value.
     *
     * @param key    The name of the parameter.
     * @param format The date format to parse the date string.
     * @param def    The default value to return if the parameter is not found or parsing fails.
     * @return The value of the parameter as a date, or the default value if not found or parsing fails.
     */
    Date getDate(String key, SimpleDateFormat format, Date def);

    /**
     * Retrieves the float parameter value for the specified key.
     *
     * @param key The name of the parameter.
     * @return The value of the parameter as a float.
     */
    Float getFloat(String key);

    /**
     * Retrieves the float parameter value for the specified key, with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter is not found.
     * @return The value of the parameter as a float, or the default value if not found.
     */
    Float getFloat(String key, Float def);

    /**
     * Retrieves the double parameter value for the specified key.
     *
     * @param key The name of the parameter.
     * @return The value of the parameter as a double.
     */
    Double getDouble(String key);

    /**
     * Retrieves the double parameter value for the specified key, with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter is not found.
     * @return The value of the parameter as a double, or the default value if not found.
     */
    Double getDouble(String key, Double def);

    /**
     * Retrieves the long integer parameter value for the specified key.
     *
     * @param key The name of the parameter.
     * @return The value of the parameter as a long integer.
     */
    Long getLong(String key);

    /**
     * Retrieves the long integer parameter value for the specified key, with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter is not found.
     * @return The value of the parameter as a long integer, or the default value if not found.
     */
    Long getLong(String key, Long def);

    /**
     * Retrieves the integer parameter value for the specified key.
     *
     * @param key The name of the parameter.
     * @return The value of the parameter as an integer.
     */
    Integer getInteger(String key);

    /**
     * Retrieves the integer parameter value for the specified key, with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter is not found.
     * @return The value of the parameter as an integer, or the default value if not found.
     */
    Integer getInteger(String key, Integer def);

    /**
     * Retrieves the short integer parameter value for the specified key.
     *
     * @param key The name of the parameter.
     * @return The value of the parameter as a short integer.
     */
    Short getShort(String key);

    /**
     * Retrieves the short integer parameter value for the specified key, with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter is not found.
     * @return The value of the parameter as a short integer, or the default value if not found.
     */
    Short getShort(String key, Short def);

    /**
     * Retrieves the byte parameter value for the specified key.
     *
     * @param key The name of the parameter.
     * @return The value of the parameter as a byte.
     */
    Byte getByte(String key);

    /**
     * Retrieves the byte value for a specified parameter.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter value is not found.
     * @return The byte value of the parameter or the default value if the parameter is not found.
     */
    Byte getByte(String key, Byte def);

    /**
     * Retrieves the boolean value for a specified parameter.
     *
     * @param key The name of the parameter.
     * @return The boolean value of the parameter.
     */
    Boolean getBoolean(String key);

    /**
     * Retrieves the boolean value for a specified parameter with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter value is not found.
     * @return The boolean value of the parameter or the default value if the parameter is not found.
     */
    Boolean getBoolean(String key, Boolean def);

    /**
     * Retrieves the long natural number value for a specified parameter with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter value is not found.
     * @return The long natural number value of the parameter or the default value if the parameter is not found.
     */
    Long getNatural(String key, Long def);

    /**
     * Retrieves the integer natural number value for a specified parameter with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter value is not found.
     * @return The integer natural number value of the parameter or the default value if the parameter is not found.
     */
    Integer getNatural(String key, Integer def);

    /**
     * Retrieves the short natural number value for a specified parameter with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter value is not found.
     * @return The short natural number value of the parameter or the default value if the parameter is not found.
     */
    Short getNatural(String key, Short def);

    /**
     * Retrieves the byte natural number value for a specified parameter with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter value is not found.
     * @return The byte natural number value of the parameter or the default value if the parameter is not found.
     */
    Byte getNatural(String key, Byte def);

    /**
     * Retrieves the long positive integer value for a specified parameter with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter value is not found.
     * @return The long positive integer value of the parameter or the default value if the parameter is not found.
     */
    Long getPositive(String key, Long def);

    /**
     * Retrieves the integer positive value for a specified parameter with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter value is not found.
     * @return The integer positive value of the parameter or the default value if the parameter is not found.
     */
    Integer getPositive(String key, Integer def);

    /**
     * Retrieves the short positive integer value for a specified parameter with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter value is not found.
     * @return The short positive integer value of the parameter or the default value if the parameter is not found.
     */
    Short getPositive(String key, Short def);

    /**
     * Retrieves the byte positive integer value for a specified parameter with a default value.
     *
     * @param key The name of the parameter.
     * @param def The default value to return if the parameter value is not found.
     * @return The byte positive integer value of the parameter or the default value if the parameter is not found.
     */
    Byte getPositive(String key, Byte def);

}
