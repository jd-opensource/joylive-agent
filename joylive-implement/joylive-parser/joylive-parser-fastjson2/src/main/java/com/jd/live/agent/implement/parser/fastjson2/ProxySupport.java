package com.jd.live.agent.implement.parser.fastjson2;

import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.jd.live.agent.core.parser.json.JsonConverter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

public class ProxySupport {
    public static ThreadLocal<Map<String, Stack<JsonConverter>>> jsonConverterThreadLocal =
            ThreadLocal.withInitial(HashMap::new);

    public static ThreadLocal<Map<String, Stack<String[]>>> timeFormatThreadLocal =
            ThreadLocal.withInitial(HashMap::new);

    public static ThreadLocal<HashSet<String>> cachedKey = ThreadLocal.withInitial(HashSet::new);

    public static Map<String, JsonConverter> converterMap = new ConcurrentHashMap<>();

    public static Map<ObjectReader, Map<String, JsonConverter>> jsonConverterReadCachedMap = new ConcurrentHashMap<>();

    public static Map<ObjectWriter, Map<String, JsonConverter>> jsonConverterWriteCachedMap = new ConcurrentHashMap<>();

    public static Map<ObjectReader, Map<String, String[]>> timeFormatReadCachedMap = new ConcurrentHashMap<>();

    public static Map<ObjectWriter, Map<String, String[]>> timeFormatWriteCachedMap = new ConcurrentHashMap<>();
}
