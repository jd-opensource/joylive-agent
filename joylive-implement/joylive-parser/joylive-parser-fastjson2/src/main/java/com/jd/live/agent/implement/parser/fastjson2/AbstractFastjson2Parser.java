package com.jd.live.agent.implement.parser.fastjson2;

import com.alibaba.fastjson2.JSONFactory;
import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.parser.ConfigParser;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public abstract class AbstractFastjson2Parser implements ConfigParser, ObjectParser {

    static {
        JSONFactory.getDefaultObjectWriterProvider().register(new JoyLiveWriterModule());
        JSONFactory.getDefaultObjectReaderProvider().register(new JoyLiveReaderModule());
    }

    private final Yaml yaml = new Yaml();

    @Override
    public Map<String, Object> parse(Reader reader) {
        return Collections.emptyMap();
    }

    @Override
    public <T> T read(Reader reader, Class<T> clazz) {
        if (reader == null || clazz == null)
            return null;
        try {
            switch (getSupportedType()) {
                case YAML:
                case YML:
                    return com.alibaba.fastjson2.JSON.parseObject(
                            com.alibaba.fastjson2.JSON.toJSONString(yaml.load(reader)),
                            clazz
                    );
                default:
                    return com.alibaba.fastjson2.JSON.parseObject(reader, clazz);
            }
        } catch (Exception e) {
            throw new ParseException("read error. caused by " + e.getMessage(), e);
        } finally {
            ProxySupport.cachedKey.remove();
            ProxySupport.jsonConverterThreadLocal.remove();
            ProxySupport.timeFormatThreadLocal.remove();
        }
    }

    @Override
    public <T> T read(Reader reader, TypeReference<T> reference) {
        if (reader == null || reference == null)
            return null;
        try {
            switch (getSupportedType()) {
                case YAML:
                case YML:
                    return com.alibaba.fastjson2.JSON.parseObject(
                            com.alibaba.fastjson2.JSON.toJSONString(yaml.load(reader)),
                            reference.getType()
                    );
                default:
                    return com.alibaba.fastjson2.JSON.parseObject(reader, reference.getType());
            }
        } catch (Exception e) {
            throw new ParseException("read error. caused by " + e.getMessage(), e);
        } finally {
            ProxySupport.cachedKey.remove();
            ProxySupport.jsonConverterThreadLocal.remove();
            ProxySupport.timeFormatThreadLocal.remove();
        }
    }

    @Override
    public <T> T read(Reader reader, Type type) {
        if (reader == null || type == null)
            return null;
        try {
            switch (getSupportedType()) {
                case YAML:
                case YML:
                    return com.alibaba.fastjson2.JSON.parseObject(
                            com.alibaba.fastjson2.JSON.toJSONString(yaml.load(reader)),
                            type
                    );
                default:
                    return com.alibaba.fastjson2.JSON.parseObject(reader, type);
            }
        } catch (Exception e) {
            throw new ParseException("read error. caused by " + e.getMessage(), e);
        } finally {
            ProxySupport.cachedKey.remove();
            ProxySupport.jsonConverterThreadLocal.remove();
            ProxySupport.timeFormatThreadLocal.remove();
        }
    }

    @Override
    public void write(Writer writer, Object obj) {
        if (writer == null || obj == null)
            return;
        try {
            ByteArrayOutputStream strStream = new ByteArrayOutputStream();
            com.alibaba.fastjson2.JSON.writeTo(strStream, obj);
            String str = strStream.toString(StandardCharsets.UTF_8.name());
            writer.write(str);
            writer.flush();
        } catch (Exception e) {
            throw new ParseException("write error. caused by " + e.getMessage(), e);
        } finally {
            ProxySupport.cachedKey.remove();
            ProxySupport.jsonConverterThreadLocal.remove();
            ProxySupport.timeFormatThreadLocal.remove();
        }
    }

    protected String getSupportedType() {
        return JSON;
    }
}
