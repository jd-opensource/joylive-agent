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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.jbind.Sourcer;
import com.jd.live.agent.core.security.Cipher;
import com.jd.live.agent.core.security.CipherDetector;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.core.util.template.Template;

import java.util.Map;
import java.util.function.Function;

public class JConfigSourcer implements Sourcer {
    private static final Logger logger = LoggerFactory.getLogger(JConfigSourcer.class);
    private final String key;
    private final Option option;

    public JConfigSourcer(String key, Option option) {
        this.key = key;
        this.option = option;
    }

    @Override
    public Object getSource(Object context) {
        Object result = null;
        if (context instanceof JSource) {
            JSource source = (JSource) context;
            if (source.getCurrent() != null) {
                result = getSource(source.getCurrent(), key, value -> decrypt(source.getRoot(), value));
            } else if (!source.cascade()) {
                result = getSource(source.getRoot(), source.getPath(), value -> decrypt(source.getRoot(), value));
            }
        } else {
            result = getSource(context, key, value -> decrypt(context, value));
        }
        return result;
    }

    protected Object getSource(Object context, String key, Function<String, String> decrypter) {
        Object result = getObject(context, key);
        if (result == null) {
            return null;
        } else if (result instanceof String) {
            // handle expression language. such as ${ENV_1:123}
            String value = (String) result;
            boolean nullable = value.startsWith("${") && value.endsWith("}");
            result = Template.evaluate((String) result, option, nullable);
        }
        if (result instanceof String && decrypter != null) {
            // such as ENC(123)
            result = decrypter.apply((String) result);
        }


        return result;
    }

    protected Object getObject(Object context, String key) {
        if (context == null) {
            return null;
        } else if (context instanceof InjectSource) {
            return ((InjectSource) context).getOption().getObject(key);
        } else if (context instanceof Option) {
            return ((Option) context).getObject(key);
        } else if (context instanceof Map) {
            return ((Map<?, ?>) context).get(key);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getObject(Object context, String key, Class<T> type) {
        Object result = getObject(context, key);
        if (type.isInstance(result)) {
            return (T) result;
        }
        return null;
    }

    protected String decrypt(Object context, String value) {
        CipherDetector detector = getObject(context, CipherDetector.COMPONENT_CIPHER_DETECTOR, CipherDetector.class);
        Cipher cipher = getObject(context, Cipher.COMPONENT_CIPHER, Cipher.class);
        if (detector != null && cipher != null && detector.isEncrypted(value)) {
            try {
                return cipher.decrypt(detector.unwrap(value));
            } catch (Exception e) {
                logger.error("Error occurs while decoding config, caused by {}", e.getMessage(), e);
            }
        }
        return value;
    }

}
