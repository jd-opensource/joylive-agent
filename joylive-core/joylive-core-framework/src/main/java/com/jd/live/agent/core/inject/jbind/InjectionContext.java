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
package com.jd.live.agent.core.inject.jbind;

import com.jd.live.agent.core.inject.Injection;
import com.jd.live.agent.core.util.option.Option;

/**
 * The InjectionContext interface defines a context for dependency injection, which includes
 * the capabilities of selecting a converter and creating arrays. It also provides an
 * environment option and a method to determine if the context is for embedding.
 */
public interface InjectionContext extends ConverterSelector, ArrayFactory {

    /**
     * Determines whether this injection context is for embedding.
     *
     * @return true if the context is for embedding; false otherwise
     */
    default boolean isEmbed() {
        return false;
    }

    /**
     * Retrieves the environment option associated with this injection context.
     *
     * @return the Option representing the environment
     */
    Option getEnvironment();

    /**
     * The EmbedInjectionContext interface extends InjectionContext and is used to represent
     * an injection context that is specifically for embedding.
     */
    interface EmbedInjectionContext extends InjectionContext {

        /**
         * Overridden to always return true, indicating that this context is for embedding.
         *
         * @return true, as this context is always for embedding
         */
        @Override
        default boolean isEmbed() {
            return true;
        }

        /**
         * Builds an instance of the specified type within the embedding injection context.
         *
         * @param type the Class object representing the type to build
         * @return an instance of the specified type, built with the embedding context
         */
        Injection build(Class<?> type);
    }
}

