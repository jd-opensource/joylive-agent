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



