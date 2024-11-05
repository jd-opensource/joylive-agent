package com.jd.live.agent.implement.parser.fastjson2;

import com.alibaba.fastjson2.JSONPath;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.parser.JsonPathParser;
import com.jd.live.agent.core.parser.ObjectParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Extension(value = ObjectParser.JSON, provider = "fastjson2")
public class Fastjson2JsonParser extends AbstractFastjson2Parser implements JsonPathParser {

    @Override
    public <T> T read(String reader, String path) {
        return (T) JSONPath.eval(reader, path);
    }

    @Override
    public <T> T read(InputStream in, String path) {
        return (T) JSONPath.eval(
                new BufferedReader(new InputStreamReader(in))
                        .lines()
                        .collect(Collectors.joining(System.lineSeparator())),
                path);
    }
}

