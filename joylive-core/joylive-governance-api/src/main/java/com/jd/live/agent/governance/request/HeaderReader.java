package com.jd.live.agent.governance.request;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public interface HeaderReader {
    Iterator<String> getHeaderNames();

    List<String> getHeaders(String key);

    default String getHeader(String key) {
        List<String> values = getHeaders(key);
        return values.isEmpty() ? null : values.get(0);
    }

    default List<String> getHeader(String key, Function<String, List<String>> func) {
        String value = getHeader(key);
        return value == null ? null : func.apply(value);
    }
}
