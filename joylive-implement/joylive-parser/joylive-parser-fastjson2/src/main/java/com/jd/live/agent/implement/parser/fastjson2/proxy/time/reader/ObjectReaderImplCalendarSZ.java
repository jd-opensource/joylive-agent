package com.jd.live.agent.implement.parser.fastjson2.proxy.time.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import com.alibaba.fastjson2.reader.ObjectReader;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class ObjectReaderImplCalendarSZ extends DateTimeCodec implements ObjectReader {

    public ObjectReaderImplCalendarSZ(String format, Locale locale) {
        super(format, locale);
    }

    @Override
    public Class getObjectClass() {
        return Calendar.class;
    }

    @Override
    public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        if (jsonReader.isInt()) {
            long millis = jsonReader.readInt64Value();
            if (formatUnixTime) {
                millis *= 1000;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(millis);
            return calendar;
        }

        if (jsonReader.readIfNull()) {
            return null;
        }

        long millis = jsonReader.readMillisFromString();
        if (formatUnixTime) {
            millis *= 1000;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar;
    }

    @Override
    public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        if (jsonReader.isString()) {
            if (format != null) {
                SimpleDateFormat dateFormat;
                if (locale != null) {
                    dateFormat = new SimpleDateFormat(format, locale);
                } else {
                    dateFormat = new SimpleDateFormat(format);
                }
                dateFormat.setTimeZone(TimeZone.getTimeZone(jsonReader.getContext().getZoneId()));
                try {
                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC));
                    calendar.setTime(dateFormat.parse(jsonReader.readString()));
                    return calendar;
                } catch (ParseException e) {
                    throw new JSONException(e.getMessage(), e);
                }
            }

            long millis = jsonReader.readMillisFromString();
            if (millis == 0 && jsonReader.wasNull()) {
                return null;
            }

            if (formatUnixTime) {
                millis *= 1000;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(millis);
            return calendar;
        }

        if (jsonReader.readIfNull()) {
            return null;
        }

        long millis = jsonReader.readMillisFromString();
        if (formatUnixTime || jsonReader.getContext().isFormatUnixTime()) {
            millis *= 1000;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar;
    }
}

