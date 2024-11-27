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
package com.jd.live.agent.governance.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Axkea
 */
@Setter
@Getter
@AllArgsConstructor
public class ExceptionMessage implements Serializable {

    private Set<String> exceptionNames;

    private String message;

    private ExceptionMessage causeBy;

    public ExceptionMessage() {
        this.exceptionNames = new HashSet<>(8);
    }

    public void compress(Set<String> exclude) {
        ExceptionMessage p = this;
        while (p != null) {
            p.exceptionNames.removeAll(exclude);
            p = p.causeBy;
        }
    }

    public boolean containsException(Set<String> exceptionNames) {
        ExceptionMessage p = this;
        while (p != null) {
            if (ErrorPolicy.containsException(this.exceptionNames, exceptionNames)) {
                return true;
            }
            p = p.causeBy;
        }
        return false;
    }

    public static ExceptionMessage build(Throwable throwable) {
        Throwable t = throwable;
        ExceptionMessage sentinel = new ExceptionMessage();
        ExceptionMessage next = null, pre = sentinel;
        while (t != null) {
            next = new ExceptionMessage();
            next.setMessage(t.getMessage());
            Class<?> clazz = t.getClass();
            next.getExceptionNames().add(clazz.getName());
            while (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
                next.getExceptionNames().add(clazz.getSuperclass().getName());
                clazz = clazz.getSuperclass();
            }
            pre.setCauseBy(next);
            pre = next;
            t = t.getCause();
        }
        return sentinel.getCauseBy();
    }
}



