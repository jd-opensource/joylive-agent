package com.jd.live.agent.implement.parser.fastjson2.proxy.time;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.jd.live.agent.implement.parser.fastjson2.ProxySupport;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyTimeWriter implements ObjectWriter<Object> {

    @Override
    public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
        ZoneId zoneId = jsonWriter.getContext().getZoneId();
        try {
            String[] timeInfo;
            Map<String, String[]> cachedMap = ProxySupport.timeFormatWriteCachedMap.computeIfAbsent(this,
                    key -> new ConcurrentHashMap<>());

            timeInfo = cachedMap.get(fieldName.toString());
            if (timeInfo == null) {
                timeInfo = ProxySupport.timeFormatThreadLocal.get().get(fieldName.toString()).pop();
                cachedMap.put(fieldName.toString(), timeInfo);
            }

            String zoneIdString = timeInfo[1];
            if (zoneIdString != null && !zoneIdString.isEmpty()) {
                jsonWriter.getContext().setZoneId(ZoneId.of(zoneIdString));
            }

            String className = null;
            switch (fieldType.getTypeName()) {
                case "java.util.Date":
                    className = "com.jd.live.agent.shaded.com.alibaba.fastjson2.writer.ObjectWriterImplDate";
                    break;
                case "java.util.Calendar":
                    className = "com.jd.live.agent.shaded.com.alibaba.fastjson2.writer.ObjectWriterImplCalendar";
                    break;
                case "java.time.ZonedDateTime":
                    className = "com.jd.live.agent.shaded.com.alibaba.fastjson2.writer.ObjectWriterImplZonedDateTime";
                    if (zoneIdString != null && !zoneIdString.isEmpty()) {
                        object = ((ZonedDateTime) object).withZoneSameInstant(ZoneId.of(zoneIdString));
                    }
                    break;
                case "java.time.OffsetDateTime":
                    className = "com.jd.live.agent.shaded.com.alibaba.fastjson2.writer.ObjectWriterImplOffsetDateTime";
                    if (zoneIdString != null && !zoneIdString.isEmpty()) {
                        object = ((OffsetDateTime) object).atZoneSameInstant(ZoneId.of(zoneIdString)).toOffsetDateTime();
                    }
                    break;
                case "java.time.OffsetTime":
                    className = "com.jd.live.agent.shaded.com.alibaba.fastjson2.writer.ObjectWriterImplOffsetTime";
                    break;
                default:
                    throw new RuntimeException("unsupported type: " + fieldType.getTypeName());
            }

            write(timeInfo[0], className, jsonWriter, object, fieldName, fieldType, features);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            jsonWriter.getContext().setZoneId(zoneId);
        }
    }

    private void write(String format, String className, JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) throws Exception {
        Class<?> clazz = Class.forName(className);
        Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, Locale.class);
        constructor.setAccessible(true);
        Object instance = constructor.newInstance(format, null);
        Method method = clazz.getDeclaredMethod("write", JSONWriter.class, Object.class, Object.class, Type.class, long.class);
        method.setAccessible(true);
        method.invoke(instance, jsonWriter, object, fieldName, fieldType, features);
    }
}
