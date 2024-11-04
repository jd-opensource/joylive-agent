package com.jd.live.agent.implement.parser.fastjson2.proxy.time.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import com.alibaba.fastjson2.reader.ObjectReader;

import java.lang.reflect.Type;
import java.time.*;
import java.util.Locale;

public class ObjectReaderImplOffsetDateTimeSZ extends DateTimeCodec implements ObjectReader {

    public ObjectReaderImplOffsetDateTimeSZ(String format, Locale locale) {
        super(format, locale);
    }

    @Override
    public Class getObjectClass() {
        return OffsetDateTime.class;
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
            return instant.atOffset(ZoneOffset.UTC);
        }

        if (jsonReader.readIfNull()) {
            return null;
        }

        if (format == null || yyyyMMddhhmmss19 || formatISO8601) {
            return jsonReader.readOffsetDateTime().atZoneSameInstant(ZoneOffset.UTC);
        }

        String str = jsonReader.readString();
        if (formatMillis || formatUnixTime) {
            long millis = Long.parseLong(str);
            if (formatUnixTime) {
                millis *= 1000L;
            }

            Instant instant = Instant.ofEpochMilli(millis);
            return instant.atOffset(ZoneOffset.UTC);
        }

        return OffsetDateTime.parse(str, getDateFormatter());
    }
}

