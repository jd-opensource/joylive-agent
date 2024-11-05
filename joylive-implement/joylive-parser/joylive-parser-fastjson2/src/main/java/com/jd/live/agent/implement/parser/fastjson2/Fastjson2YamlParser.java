package com.jd.live.agent.implement.parser.fastjson2;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.parser.ObjectParser;

@Extension(value = {ObjectParser.YAML, ObjectParser.YML}, provider = "fastjson2")
public class Fastjson2YamlParser extends AbstractFastjson2Parser {
    @Override
    protected String getSupportedType() {
        return "YAML";
    }
}