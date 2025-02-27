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
package com.jd.live.agent.implement.parser.properties;

import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.parser.ConfigParser;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * PropertiesParser
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Extension(value = ConfigParser.PROPERTIES)
public class PropertiesParser implements ConfigParser {

    @Override
    public Map<String, Object> parse(Reader reader) {
        if (reader == null)
            return new HashMap<>();
        try {
            Properties properties = new Properties();
            properties.load(reader);
            Map<String, Object> result = new HashMap<>(properties.size());
            properties.forEach((key, value) -> result.put(key.toString(), value));
            return result;
        } catch (IOException e) {
            throw new ParseException("an error occurred while parsing properties.", e);
        }
    }

    @Override
    public boolean isFlatted() {
        return true;
    }
}
