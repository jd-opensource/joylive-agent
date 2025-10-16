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

/**
 * Composite implementation that chains multiple options together.
 */
public class CompositeOption extends AbstractOption {

    private final Option[] options;

    public CompositeOption(Option... options) {
        this.options = options;
    }

    @Override
    public <T> T getObject(String key) {
        Object result = null;
        if (options != null) {
            for (Option option : options) {
                result = option.getObject(key);
                if (result != null) {
                    break;
                }
            }
        }
        return (T) result;
    }

    /**
     * Creates a composite option from the given options.
     *
     * @param options the delegate options to combine
     * @return a new composite option
     */
    public static Option of(Option... options) {
        return new CompositeOption(options);
    }
}
