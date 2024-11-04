package com.jd.live.agent.implement.parser.fastjson2.proxy.object;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.jd.live.agent.core.parser.json.JsonConverter;
import com.jd.live.agent.implement.parser.fastjson2.ProxySupport;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyObjectWriter implements ObjectWriter<Object> {

    @Override
    public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
        JsonConverter jsonConverter;
        Map<String, JsonConverter> cachedMap = ProxySupport.jsonConverterWriteCachedMap.computeIfAbsent(this,
                key -> new ConcurrentHashMap<>());

        if (cachedMap.get(fieldName.toString()) == null) {
            jsonConverter = ProxySupport.jsonConverterThreadLocal.get().get(fieldName.toString()).pop();
            cachedMap.put(fieldName.toString(), jsonConverter);
        } else {
            jsonConverter = cachedMap.get(fieldName.toString());
        }

        if (jsonConverter != null) {
            jsonWriter.writeAny(jsonConverter.convert(object));
        }
    }
}

