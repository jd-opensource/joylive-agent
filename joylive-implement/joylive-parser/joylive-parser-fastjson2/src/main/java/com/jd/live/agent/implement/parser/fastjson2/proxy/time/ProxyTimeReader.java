package com.jd.live.agent.implement.parser.fastjson2.proxy.time;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.jd.live.agent.implement.parser.fastjson2.ProxySupport;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyTimeReader implements ObjectReader<Object> {

    @Override
    public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        ZoneId zoneId = jsonReader.getContext().getZoneId();
        String[] timeInfo;

        try {
            Map<String, String[]> cachedMap = ProxySupport.timeFormatReadCachedMap.computeIfAbsent(this,
                    key -> new ConcurrentHashMap<>());

            if (cachedMap.get(fieldName.toString()) == null) {
                timeInfo = ProxySupport.timeFormatThreadLocal.get().get(fieldName.toString()).pop();
                cachedMap.put(fieldName.toString(), timeInfo);
            } else {
                timeInfo = cachedMap.get(fieldName.toString());
            }

            String className = null;
            switch (fieldType.getTypeName()) {
                case "java.util.Date":
                    className = "com.jd.live.agent.implement.parser.fastjson2.proxy.time.reader.ObjectReaderImplDateSZ";
                    break;
                case "java.util.Calendar":
                    className = "com.jd.live.agent.implement.parser.fastjson2.proxy.time.reader.ObjectReaderImplCalendarSZ";
                    break;
                case "java.time.ZonedDateTime":
                    className = "com.jd.live.agent.implement.parser.fastjson2.proxy.time.reader.ObjectReaderImplZonedDateTimeSZ";
                    break;
                case "java.time.OffsetDateTime":
                    className = "com.jd.live.agent.implement.parser.fastjson2.proxy.time.reader.ObjectReaderImplOffsetDateTimeSZ";
                    break;
                case "java.time.OffsetTime":
                    className = "com.jd.live.agent.implement.parser.fastjson2.proxy.time.reader.ObjectReaderImplOffsetTimeSZ";
                    break;
                default:
                    throw new RuntimeException("unsupported type: " + fieldType.getTypeName());
            }

            String zoneIdString = timeInfo[1];
            if (zoneIdString != null && !zoneIdString.isEmpty()) {
                jsonReader.getContext().setZoneId(ZoneId.of(zoneIdString));
            }

            return readObject(timeInfo[0], className, jsonReader, fieldType, fieldName, features);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            jsonReader.getContext().setZoneId(zoneId);
        }
    }

    private Object readObject(String format, String className, JSONReader jsonReader, Type fieldType, Object fieldName, long features) throws Exception {
        Class<?> clazz = Class.forName(className);
        Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, Locale.class);
        constructor.setAccessible(true);
        Object instance = constructor.newInstance(format, null);
        Method method = clazz.getDeclaredMethod("readObject", JSONReader.class, Type.class, Object.class, long.class);
        method.setAccessible(true);
        return method.invoke(instance, jsonReader, fieldType, fieldName, features);
    }
}

