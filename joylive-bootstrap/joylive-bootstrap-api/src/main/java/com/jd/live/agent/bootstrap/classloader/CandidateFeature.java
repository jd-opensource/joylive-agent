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
package com.jd.live.agent.bootstrap.classloader;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

@Getter
@Setter
public class CandidateFeature {

    private boolean contextLoaderEnabled = true;

    private Predicate<ClassLoader> predicate;

    public boolean test(ClassLoader classLoader) {
        return classLoader != null && (predicate == null || predicate.test(classLoader));
    }

    public Class<?> disableAndRun(Callable<Class<?>> callable) throws ClassNotFoundException {
        boolean enabled = contextLoaderEnabled;
        try {
            contextLoaderEnabled = false;
            return callable.call();
        } catch (ClassNotFoundException e) {
            throw e;
        } catch (NoClassDefFoundError e) {
            throw new ClassNotFoundException(e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            contextLoaderEnabled = enabled;
        }
    }

}
