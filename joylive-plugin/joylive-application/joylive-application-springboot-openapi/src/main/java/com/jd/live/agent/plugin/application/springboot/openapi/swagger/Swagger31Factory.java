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
package com.jd.live.agent.plugin.application.springboot.openapi.swagger;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.core.openapi.spec.v3.*;
import com.jd.live.agent.core.openapi.spec.v3.callbacks.Callback;
import com.jd.live.agent.core.openapi.spec.v3.examples.Example;
import com.jd.live.agent.core.openapi.spec.v3.headers.Header;
import com.jd.live.agent.core.openapi.spec.v3.info.Contact;
import com.jd.live.agent.core.openapi.spec.v3.info.Info;
import com.jd.live.agent.core.openapi.spec.v3.info.License;
import com.jd.live.agent.core.openapi.spec.v3.links.Link;
import com.jd.live.agent.core.openapi.spec.v3.media.*;
import com.jd.live.agent.core.openapi.spec.v3.parameters.Parameter;
import com.jd.live.agent.core.openapi.spec.v3.parameters.RequestBody;
import com.jd.live.agent.core.openapi.spec.v3.responses.ApiResponse;
import com.jd.live.agent.core.openapi.spec.v3.responses.ApiResponses;
import com.jd.live.agent.core.openapi.spec.v3.security.OAuthFlow;
import com.jd.live.agent.core.openapi.spec.v3.security.OAuthFlows;
import com.jd.live.agent.core.openapi.spec.v3.security.SecurityRequirement;
import com.jd.live.agent.core.openapi.spec.v3.security.SecurityScheme;
import com.jd.live.agent.core.openapi.spec.v3.servers.Server;
import com.jd.live.agent.core.openapi.spec.v3.servers.ServerVariable;
import com.jd.live.agent.core.openapi.spec.v3.tags.Tag;
import com.jd.live.agent.core.util.cache.LazyObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.CollectionUtils.*;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * Factory implementation for creating OpenApi objects from OpenAPI 3.1 specifications.
 * <p>
 * This class converts Swagger/OpenAPI 3.1 model objects to the internal OpenApi representation
 * used by the application. It handles all aspects of the OpenAPI specification including paths,
 * operations, schemas, security definitions, and more.
 */
public class Swagger31Factory extends SwaggerFactory {

    private final LazyObject<OpenApi> openApi;

    public Swagger31Factory(Callable<io.swagger.v3.oas.models.OpenAPI> callable) {
        this.openApi = new LazyObject<>(() -> {
            try {
                return build(callable.call());
            } catch (Exception e) {
                return null;
            }
        });
    }

    @Override
    public OpenApi create() {
        return openApi.get();
    }

    /**
     * Builds an OpenApi object from an OpenAPI 3.1 specification.
     * <p>
     * This method transforms the root OpenAPI object and all its components.
     *
     * @param openApi The OpenAPI 3.1 specification
     * @return The resulting OpenApi object
     */
    protected OpenApi build(io.swagger.v3.oas.models.OpenAPI openApi) {
        return OpenApi.builder()
                .openapi(openApi.getOpenapi())
                .info(buildInfo(openApi.getInfo()))
                .externalDocs(buildExternalDoc(openApi.getExternalDocs()))
                .servers(toList(openApi.getServers(), this::buildServer))
                .tags(toList(openApi.getTags(), this::buildTag))
                .paths(toLinkMap(openApi.getPaths(), this::buildPathItem))
                .components(buildComponents(openApi.getComponents()))
                .security(toList(openApi.getSecurity(), item -> toLinkMap(item, v -> new ArrayList<>(v))))
                .jsonSchemaDialect(openApi.getJsonSchemaDialect())
                .specVersion(openApi.getSpecVersion() == null ? null : openApi.getSpecVersion().name())
                .webhooks(toLinkMap(openApi.getWebhooks(), this::buildPathItem))
                .extensions(copy(openApi.getExtensions()))
                .build();
    }

