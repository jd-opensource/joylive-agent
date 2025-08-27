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
package com.jd.live.agent.core.inject.jbind.supplier;

import com.jd.live.agent.core.inject.InjectComponent;
import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.jbind.Sourcer;
import com.jd.live.agent.core.security.StringDecrypter;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.core.util.template.Template;

import java.util.Map;
import java.util.function.Function;

/**
 * Configuration sourcer that retrieves values by key with support for
 * template evaluation and decryption.
 */
public class JConfigSourcer implements Sourcer {
    private final String key;
    private final Option option;

    public JConfigSourcer(String key, Option option) {
        this.key = key;
        this.option = option;
    }

    @Override
    public Object getSource(Object context) {
        if (context instanceof JSource) {
            JSource source = (JSource) context;
            if (source.getCurrent() != null) {
                return getSource(source.getCurrent(), key, value -> decrypt(source, value));
            } else if (!source.cascade()) {
                return getSource(source.getRoot(), source.getPath(), value -> decrypt(source, value));
            }
        } else {
            return getSource(context, key, value -> decrypt(context, value));
        }
        return null;
    }

    /**
     * Retrieves and processes the source value with decryption.
     *
     * @param context   the source context
     * @param key       the configuration key
     * @param decrypter the decryption function
     * @return the processed value
     */
    protected Object getSource(Object context, String key, Function<String, String> decrypter) {
        Object result = getObject(context, key);
        if (result == null) {
            return null;
        } else if (result instanceof String) {
            // handle expression language. such as ${ENV_1:123}
            String value = (String) result;
            boolean nullable = value.startsWith("${") && value.endsWith("}");
            result = Template.evaluate((String) result, option, nullable);
            // such as ENC(123)
            return result instanceof String && decrypter != null ? decrypter.apply((String) result) : result;
        }
        return result;
    }

    /**
     * Retrieves the raw object value by key from the context.
     *
     * @param context the source context
     * @param key the configuration key
     * @return the raw value
     */
    protected Object getObject(Object context, String key) {
        context = context instanceof InjectSource ? ((InjectSource) context).getOption() : context;
        if (context instanceof Option) {
            return ((Option) context).getObject(key);
        } else if (context instanceof Map) {
            return ((Map<?, ?>) context).get(key);
        }
        return null;
    }

    /**
     * Decrypts the given value using the context's decrypter if available.
     *
     * @param context the source context
     * @param value the value to decrypt
     * @return the decrypted value, or original value if no decrypter found
     */
    protected String decrypt(Object context, String value) {
        StringDecrypter detector = null;
        if (context instanceof InjectComponent) {
            detector = ((InjectComponent) context).getComponent(StringDecrypter.COMPONENT_STRING_DECRYPTER, StringDecrypter.class);
        }
        return detector == null ? value : detector.tryDecrypt(value);
    }

}
