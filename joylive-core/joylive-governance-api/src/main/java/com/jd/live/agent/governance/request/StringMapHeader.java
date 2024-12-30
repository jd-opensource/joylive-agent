package com.jd.live.agent.governance.request;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class StringMapHeader implements HeaderReader, HeaderWriter {
    private Map<String, String> map;

    private BiConsumer<String, String> setHeader;

    public StringMapHeader(Supplier<Map<String, String>> getHeader, BiConsumer<String, String> setHeader) {
        this.setHeader = setHeader;
        this.map = getHeader.get() == null ? new HashMap<>() : getHeader.get();
    }

    @Override
    public Iterator<String> getHeaderNames() {
        return map.keySet().iterator();
    }

    @Override
    public List<String> getHeaders(String key) {
        return map.get(key) == null ? null : Collections.singletonList(map.get(key));
    }

    @Override
    public void setHeader(String key, String value) {
        setHeader.accept(key, value);
    }
}