    /**
     * Builds an Info object from the OpenAPI Info object.
     *
     * @param info The OpenAPI Info object
     * @return The internal Info representation
     */
    protected Info buildInfo(io.swagger.v3.oas.models.info.Info info) {
        if (info == null) {
            return null;
        }
        return Info.builder()
                .title(info.getTitle())
                .summary(info.getSummary())
                .description(info.getDescription())
                .termsOfService(info.getTermsOfService())
                .version(info.getVersion())
                .contact(buildContact(info.getContact()))
                .license(buildLicense(info.getLicense()))
                .extensions(copy(info.getExtensions()))
                .build();
    }

    /**
     * Builds a Contact object from the OpenAPI Contact object.
     *
     * @param contact The OpenAPI Contact object
     * @return The internal Contact representation
     */
    protected Contact buildContact(io.swagger.v3.oas.models.info.Contact contact) {
        if (contact == null) {
            return null;
        }
        return Contact.builder()
                .name(contact.getName())
                .url(contact.getUrl())
                .email(contact.getEmail())
                .extensions(copy(contact.getExtensions()))
                .build();
    }

    /**
     * Builds a License object from the OpenAPI License object.
     *
     * @param license The OpenAPI License object
     * @return The internal License representation
     */
    protected License buildLicense(io.swagger.v3.oas.models.info.License license) {
        if (license == null) {
            return null;
        }
        return License.builder()
                .name(license.getName())
                .url(license.getUrl())
                .identifier(license.getIdentifier())
                .extensions(copy(license.getExtensions()))
                .build();
    }

    /**
     * Builds an ExternalDocumentation object from the OpenAPI ExternalDocumentation object.
     *
     * @param doc The OpenAPI ExternalDocumentation object
     * @return The internal ExternalDocumentation representation
     */
    protected ExternalDoc buildExternalDoc(io.swagger.v3.oas.models.ExternalDocumentation doc) {
        if (doc == null) {
            return null;
        }
        return ExternalDoc.builder()
                .description(doc.getDescription())
                .url(doc.getUrl())
                .extensions(copy(doc.getExtensions()))
                .build();
    }

    /**
     * Builds a Server object from the OpenAPI Server object.
     *
     * @param server The OpenAPI Server object
     * @return The internal Server representation
     */
    protected Server buildServer(io.swagger.v3.oas.models.servers.Server server) {
        if (server == null) {
            return null;
        }
        return Server.builder()
                .url(server.getUrl())
                .description(server.getDescription())
                .variables(toLinkMap(server.getVariables(), this::buildServerVariable))
                .extensions(copy(server.getExtensions()))
                .build();
    }

    /**
     * Builds a Tag object from the OpenAPI Tag object.
     *
     * @param tag The OpenAPI Tag object
     * @return The internal Tag representation
     */
    protected Tag buildTag(io.swagger.v3.oas.models.tags.Tag tag) {
        if (tag == null) {
            return null;
        }
        return Tag.builder()
                .name(tag.getName())
                .description(tag.getDescription())
                .externalDocs(buildExternalDoc(tag.getExternalDocs()))
                .extensions(copy(tag.getExtensions()))
                .build();
    }

    /**
     * Builds a ServerVariable object from the OpenAPI ServerVariable object.
     *
     * @param variable The OpenAPI ServerVariable object
     * @return The internal ServerVariable representation
     */
    protected ServerVariable buildServerVariable(io.swagger.v3.oas.models.servers.ServerVariable variable) {
        if (variable == null) {
            return null;
        }
        return ServerVariable.builder()
                .enumValues(variable.getEnum() == null ? null : new ArrayList<>(variable.getEnum()))
                .defaultValue(variable.getDefault())
                .description(variable.getDescription())
                .extensions(copy(variable.getExtensions()))
                .build();
    }

