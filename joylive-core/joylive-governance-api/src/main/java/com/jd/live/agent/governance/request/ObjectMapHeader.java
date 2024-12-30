package com.jd.live.agent.governance.request;

import com.jd.live.agent.core.util.tag.Label;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ObjectMapHeader implements HeaderReader, HeaderWriter {
    private Map<String, Object> map;

    private BiConsumer<String, String> setHeader;

    public ObjectMapHeader(Supplier<Map<String, Object>> getHeader, BiConsumer<String, String> setHeader) {
        this.setHeader = setHeader;
        this.map = getHeader.get() == null ? new HashMap<>() : getHeader.get();
    }

    @Override
    public Iterator<String> getHeaderNames() {
        return map.keySet().iterator();
    }

    @Override
    public List<String> getHeaders(String key) {
        Object value = map.get(key);
        return value == null ? null : Label.parseValue(value.toString());
    }

    @Override
    public void setHeader(String key, String value) {
        setHeader.accept(key, value);
    }
}
