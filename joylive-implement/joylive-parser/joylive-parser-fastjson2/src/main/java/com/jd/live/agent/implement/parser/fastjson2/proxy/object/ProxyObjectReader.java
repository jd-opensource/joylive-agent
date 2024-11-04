package com.jd.live.agent.implement.parser.fastjson2.proxy.object;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.jd.live.agent.core.parser.json.JsonConverter;
import com.jd.live.agent.implement.parser.fastjson2.ProxySupport;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyObjectReader implements ObjectReader<Object> {

    @Override
    public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        JsonConverter jsonConverter;
        Map<String, JsonConverter> cachedMap = ProxySupport.jsonConverterReadCachedMap.computeIfAbsent(this,
                key -> new ConcurrentHashMap<>());

        if (cachedMap.get(fieldName.toString()) == null) {
            jsonConverter = ProxySupport.jsonConverterThreadLocal.get().get(fieldName.toString()).pop();
            cachedMap.put(fieldName.toString(), jsonConverter);
        } else {
            jsonConverter = cachedMap.get(fieldName.toString());
        }

        if (jsonConverter != null) {
            return jsonConverter.convert(jsonReader.readAny());
        }

        return null;
    }
}

