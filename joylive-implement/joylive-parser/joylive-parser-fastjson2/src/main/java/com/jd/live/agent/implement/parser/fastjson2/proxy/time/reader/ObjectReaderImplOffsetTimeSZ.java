package com.jd.live.agent.implement.parser.fastjson2.proxy.time.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import com.alibaba.fastjson2.reader.ObjectReader;

import java.lang.reflect.Type;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ObjectReaderImplOffsetTimeSZ extends DateTimeCodec implements ObjectReader {

    public ObjectReaderImplOffsetTimeSZ(String format, Locale locale) {
        super(format, locale);
    }

    @Override
    public Class getObjectClass() {
        return OffsetTime.class;
    }

    @Override
    public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        return readObject(jsonReader, fieldType, fieldName, features);
    }

    @Override
    public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        JSONReader.Context context = jsonReader.getContext();
        if (jsonReader.isInt()) {
            long millis = jsonReader.readInt64Value();
            if (formatUnixTime || context.isFormatUnixTime()) {
                millis *= 1000;
            }
            Instant instant = Instant.ofEpochMilli(millis);
            return instant.atOffset(ZoneOffset.UTC).toOffsetTime();
        }

        if (jsonReader.readIfNull()) {
            return null;
        }

        if (format == null) {
            return jsonReader.readOffsetDateTime();
        }

        String str = jsonReader.readString();
        ZoneId zoneId = context.getZoneId();
        if (formatMillis || formatUnixTime) {
            long millis = Long.parseLong(str);
            if (formatUnixTime) {
                millis *= 1000L;
            }
            Instant instant = Instant.ofEpochMilli(millis);
            return instant.atOffset(ZoneOffset.UTC);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withZone(zoneId);
        return OffsetTime.parse(str, formatter);
    }
}

