package com.jd.live.agent.implement.parser.fastjson2.proxy.time.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import com.alibaba.fastjson2.reader.ObjectReader;

import java.lang.reflect.Type;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ObjectReaderImplZonedDateTimeSZ extends DateTimeCodec implements ObjectReader {

    public ObjectReaderImplZonedDateTimeSZ(String format, Locale locale) {
        super(format, locale);
    }

    @Override
    public Class getObjectClass() {
        return ZonedDateTime.class;
    }

    @Override
    public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        ZonedDateTime zdt;
        if (jsonReader.isInt()) {
            long millis = jsonReader.readInt64Value();
            if (formatUnixTime) {
                millis *= 1000;
            }
            Instant instant = Instant.ofEpochMilli(millis);
            zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
        } else {
            if (jsonReader.readIfNull()) {
                zdt = null;
            } else if (format == null || yyyyMMddhhmmss19 || formatISO8601) {
                zdt = jsonReader.readZonedDateTime().withZoneSameInstant(ZoneOffset.UTC);
            } else {
                String str = jsonReader.readString();
                if (formatMillis || formatUnixTime) {
                    long millis = Long.parseLong(str);
                    if (formatUnixTime) {
                        millis *= 1000L;
                    }
                    Instant instant = Instant.ofEpochMilli(millis);
                    zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withZone(jsonReader.getContext().getZoneId());
                    zdt = ZonedDateTime.from(formatter.parse(str)).withZoneSameInstant(ZoneOffset.UTC);
                }
            }
        }

        return zdt;
    }
}
