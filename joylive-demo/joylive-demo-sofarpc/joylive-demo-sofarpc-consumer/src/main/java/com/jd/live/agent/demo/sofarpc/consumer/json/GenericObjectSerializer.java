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
package com.jd.live.agent.demo.sofarpc.consumer.json;

import com.alipay.hessian.generic.model.GenericObject;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;

public class GenericObjectSerializer extends JsonSerializer<GenericObject> {

    @Override
    public void serialize(GenericObject person, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        if (person.getFields() != null) {
            for (Map.Entry<String, Object> entry : person.getFields().entrySet()) {
                gen.writeObjectField(entry.getKey(), entry.getValue());
            }
        }
        gen.writeEndObject();
    }
}