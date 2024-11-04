package com.jd.live.agent.implement.parser.fastjson2.proxy.time.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.util.TypeUtils;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ObjectReaderImplDateSZ extends DateTimeCodec implements ObjectReader {

    public ObjectReaderImplDateSZ(String format, Locale locale) {
        super(format, locale);
    }

    @Override
    public Class getObjectClass() {
        return Date.class;
    }

    @Override
    public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        return readDate(jsonReader);
    }

    @Override
    public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        return readDate(jsonReader);
    }

    private Object readDate(JSONReader jsonReader) {
        if (jsonReader.isInt()) {
            long millis = jsonReader.readInt64Value();
            if (formatUnixTime) {
                millis *= 1000;
            }
            return new Date(millis);
        }

        if (jsonReader.readIfNull()) {
            return null;
        }

        if (jsonReader.nextIfNullOrEmptyString()) {
            return null;
        }

        if (jsonReader.current() == 'n') {
            return jsonReader.readNullOrNewDate();
        }

        long millis;
        if ((formatUnixTime || formatMillis) && jsonReader.isString()) {
            millis = jsonReader.readInt64Value();
            if (formatUnixTime) {
                millis *= 1000L;
            }
        } else if (format != null || useSimpleFormatter) {
            if (yyyyMMddhhmmss19) {
                if (jsonReader.isSupportSmartMatch()) {
                    millis = jsonReader.readMillisFromString();
                } else {
                    millis = jsonReader.readMillis19();
                }
                return new Date(millis);
            } else {
                SimpleDateFormat dateFormat;
                if (locale != null) {
                    dateFormat = new SimpleDateFormat(format, locale);
                } else {
                    dateFormat = new SimpleDateFormat(format);
                }
                dateFormat.setTimeZone(TimeZone.getTimeZone(jsonReader.getContext().getZoneId()));
                try {
                    return dateFormat.parse(jsonReader.readString());
                } catch (ParseException e) {
                    throw new JSONException(e.getMessage(), e);
                }
            }
        } else {
            if (jsonReader.isDate()) {
                return jsonReader.readDate();
            }

            if (jsonReader.isTypeRedirect() && jsonReader.nextIfMatchIdent('"', 'v', 'a', 'l', '"')) {
                jsonReader.nextIfMatch(':');
                millis = jsonReader.readInt64Value();
                jsonReader.nextIfObjectEnd();
                jsonReader.setTypeRedirect(false);
            } else {
                millis = jsonReader.readMillisFromString();
            }

            if (millis == 0 && jsonReader.wasNull()) {
                return null;
            }

            if (formatUnixTime) {
                millis *= 1000;
            }
        }
        return new Date(millis);
    }

    public Date createInstance(Map map, long features) {
        return TypeUtils.toDate(map);
    }
}
