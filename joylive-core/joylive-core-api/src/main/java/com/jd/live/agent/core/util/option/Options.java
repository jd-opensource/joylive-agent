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

public class Options implements Option {

    private Option[] options;

    public Options(Option... options) {
        this.options = options;
    }

    @Override
    public String getString(String key) {
        Object result = getObject(key);
        return result == null ? null : result.toString();
    }

    @Override
    public String getString(String key, String def) {
        String result = getString(key);
        return result == null || result.isEmpty() ? def : result;
    }

    @Override
    public Date getDate(final String key, final Date def) {
        return Converts.getDate(getObject(key), def);
    }

    @Override
    public Date getDate(final String key, final SimpleDateFormat format) {
        return Converts.getDate(getObject(key), format, null);
    }

    @Override
    public Date getDate(final String key, final SimpleDateFormat format, final Date def) {
        return Converts.getDate(getObject(key), format, def);
    }

    @Override
    public Float getFloat(final String key) {
        return Converts.getFloat(getObject(key), null);
    }

    @Override
    public Float getFloat(final String key, final Float def) {
        return Converts.getFloat(getObject(key), def);
    }

    @Override
    public Double getDouble(final String key) {
        return Converts.getDouble(getObject(key), null);
    }

    @Override
    public Double getDouble(final String key, final Double def) {
        return Converts.getDouble(getObject(key), def);
    }

    @Override
    public Long getLong(final String key) {
        return Converts.getLong(getObject(key), null);
    }

    @Override
    public Long getLong(final String key, final Long def) {
        return Converts.getLong(getObject(key), def);
    }

    @Override
    public Integer getInteger(final String key) {
        return Converts.getInteger(getObject(key), null);
    }

    @Override
    public Integer getInteger(final String key, final Integer def) {
        return Converts.getInteger(getObject(key), def);
    }

    @Override
    public Short getShort(String key) {
        return Converts.getShort(getObject(key), null);
    }

    @Override
    public Short getShort(String key, Short def) {
        return Converts.getShort(getObject(key), def);
    }

    @Override
    public Byte getByte(final String key) {
        return Converts.getByte(getObject(key), null);
    }

    @Override
    public Byte getByte(final String key, final Byte def) {
        return Converts.getByte(getObject(key), def);
    }

    @Override
    public Boolean getBoolean(final String key) {
        return Converts.getBoolean(getObject(key), null);
    }

    @Override
    public Boolean getBoolean(final String key, final Boolean def) {
        return Converts.getBoolean(getObject(key), def);
    }

    @Override
    public Long getNatural(final String key, final Long def) {
        return Converts.getNatural(getObject(key), def);
    }

    @Override
    public Integer getNatural(final String key, final Integer def) {
        return Converts.getNatural(getObject(key), def);
    }

    @Override
    public Short getNatural(final String key, final Short def) {
        return Converts.getNatural(getObject(key), def);
    }

    @Override
    public Byte getNatural(final String key, final Byte def) {
        return Converts.getNatural(getObject(key), def);
    }

    @Override
    public Long getPositive(final String key, final Long def) {
        return Converts.getPositive(getObject(key), def);
    }

    @Override
    public Integer getPositive(final String key, final Integer def) {
        return Converts.getPositive(getObject(key), def);
    }

    @Override
    public Short getPositive(final String key, final Short def) {
        return Converts.getPositive(getObject(key), def);
    }

    @Override
    public Byte getPositive(final String key, final Byte def) {
        return Converts.getPositive(getObject(key), def);
    }

    @Override
    public Double getPositive(final String key, Double def) {
        return Converts.getPositive(getObject(key), def);
    }

    @Override
    public <T> T getObject(String key) {
        Object result = null;
        Object value;
        for (Option option : options) {
            value = option.getObject(key);
            if (value != null) {
                result = value;
                if (result instanceof CharSequence) {
                    if (((CharSequence) result).length() > 0) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return (T) result;
    }
}