    /**
     * Builds a PathItem object from the OpenAPI PathItem object.
     *
     * @param pathItem The OpenAPI PathItem object
     * @return The internal PathItem representation
     */
    protected PathItem buildPathItem(io.swagger.v3.oas.models.PathItem pathItem) {
        if (pathItem == null) {
            return null;
        }
        return PathItem.builder()
                .ref(pathItem.get$ref())
                .summary(pathItem.getSummary())
                .description(pathItem.getDescription())
                .parameters(toList(pathItem.getParameters(), this::buildParameter))
                .get(buildOperation(pathItem.getGet()))
                .head(buildOperation(pathItem.getHead()))
                .put(buildOperation(pathItem.getPut()))
                .patch(buildOperation(pathItem.getPatch()))
                .post(buildOperation(pathItem.getPost()))
                .delete(buildOperation(pathItem.getDelete()))
                .options(buildOperation(pathItem.getOptions()))
                .trace(buildOperation(pathItem.getTrace()))
                .extensions(copy(pathItem.getExtensions()))
                .build();
    }

    /**
     * Builds a Parameter object from the OpenAPI Parameter object.
     *
     * @param parameter The OpenAPI Parameter object
     * @return The internal Parameter representation
     */
    protected Parameter buildParameter(io.swagger.v3.oas.models.parameters.Parameter parameter) {
        if (parameter == null) {
            return null;
        }
        return Parameter.builder()
                .ref(parameter.get$ref())
                .name(parameter.getName())
                .in(parameter.getIn())
                .description(parameter.getDescription())
                .required(parameter.getRequired())
                .deprecated(parameter.getDeprecated())
                .allowEmptyValue(parameter.getAllowEmptyValue())
                .schema(buildSchema(parameter.getSchema()))
                .examples(toLinkMap(parameter.getExamples(), this::buildExample))
                .example(parameter.getExample())
                .content(toLinkMap(parameter.getContent(), this::buildMediaType))
                .extensions(copy(parameter.getExtensions()))
                .build();
    }

