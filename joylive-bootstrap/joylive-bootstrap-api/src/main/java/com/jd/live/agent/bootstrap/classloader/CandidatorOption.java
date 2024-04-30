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


public class CandidatorOption {

    private static final ThreadLocal<CandidatorOption> optionThreadLocal = new ThreadLocal<>();

    private static boolean contextLoaderEnabled;

    private ClassLoader classLoader;

    private boolean enabled = true;

    public CandidatorOption() {
    }

    public CandidatorOption(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public CandidatorOption(ClassLoader classLoader, boolean enabled) {
        this.classLoader = classLoader;
        this.enabled = enabled;
    }

    public ClassLoader getClassLoader() {
        if (!enabled)
            return null;
        else if (classLoader != null)
            return classLoader;
        else if (contextLoaderEnabled)
            return Thread.currentThread().getContextClassLoader();
        else
            return null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static CandidatorOption getOption() {
        CandidatorOption result = optionThreadLocal.get();
        if (result == null && contextLoaderEnabled) {
            result = new CandidatorOption();
        }
        return result;
    }

    public static CandidatorOption setOption(CandidatorOption option) {
        optionThreadLocal.set(option);
        return option;
    }

    public static CandidatorOption setOption(ClassLoader classLoader) {
        return setOption(classLoader, true);
    }

    public static CandidatorOption setOption(ClassLoader classLoader, boolean enabled) {
        CandidatorOption option = new CandidatorOption(classLoader, enabled);
        optionThreadLocal.set(option);
        return option;
    }

    public static void disable() {
        CandidatorOption option = optionThreadLocal.get();
        optionThreadLocal.set(new CandidatorOption(option == null ? null : option.getClassLoader(), false));
    }

    public static void setContextLoaderEnabled(boolean contextLoaderEnabled) {
        CandidatorOption.contextLoaderEnabled = contextLoaderEnabled;
    }
}
