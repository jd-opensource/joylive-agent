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
package com.jd.live.agent.core.util.type.generic;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

/**
 * Represents a wildcard type expression, such as {@code ? extends Number} or {@code ? super Integer}.
 * This class implements the {@link WildcardType} interface to provide concrete representations of wildcard type expressions
 * in the Java programming language.
 */
public class JWildcardType implements WildcardType {

    /**
     * The upper bound(s) of the wildcard type. If no upper bound is explicitly declared, the upper bound is {@code Object}.
     */
    protected final Type[] upperBounds;

    /**
     * The lower bound(s) of the wildcard type. A wildcard type may have a lower bound if it is of the form {@code ? super T}.
     */
    protected final Type[] lowerBounds;

    /**
     * Constructs a {@code JWildcardType} instance with specified upper and lower bounds.
     *
     * @param upperBounds The upper bounds of the wildcard type. This array may be empty if no upper bounds are declared.
     * @param lowerBounds The lower bounds of the wildcard type. This array must be empty if upper bounds are declared.
     */
    public JWildcardType(Type[] upperBounds, Type[] lowerBounds) {
        this.upperBounds = upperBounds;
        this.lowerBounds = lowerBounds;
    }

    @Override
    public Type[] getUpperBounds() {
        return upperBounds;
    }

    @Override
    public Type[] getLowerBounds() {
        return lowerBounds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JWildcardType that = (JWildcardType) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(upperBounds, that.upperBounds)) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(lowerBounds, that.lowerBounds);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(upperBounds);
        result = 31 * result + Arrays.hashCode(lowerBounds);
        return result;
    }

    @Override
    public String toString() {
        Type[] types;
        StringBuilder builder = new StringBuilder();
        if (lowerBounds.length > 0) {
            types = lowerBounds;
            builder.append("? super ");
        } else {
            if (upperBounds.length <= 0 || upperBounds[0].equals(Object.class)) {
                return "?";
            }
            types = upperBounds;
            builder.append("? extends ");
        }
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                builder.append(" & ");
            }
            builder.append(types[i].getTypeName());
        }
        return builder.toString();
    }
}