    /**
     * Builds a Schema object from the OpenAPI Schema object.
     *
     * @param schema The OpenAPI Schema object
     * @return The internal Schema representation
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Schema buildSchema(io.swagger.v3.oas.models.media.Schema schema) {
        if (schema == null) {
            return null;
        }
        return Schema.builder()
                .id(schema.get$id())
                .ref(schema.get$ref())
                .name(schema.getName())
                .title(schema.getTitle())
                .comment(schema.get$comment())
                .description(schema.getDescription())
                .externalDocs(buildExternalDoc(schema.getExternalDocs()))
                .type(schema.getType())
                .types(schema.getTypes())
                .format(schema.getFormat())
                .defaultValue(schema.getDefault())
                .properties(toLinkMap(schema.getProperties(), this::buildSchema))
                .patternProperties(toLinkMap(schema.getPatternProperties(), this::buildSchema))
                .additionalProperties(schema.getAdditionalProperties())
                .booleanSchemaValue(Version.getBooleanSchemaValue(schema)) // @since 2.2.2
                .items(buildSchema(schema.getItems()))
                .required(schema.getRequired() == null ? null : new ArrayList<>(schema.getRequired()))
                .nullable(schema.getNullable())
                .readOnly(schema.getReadOnly())
                .writeOnly(schema.getWriteOnly())
                .deprecated(schema.getDeprecated())
                .multipleOf(schema.getMultipleOf())
                .maximum(schema.getMaximum())
                .exclusiveMaximum(schema.getExclusiveMaximum())
                .exclusiveMaximumValue(schema.getExclusiveMaximumValue())
                .minimum(schema.getMinimum())
                .exclusiveMinimum(schema.getExclusiveMinimum())
                .exclusiveMinimumValue(schema.getExclusiveMinimumValue())
                .maxLength(schema.getMaxLength())
                .minLength(schema.getMinLength())
                .pattern(schema.getPattern())
                .maxItems(schema.getMaxItems())
                .minItems(schema.getMinItems())
                .uniqueItems(schema.getUniqueItems())
                .maxProperties(schema.getMaxProperties())
                .minProperties(schema.getMinProperties())
                .enums(schema.getEnum())
                .constValue(schema.getConst())
                .prefixItems(toList(schema.getPrefixItems(), this::buildSchema))
                .allOf(toList(schema.getAllOf(), this::buildSchema))
                .anyOf(toList(schema.getAnyOf(), this::buildSchema))
                .oneOf(toList(schema.getOneOf(), this::buildSchema))
                .not(buildSchema(schema.getNot()))
                .contains(buildSchema(schema.getContains()))
                .schema(schema.get$schema())
                .anchor(schema.get$anchor())
                .vocabulary(Version.getVocabulary(schema)) // @since 2.2.14
                .dynamicAnchor(Version.getDynamicAnchor(schema)) // @since 2.2.14
                .dynamicRef(Version.getDynamicRef(schema)) // @since 2.2.32
                .contentEncoding(schema.getContentEncoding())
                .contentMediaType(schema.getContentMediaType())
                .contentSchema(buildSchema(schema.getContentSchema()))
                .propertyNames(buildSchema(schema.getPropertyNames()))
                .unevaluatedProperties(buildSchema(schema.getUnevaluatedProperties()))
                .maxContains(schema.getMaxContains())
                .minContains(schema.getMinContains())
                .additionalItems(buildSchema(schema.getAdditionalItems()))
                .unevaluatedItems(buildSchema(schema.getUnevaluatedItems()))
                .ifCnd(buildSchema(schema.getIf()))
                .elseCnd(buildSchema(schema.getElse()))
                .thenCnd(buildSchema(schema.getThen()))
                .dependentSchemas(toLinkMap((Map<String, io.swagger.v3.oas.models.media.Schema>) schema.getDependentSchemas(), this::buildSchema))
                .dependentRequired(toLinkMap((Map<String, List<String>>) schema.getDependentRequired(), v -> copy(v)))
                .xml(buildXml(schema.getXml()))
                .discriminator(buildDiscriminator(schema.getDiscriminator()))
                .example(schema.getExample())
                .examples(copy(schema.getExamples()))
                .extensions(copy(schema.getExtensions()))
                .build();
    }

    /**
     * Builds a Discriminator object from OpenAPI discriminator model.
     *
     * @param discriminator the OpenAPI discriminator model
     * @return the built Discriminator object, or null if input is null
     */
    protected Discriminator buildDiscriminator(io.swagger.v3.oas.models.media.Discriminator discriminator) {
        if (discriminator == null) {
            return null;
        }
        return Discriminator.builder()
                .propertyName(discriminator.getPropertyName())
                .mapping(copy(discriminator.getMapping()))
                .extensions(copy(discriminator.getExtensions()))
                .build();
    }

    /**
     * Builds an XML object from OpenAPI XML model.
     *
     * @param xml the OpenAPI XML model
     * @return the built XML object, or null if input is null
     */
    protected XML buildXml(io.swagger.v3.oas.models.media.XML xml) {
        if (xml == null) {
            return null;
        }
        return XML.builder()
                .name(xml.getName())
                .namespace(xml.getNamespace())
                .prefix(xml.getPrefix())
                .attribute(xml.getAttribute())
                .wrapped(xml.getWrapped())
                .extensions(copy(xml.getExtensions()))
                .build();
    }

    /**
     * Builds an Operation object from the OpenAPI Operation object.
     *
     * @param operation The OpenAPI Operation object
     * @return The internal Operation representation
     */
    protected Operation buildOperation(io.swagger.v3.oas.models.Operation operation) {
        if (operation == null) {
            return null;
        }
        io.swagger.v3.oas.models.responses.ApiResponses responses = operation.getResponses();
        ApiResponses apiResponses = null;
        if (responses != null) {
            apiResponses = new ApiResponses(toLinkMap(responses, this::buildApiResponse), copy(responses.getExtensions()));
        }
        return Operation.builder()
                .operationId(operation.getOperationId())
                .summary(operation.getSummary())
                .description(operation.getDescription())
                .externalDocs(buildExternalDoc(operation.getExternalDocs()))
                .tags(toList(operation.getTags(), v -> v))
                .parameters(toList(operation.getParameters(), this::buildParameter))
                .requestBody(buildRequestBody(operation.getRequestBody()))
                .responses(apiResponses)
                .callbacks(toLinkMap(operation.getCallbacks(), this::buildCallback))
                .security(toList(operation.getSecurity(),
                        v -> new SecurityRequirement(toLinkMap(v, k -> new ArrayList<>(k)))))
                .deprecated(operation.getDeprecated())
                .servers(toList(operation.getServers(), this::buildServer))
                .extensions(copy(operation.getExtensions()))
                .build();
    }

