/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.core.mcp.converter;

import com.jd.live.agent.core.mcp.spec.v1.Tool;
import com.jd.live.agent.core.mcp.version.v1.McpVersion1;
import com.jd.live.agent.core.mcp.version.v2.McpVersion2;
import com.jd.live.agent.core.openapi.spec.v3.OpenApi;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.implement.parser.jackson.JacksonJsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

public class OpenApiConverterTest {

    private static String petstore3;

    private static ObjectParser objectParser;

    private static OpenApi openApi;

    @BeforeAll
    static void setup() throws IOException {
        objectParser = new JacksonJsonParser();
        petstore3 = readPetstore3();
        openApi = objectParser.read(new StringReader(petstore3), OpenApi.class);
        Assertions.assertNotNull(openApi);
    }

    @Test
    void testOpenApi3() {
        StringWriter writer = new StringWriter();
        objectParser.write(writer, openApi);
        String json = writer.toString();
        Assertions.assertNotNull(json);
    }

    @Test
    void testMcpVersion1() {
        OpenApiConverter converter = new OpenApiConverter(openApi, McpVersion1.INSTANCE);
        List<Tool> tools = converter.convert();
        Assertions.assertNotNull(tools);
    }

    @Test
    void testMcpVersion2() {
        OpenApiConverter converter = new OpenApiConverter(openApi, McpVersion2.INSTANCE);
        List<Tool> tools = converter.convert();
        Assertions.assertNotNull(tools);
    }

    private static String readPetstore3() throws IOException {
        try (InputStream in = OpenApiConverterTest.class.getResourceAsStream("/petstore3.json")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            String line;
            while ((line = reader.readLine()) != null) {
                bos.write(line.getBytes());
            }
            return bos.toString();
        }
    }

}
