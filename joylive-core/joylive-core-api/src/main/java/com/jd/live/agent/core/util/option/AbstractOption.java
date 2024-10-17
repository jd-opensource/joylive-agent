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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Abstract Option
 */
public abstract class AbstractOption implements Option {

    @Override
    public String getString(final String key) {
        Object target = getObject(key);
        return target == null ? null : target.toString();
    }

    @Override
    public String getString(final String key, final String def) {
        String value = getString(key);
        if (value == null || value.isEmpty()) {
            return def;
        }
        return value;
    }

    @Override
    public Date getDate(final String key, final Date def) {
        return Converts.getDate(getString(key), def);
    }

    @Override
    public Date getDate(final String key, final SimpleDateFormat format) {
        return Converts.getDate(getString(key), format, null);
    }

    @Override
    public Date getDate(final String key, final SimpleDateFormat format, final Date def) {
        return Converts.getDate(getString(key), format, def);
    }

    @Override
    public Float getFloat(final String key) {
        return Converts.getFloat(getString(key), null);
    }

    @Override
    public Float getFloat(final String key, final Float def) {
        return Converts.getFloat(getString(key), def);
    }

    @Override
    public Double getDouble(final String key) {
        return Converts.getDouble(getString(key), null);
    }

    @Override
    public Double getDouble(final String key, final Double def) {
        return Converts.getDouble(getString(key), def);
    }

    @Override
    public Long getLong(final String key) {
        return Converts.getLong(getString(key), null);
    }

    @Override
    public Long getLong(final String key, final Long def) {
        return Converts.getLong(getString(key), def);
    }

    @Override
    public Integer getInteger(final String key) {
        return Converts.getInteger(getString(key), null);
    }

    @Override
    public Integer getInteger(final String key, final Integer def) {
        return Converts.getInteger(getString(key), def);
    }

    @Override
    public Short getShort(String key) {
        return Converts.getShort(getString(key), null);
    }

    @Override
    public Short getShort(String key, Short def) {
        return Converts.getShort(getString(key), def);
    }

    @Override
    public Byte getByte(final String key) {
        return Converts.getByte(getString(key), null);
    }

    @Override
    public Byte getByte(final String key, final Byte def) {
        return Converts.getByte(getString(key), def);
    }

    @Override
    public Boolean getBoolean(final String key) {
        return Converts.getBoolean(getString(key), null);
    }

    @Override
    public Boolean getBoolean(final String key, final Boolean def) {
        return Converts.getBoolean(getString(key), def);
    }

    @Override
    public Long getNatural(final String key, final Long def) {
        return Converts.getNatural(getString(key), def);
    }

    @Override
    public Integer getNatural(final String key, final Integer def) {
        return Converts.getNatural(getString(key), def);
    }

    @Override
    public Short getNatural(final String key, final Short def) {
        return Converts.getNatural(getString(key), def);
    }

    @Override
    public Byte getNatural(final String key, final Byte def) {
        return Converts.getNatural(getString(key), def);
    }

    @Override
    public Long getPositive(final String key, final Long def) {
        return Converts.getPositive(getString(key), def);
    }

    @Override
    public Integer getPositive(final String key, final Integer def) {
        return Converts.getPositive(getString(key), def);
    }

    @Override
    public Short getPositive(final String key, final Short def) {
        return Converts.getPositive(getString(key), def);
    }

    @Override
    public Byte getPositive(final String key, final Byte def) {
        return Converts.getPositive(getString(key), def);
    }

    @Override
    public Double getPositive(final String key, Double def) {
        return Converts.getPositive(getString(key), def);
    }
}