    /**
     * Builds an ApiResponse object from the OpenAPI ApiResponse object.
     *
     * @param response The OpenAPI ApiResponse object
     * @return The internal ApiResponse representation
     */
    protected ApiResponse buildApiResponse(io.swagger.v3.oas.models.responses.ApiResponse response) {
        if (response == null) {
            return null;
        }
        return ApiResponse.builder()
                .ref(response.get$ref())
                .description(response.getDescription())
                .content(toLinkMap(response.getContent(), this::buildMediaType))
                .headers(toLinkMap(response.getHeaders(), this::buildHeader))
                .links(toLinkMap(response.getLinks(), this::buildLink))
                .extensions(copy(response.getExtensions()))
                .build();
    }

    /**
     * Builds a Header object from the OpenAPI Header object.
     *
     * @param header The OpenAPI Header object
     * @return The internal Header representation
     */
    protected Header buildHeader(io.swagger.v3.oas.models.headers.Header header) {
        if (header == null) {
            return null;
        }
        return Header.builder()
                .ref(header.get$ref())
                .description(header.getDescription())
                .required(header.getRequired())
                .deprecated(header.getDeprecated())
                .explode(header.getExplode())
                .style(header.getStyle() != null ? header.getStyle().toString() : null)
                .schema(buildSchema(header.getSchema()))
                .example(toLinkMap(header.getExamples(), this::buildExample))
                .example(header.getExample())
                .content(toLinkMap(header.getContent(), this::buildMediaType))
                .extensions(copy(header.getExtensions()))
                .build();
    }

    /**
     * Builds a Components object from the OpenAPI Components object.
     *
     * @param components The OpenAPI Components object
     * @return The internal Components representation
     */
    protected Components buildComponents(io.swagger.v3.oas.models.Components components) {
        if (components == null) {
            return null;
        }
        return Components.builder()
                .schemas(toLinkMap(components.getSchemas(), this::buildSchema))
                .parameters(toLinkMap(components.getParameters(), this::buildParameter))
                .requestBodies(toLinkMap(components.getRequestBodies(), this::buildRequestBody))
                .callbacks(toLinkMap(components.getCallbacks(), this::buildCallback))
                .pathItems(toLinkMap(components.getPathItems(), this::buildPathItem))
                .headers(toLinkMap(components.getHeaders(), this::buildHeader))
                .responses(toLinkMap(components.getResponses(), this::buildApiResponse))
                .callbacks(toLinkMap(components.getCallbacks(), this::buildCallback))
                .examples(toLinkMap(components.getExamples(), this::buildExample))
                .links(toLinkMap(components.getLinks(), this::buildLink))
                .securitySchemes(toLinkMap(components.getSecuritySchemes(), this::buildSecurityScheme))
                .extensions(copy(components.getExtensions()))
                .build();
    }

    /**
     * Builds a SecurityScheme object from the OpenAPI SecurityScheme object.
     *
     * @param securityScheme The OpenAPI SecurityScheme object
     * @return The internal SecurityScheme representation
     */
    protected SecurityScheme buildSecurityScheme(io.swagger.v3.oas.models.security.SecurityScheme securityScheme) {
        if (securityScheme == null) {
            return null;
        }
        return SecurityScheme.builder()
                .ref(securityScheme.get$ref())
                .description(securityScheme.getDescription())
                .type(securityScheme.getType() == null ? null : securityScheme.getType().toString())
                .name(securityScheme.getName())
                .in(securityScheme.getIn() == null ? null : securityScheme.getIn().toString())
                .scheme(securityScheme.getScheme())
                .bearerFormat(securityScheme.getBearerFormat())
                .flows(buildOAuthFlows(securityScheme.getFlows()))
                .openIdConnectUrl(securityScheme.getOpenIdConnectUrl())
                .extensions(copy(securityScheme.getExtensions()))
                .build();
    }

