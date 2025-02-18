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
package com.jd.live.agent.bootstrap.util.type;

/**
 * An interface that provides access to a specific field of an object using the Unsafe API.
 *
 * @see ObjectAccessor
 */
public interface UnsafeFieldAccessor extends ObjectAccessor {

    /**
     * Returns the value of the field as a boolean for the specified object.
     *
     * @param object the object to access
     * @return the value of the field as a boolean
     */
    boolean getBoolean(Object object);

    /**
     * Sets the value of the field to the specified boolean value for the specified object.
     *
     * @param target the object to access
     * @param value  the new value of the field
     */
    void setBoolean(Object target, boolean value);

    /**
     * Returns the value of the field as a char for the specified object.
     *
     * @param object the object to access
     * @return the value of the field as a char
     */
    char getChar(Object object);

    /**
     * Sets the value of the field to the specified char value for the specified object.
     *
     * @param target the object to access
     * @param value  the new value of the field
     */
    void setChar(Object target, char value);

    /**
     * Returns the value of the field as a byte for the specified object.
     *
     * @param object the object to access
     * @return the value of the field as a byte
     */
    byte getByte(Object object);

    /**
     * Sets the value of the field to the specified byte value for the specified object.
     *
     * @param target the object to access
     * @param value  the new value of the field
     */
    void setByte(Object target, byte value);

    /**
     * Returns the value of the field as a short for the specified object.
     *
     * @param object the object to access
     * @return the value of the field as a short
     */
    short getShort(Object object);

    /**
     * Sets the value of the field to the specified short value for the specified object.
     *
     * @param target the object to access
     * @param value  the new value of the field
     */
    void setShort(Object target, short value);

    /**
     * Returns the value of the field as an int for the specified object.
     *
     * @param object the object to access
     * @return the value of the field as an int
     */
    int getInt(Object object);

    /**
     * Sets the value of the field to the specified int value for the specified object.
     *
     * @param target the object to access
     * @param value  the new value of the field
     */
    void setInt(Object target, int value);

    /**
     * Returns the value of the field as a long for the specified object.
     *
     * @param object the object to access
     * @return the value of the field as a long
     */
    long getLong(Object object);

    /**
     * Sets the value of the field to the specified long value for the specified object.
     *
     * @param target the object to access
     * @param value  the new value of the field
     */
    void setLong(Object target, long value);

    /**
     * Returns the value of the field as a float for the specified object.
     *
     * @param object the object to access
     * @return the value of the field as a float
     */
    float getFloat(Object object);

    /**
     * Sets the value of the field to the specified float value for the specified object.
     *
     * @param target the object to access
     * @param value  the new value of the field
     */
    void setFloat(Object target, float value);

    /**
     * Returns the value of the field as a double for the specified object.
     *
     * @param object the object to access
     * @return the value of the field as a double
     */
    double getDouble(Object object);

    /**
     * Sets the value of the field to the specified double value for the specified object.
     *
     * @param target the object to access
     * @param value  the new value of the field
     */
    void setDouble(Object target, double value);
}

