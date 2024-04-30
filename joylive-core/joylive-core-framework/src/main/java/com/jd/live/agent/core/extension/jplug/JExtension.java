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
package com.jd.live.agent.core.extension.jplug;

import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.core.exception.InstantiateException;
import com.jd.live.agent.core.extension.ExtensionDesc;
import com.jd.live.agent.core.extension.ExtensionEvent;
import com.jd.live.agent.core.extension.ExtensionListener;
import com.jd.live.agent.core.extension.Name;
import lombok.Getter;

/**
 * JExtension
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Getter
public class JExtension<T> implements ExtensionDesc<T> {

    private final Name<T> extensible;

    private final Name<T> name;

    private final String provider;

    private final int order;

    private final boolean singleton;

    private final ClassLoader classLoader;

    private final Instantiation instantiation;

    private final ExtensionListener listener;

    protected volatile T target;

    public JExtension(Name<T> extensible, Name<T> name, String provider, int order, boolean singleton,
                      ClassLoader classLoader, Instantiation instantiation, ExtensionListener listener) {
        this.extensible = extensible;
        this.name = name;
        this.provider = provider;
        this.order = order;
        this.singleton = singleton;
        this.classLoader = classLoader;
        this.instantiation = instantiation;
        this.listener = listener;
    }

    public T getTarget() {
        if (!singleton) {
            return newInstance();
        } else if (target == null) {
            synchronized (this) {
                if (target == null) {
                    target = newInstance();
                }
            }
        }
        return target;
    }

    private T newInstance() {
        Thread currentThread = Thread.currentThread();
        ClassLoader contextClassLoader = currentThread.getContextClassLoader();
        boolean flag = classLoader != null && contextClassLoader != classLoader;
        if (flag) {
            currentThread.setContextClassLoader(classLoader);
        }
        try {
            T result = instantiation.newInstance(name);
            if (result != null) {
                publish(new ExtensionEvent(this, result));
            }
            return result;
        } catch (Throwable e) {
            publish(new ExtensionEvent(this, e));
            if (e instanceof LiveException) {
                throw (LiveException) e;
            } else {
                throw new InstantiateException("failed to instantiate extension " + name.toString(), e);
            }
        } finally {
            if (flag) {
                currentThread.setContextClassLoader(contextClassLoader);
            }
        }
    }

    protected void publish(ExtensionEvent event) {
        if (listener != null) {
            listener.onEvent(event);
        }
    }


}