    /**
     * Builds an OAuthFlows object from the OpenAPI OAuthFlows object.
     *
     * @param oAuthFlows The OpenAPI OAuthFlows object
     * @return The internal OAuthFlows representation
     */
    protected OAuthFlows buildOAuthFlows(io.swagger.v3.oas.models.security.OAuthFlows oAuthFlows) {
        if (oAuthFlows == null) {
            return null;
        }
        return OAuthFlows.builder()
                .implicit(buildOAuthFlow(oAuthFlows.getImplicit()))
                .password(buildOAuthFlow(oAuthFlows.getPassword()))
                .clientCredentials(buildOAuthFlow(oAuthFlows.getClientCredentials()))
                .authorizationCode(buildOAuthFlow(oAuthFlows.getAuthorizationCode()))
                .extensions(copy(oAuthFlows.getExtensions()))
                .build();
    }

    /**
     * Builds an OAuthFlow object from the OpenAPI OAuthFlow object.
     *
     * @param oauthFlow The OpenAPI OAuthFlow object
     * @return The internal OAuthFlow representation
     */
    protected OAuthFlow buildOAuthFlow(io.swagger.v3.oas.models.security.OAuthFlow oauthFlow) {
        if (oauthFlow == null) {
            return null;
        }
        return OAuthFlow.builder()
                .authorizationUrl(oauthFlow.getAuthorizationUrl())
                .tokenUrl(oauthFlow.getTokenUrl())
                .refreshUrl(oauthFlow.getRefreshUrl())
                .scopes(oauthFlow.getScopes() == null ? null : new HashMap<>(oauthFlow.getScopes()))
                .extensions(copy(oauthFlow.getExtensions()))
                .build();
    }

    /**
     * Builds a MediaType object from the OpenAPI MediaType object.
     *
     * @param mediaType The OpenAPI MediaType object
     * @return The internal MediaType representation
     */
    protected MediaType buildMediaType(io.swagger.v3.oas.models.media.MediaType mediaType) {
        if (mediaType == null) {
            return null;
        }
        return MediaType.builder()
                .schema(buildSchema(mediaType.getSchema()))
                .encoding(toLinkMap(mediaType.getEncoding(), this::buildEncoding))
                .examples(toLinkMap(mediaType.getExamples(), this::buildExample))
                .example(mediaType.getExample())
                .exampleSetFlag(mediaType.getExampleSetFlag())
                .extensions(copy(mediaType.getExtensions()))
                .build();
    }

    /**
     * Builds an Encoding object from the OpenAPI Encoding object.
     *
     * @param encoding The OpenAPI Encoding object
     * @return The internal Encoding representation
     */
    protected Encoding buildEncoding(io.swagger.v3.oas.models.media.Encoding encoding) {
        if (encoding == null) {
            return null;
        }
        return Encoding.builder()
                .contentType(encoding.getContentType())
                .headers(toLinkMap(encoding.getHeaders(), this::buildHeader))
                .style(encoding.getStyle() == null ? null : encoding.getStyle().name())
                .explode(encoding.getExplode())
                .allowReserved(encoding.getAllowReserved())
                .extensions(copy(encoding.getExtensions()))
                .build();
    }

    /**
     * Builds an Example object from the OpenAPI Example object.
     *
     * @param example The OpenAPI Example object
     * @return The internal Example representation
     */
    protected Example buildExample(io.swagger.v3.oas.models.examples.Example example) {
        if (example == null) {
            return null;
        }
        return Example.builder()
                .ref(example.get$ref())
                .summary(example.getSummary())
                .description(example.getDescription())
                .value(example.getValue())
                .externalValue(example.getExternalValue())
                .valueSetFlag(example.getValueSetFlag())
                .extensions(copy(example.getExtensions()))
                .build();
    }

