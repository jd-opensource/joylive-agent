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

import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.jbind.Sourcer;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.core.util.template.Template;

import java.util.Map;

public class JConfigSourcer implements Sourcer {
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
            String path = source.getPath();
            if (source.getCurrent() != null) {
                result = getSource(source.getCurrent(), key);
            } else if (!source.cascade()) {
                result = getSource(source.getRoot(), path);
            }
        } else {
            result = getSource(context, key);
        }
        return result;
    }

    protected Object getSource(Object context, String key) {
        Object result = null;
        if (context == null) {
            return null;
        } else if (context instanceof InjectSource) {
            result = ((InjectSource) context).getOption().getObject(key);
        } else if (context instanceof Option) {
            result = ((Option) context).getObject(key);
        } else if (context instanceof Map) {
            result = ((Map<?, ?>) context).get(key);
        }
        if (result instanceof String) {
            // handle expression language. such as ${ENV_1:123}
            result = Template.evaluate((String) result, option);
        }
        return result;
    }

}
