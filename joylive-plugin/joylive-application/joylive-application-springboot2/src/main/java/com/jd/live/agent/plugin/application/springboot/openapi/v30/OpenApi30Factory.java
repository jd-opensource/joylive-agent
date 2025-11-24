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
package com.jd.live.agent.plugin.application.springboot.openapi.v30;

import com.jd.live.agent.core.openapi.spec.v3.OpenApi;
import com.jd.live.agent.core.openapi.spec.v3.info.Info;
import com.jd.live.agent.core.openapi.spec.v3.info.License;
import com.jd.live.agent.core.openapi.spec.v3.media.Schema;
import com.jd.live.agent.plugin.application.springboot.openapi.v31.OpenApi31Factory;
import io.swagger.v3.oas.models.OpenAPI;

import java.util.ArrayList;

import static com.jd.live.agent.core.util.CollectionUtils.*;

/**
 * Factory implementation for creating OpenApi objects from OpenAPI 3.1 specifications.
 */
public class OpenApi30Factory extends OpenApi31Factory {

    @Override
    protected OpenApi build(OpenAPI openApi) {
        return OpenApi.builder()
                .openapi(openApi.getOpenapi())
                .info(buildInfo(openApi.getInfo()))
                .externalDocs(buildExternalDoc(openApi.getExternalDocs()))
                .servers(toList(openApi.getServers(), this::buildServer))
                .tags(toList(openApi.getTags(), this::buildTag))
                .paths(toLinkMap(openApi.getPaths(), this::buildPathItem))
                .components(buildComponents(openApi.getComponents()))
                .security(toList(openApi.getSecurity(), item -> toLinkMap(item, v -> new ArrayList<>(v))))
                .specVersion(openApi.getSpecVersion().name())
                .extensions(copy(openApi.getExtensions()))
                .build();
    }

    @Override
    protected Schema buildSchema(io.swagger.v3.oas.models.media.Schema schema) {
        if (schema == null) {
            return null;
        }
        return Schema.builder()
                .ref(schema.get$ref())
                .name(schema.getName())
                .title(schema.getTitle())
                .description(schema.getDescription())
                .externalDocs(buildExternalDoc(schema.getExternalDocs()))
                .type(schema.getType())
                .format(schema.getFormat())
                .defaultValue(schema.getDefault())
                .properties(toLinkMap(schema.getProperties(), this::buildSchema))
                .additionalProperties(schema.getAdditionalProperties())
                .items(buildSchema(schema.getItems()))
                .required(schema.getRequired() == null ? null : new ArrayList<>(schema.getRequired()))
                .nullable(schema.getNullable())
                .readOnly(schema.getReadOnly())
                .writeOnly(schema.getWriteOnly())
                .deprecated(schema.getDeprecated())
                .multipleOf(schema.getMultipleOf())
                .maximum(schema.getMaximum())
                .exclusiveMaximum(schema.getExclusiveMaximum())
                .minimum(schema.getMinimum())
                .exclusiveMinimum(schema.getExclusiveMinimum())
                .maxLength(schema.getMaxLength())
                .minLength(schema.getMinLength())
                .pattern(schema.getPattern())
                .maxItems(schema.getMaxItems())
                .minItems(schema.getMinItems())
                .uniqueItems(schema.getUniqueItems())
                .maxProperties(schema.getMaxProperties())
                .minProperties(schema.getMinProperties())
                .enums(schema.getEnum())
                .allOf(toList(schema.getAllOf(), this::buildSchema))
                .anyOf(toList(schema.getAnyOf(), this::buildSchema))
                .oneOf(toList(schema.getOneOf(), this::buildSchema))
                .not(buildSchema(schema.getNot()))
                .xml(buildXml(schema.getXml()))
                .discriminator(buildDiscriminator(schema.getDiscriminator()))
                .example(schema.getExample())
                .extensions(copy(schema.getExtensions()))
                .build();
    }

    @Override
    protected Info buildInfo(io.swagger.v3.oas.models.info.Info info) {
        return Info.builder()
                .title(info.getTitle())
                .description(info.getDescription())
                .termsOfService(info.getTermsOfService())
                .version(info.getVersion())
                .contact(buildContact(info.getContact()))
                .license(buildLicense(info.getLicense()))
                .extensions(copy(info.getExtensions()))
                .build();
    }

    @Override
    protected License buildLicense(io.swagger.v3.oas.models.info.License license) {
        return License.builder()
                .name(license.getName())
                .url(license.getUrl())
                .extensions(copy(license.getExtensions()))
                .build();
    }

}