    /**
     * Builds a RequestBody object from the OpenAPI RequestBody object.
     *
     * @param requestBody The OpenAPI RequestBody object
     * @return The internal RequestBody representation
     */
    protected RequestBody buildRequestBody(io.swagger.v3.oas.models.parameters.RequestBody requestBody) {
        if (requestBody == null) {
            return null;
        }
        return RequestBody.builder()
                .ref(requestBody.get$ref())
                .description(requestBody.getDescription())
                .content(toLinkMap(requestBody.getContent(), this::buildMediaType))
                .required(requestBody.getRequired())
                .extensions(copy(requestBody.getExtensions()))
                .build();
    }

    /**
     * Builds a Callback object from the OpenAPI Callback object.
     *
     * @param callback The OpenAPI Callback object
     * @return The internal Callback representation
     */
    protected Callback buildCallback(io.swagger.v3.oas.models.callbacks.Callback callback) {
        if (callback == null) {
            return null;
        }
        return Callback.builder()
                .ref(callback.get$ref())
                .m(toLinkMap(callback, this::buildPathItem))
                .extensions(copy(callback.getExtensions()))
                .build();
    }

    /**
     * Builds a Link object from the OpenAPI Link object.
     *
     * @param link The OpenAPI Link object
     * @return The internal Link representation
     */
    @SuppressWarnings("deprecation")
    protected Link buildLink(io.swagger.v3.oas.models.links.Link link) {
        if (link == null) {
            return null;
        }
        return Link.builder()
                .ref(link.get$ref())
                .description(link.getDescription())
                .operationRef(link.getOperationRef())
                .operationId(link.getOperationId())
                .parameters(link.getParameters() == null ? null : new HashMap<>(link.getParameters()))
                .requestBody(link.getRequestBody())
                .headers(toLinkMap(link.getHeaders(), this::buildHeader))
                .server(buildServer(link.getServer()))
                .extensions(copy(link.getExtensions()))
                .build();
    }

    protected static class Version {

        private static final ClassLoader CLASS_LOADER = io.swagger.v3.oas.models.media.Schema.class.getClassLoader();

        private static final Class<?> CLASS_SCHEMA = loadClass("io.swagger.v3.oas.models.media.Schema", CLASS_LOADER);

        private static final FieldAccessor ACCESSOR_VOCABULARY = getAccessor(CLASS_SCHEMA, "$vocabulary");

        private static final FieldAccessor ACCESSOR_DYNAMIC_ANCHOR = getAccessor(CLASS_SCHEMA, "$dynamicAnchor");

        private static final FieldAccessor ACCESSOR_DYNAMIC_REF = getAccessor(CLASS_SCHEMA, "$dynamicRef");

        private static final FieldAccessor ACCESSOR_BOOLEAN_SCHEMA_VALUE = getAccessor(CLASS_SCHEMA, "booleanSchemaValue");

        public static String getVocabulary(io.swagger.v3.oas.models.media.Schema schema) {
            // @since 2.2.14
            if (ACCESSOR_VOCABULARY != null) {
                return schema.get$vocabulary();
            }
            return null;
        }

        public static String getDynamicAnchor(io.swagger.v3.oas.models.media.Schema schema) {
            // @since 2.2.14
            if (ACCESSOR_DYNAMIC_ANCHOR != null) {
                return schema.get$dynamicAnchor();
            }
            return null;
        }

        public static String getDynamicRef(io.swagger.v3.oas.models.media.Schema schema) {
            // @since 2.2.32
            if (ACCESSOR_DYNAMIC_REF != null) {
                return schema.get$dynamicRef();
            }
            return null;
        }

        public static Boolean getBooleanSchemaValue(io.swagger.v3.oas.models.media.Schema schema) {
            // @since 2.2.2
            if (ACCESSOR_BOOLEAN_SCHEMA_VALUE != null) {
                return schema.getBooleanSchemaValue();
            }
            return null;
        }

    }
}