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
package com.jd.live.agent.plugin.application.springboot.v2.openapi.v31;

import com.jd.live.agent.governance.openapi.*;
import com.jd.live.agent.governance.openapi.callbacks.Callback;
import com.jd.live.agent.governance.openapi.examples.Example;
import com.jd.live.agent.governance.openapi.headers.Header;
import com.jd.live.agent.governance.openapi.info.Contact;
import com.jd.live.agent.governance.openapi.info.Info;
import com.jd.live.agent.governance.openapi.info.License;
import com.jd.live.agent.governance.openapi.links.Link;
import com.jd.live.agent.governance.openapi.media.Encoding;
import com.jd.live.agent.governance.openapi.media.MediaType;
import com.jd.live.agent.governance.openapi.media.Schema;
import com.jd.live.agent.governance.openapi.parameters.Parameter;
import com.jd.live.agent.governance.openapi.parameters.RequestBody;
import com.jd.live.agent.governance.openapi.responses.ApiResponse;
import com.jd.live.agent.governance.openapi.security.OAuthFlow;
import com.jd.live.agent.governance.openapi.security.OAuthFlows;
import com.jd.live.agent.governance.openapi.security.SecurityRequirement;
import com.jd.live.agent.governance.openapi.security.SecurityScheme;
import com.jd.live.agent.governance.openapi.servers.Server;
import com.jd.live.agent.governance.openapi.servers.ServerVariable;
import com.jd.live.agent.governance.openapi.tags.Tag;
import com.jd.live.agent.plugin.application.springboot.v2.openapi.OpenApiFactory;

import java.util.ArrayList;
import java.util.HashMap;

import static com.jd.live.agent.core.util.CollectionUtils.toLinkMap;
import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * Factory implementation for creating OpenApi objects from OpenAPI 3.1 specifications.
 * <p>
 * This class converts Swagger/OpenAPI 3.1 model objects to the internal OpenApi representation
 * used by the application. It handles all aspects of the OpenAPI specification including paths,
 * operations, schemas, security definitions, and more.
 */
public class OpenApi31Factory implements OpenApiFactory {

    /**
     * Creates an OpenApi object from the provided API specification.
     * <p>
     * This method checks if the input is an OpenAPI 3.1 specification and converts it
     * to the internal OpenApi model if applicable.
     *
     * @param api The API specification object (expected to be an OpenAPI instance)
     * @return An OpenApi object if the input is an OpenAPI instance, null otherwise
     */
    @Override
    public OpenApi create(Object api) {
        if (api instanceof io.swagger.v3.oas.models.OpenAPI) {
            return build((io.swagger.v3.oas.models.OpenAPI) api);
        }
        return null;
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
                .extensions(openApi.getExtensions() == null ? null : new HashMap<>(openApi.getExtensions()))
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
                .extensions(info.getExtensions() == null ? null : new HashMap<>(info.getExtensions()))
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
                .extensions(contact.getExtensions() == null ? null : new HashMap<>(contact.getExtensions()))
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
                .extensions(license.getExtensions() == null ? null : new HashMap<>(license.getExtensions()))
                .build();
    }

    /**
     * Builds an ExternalDocumentation object from the OpenAPI ExternalDocumentation object.
     *
     * @param doc The OpenAPI ExternalDocumentation object
     * @return The internal ExternalDocumentation representation
     */
    protected ExternalDocumentation buildExternalDoc(io.swagger.v3.oas.models.ExternalDocumentation doc) {
        if (doc == null) {
            return null;
        }
        return ExternalDocumentation.builder()
                .description(doc.getDescription())
                .url(doc.getUrl())
                .extensions(doc.getExtensions() == null ? null : new HashMap<>(doc.getExtensions()))
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
                .extensions(server.getExtensions() == null ? null : new HashMap<>(server.getExtensions()))
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
                .extensions(tag.getExtensions() == null ? null : new HashMap<>(tag.getExtensions()))
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
                .extensions(variable.getExtensions() == null ? null : new HashMap<>(variable.getExtensions()))
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
                .operations(toLinkMap(pathItem.readOperationsMap(), m -> m.name().toLowerCase(), this::buildOperation))
                .extensions(pathItem.getExtensions() == null ? null : new HashMap<>(pathItem.getExtensions()))
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
                .extensions(parameter.getExtensions() == null ? null : new HashMap<>(parameter.getExtensions()))
                .build();
    }

    /**
     * Builds a Schema object from the OpenAPI Schema object.
     *
     * @param schema The OpenAPI Schema object
     * @return The internal Schema representation
     */
    protected Schema buildSchema(io.swagger.v3.oas.models.media.Schema schema) {
        if (schema == null) {
            return null;
        }
        // TODO other fields
        return Schema.builder()
                .name(schema.getName())
                .title(schema.getTitle())
                .description(schema.getDescription())
                .type(schema.getType())
                .format(schema.getFormat())
                .defaultValue(schema.getDefault())
                .properties(toLinkMap(schema.getProperties(), this::buildSchema))
                .items(buildSchema(schema.getItems()))
                .ref(schema.get$ref())
                .required(schema.getRequired() == null ? null : new ArrayList<>(schema.getRequired()))
                .nullable(schema.getNullable())
                .readOnly(schema.getReadOnly())
                .writeOnly(schema.getWriteOnly())
                .description(schema.getDescription())
                .additionalProperties(schema.getAdditionalProperties())
                .externalDocs(buildExternalDoc(schema.getExternalDocs()))
                .extensions(schema.getExtensions() == null ? null : new HashMap<>(schema.getExtensions()))
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
        return Operation.builder()
                .operationId(operation.getOperationId())
                .summary(operation.getSummary())
                .description(operation.getDescription())
                .externalDocs(buildExternalDoc(operation.getExternalDocs()))
                .tags(toList(operation.getTags(), v -> v))
                .parameters(toList(operation.getParameters(), this::buildParameter))
                .requestBody(buildRequestBody(operation.getRequestBody()))
                .responses(toLinkMap(operation.getResponses(), this::buildApiResponse))
                .callbacks(toLinkMap(operation.getCallbacks(), this::buildCallback))
                .security(toList(operation.getSecurity(),
                        v -> new SecurityRequirement(toLinkMap(v, k -> new ArrayList(k)))))
                .deprecated(operation.getDeprecated())
                .servers(toList(operation.getServers(), this::buildServer))
                .extensions(operation.getExtensions() == null ? null : new HashMap<>(operation.getExtensions()))
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
                .extensions(response.getExtensions() == null ? null : new HashMap<>(response.getExtensions()))
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
                .extensions(header.getExtensions() == null ? null : new HashMap<>(header.getExtensions()))
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
                .extensions(components.getExtensions() == null ? null : new HashMap<>(components.getExtensions()))
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
                .extensions(securityScheme.getExtensions() == null ? null : new HashMap<>(securityScheme.getExtensions()))
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
                .extensions(oAuthFlows.getExtensions() == null ? null : new HashMap<>(oAuthFlows.getExtensions()))
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
                .extensions(oauthFlow.getExtensions() == null ? null : new HashMap<>(oauthFlow.getExtensions()))
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
                .extensions(mediaType.getExtensions() == null ? null : new HashMap<>(mediaType.getExtensions()))
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
                .extensions(encoding.getExtensions() == null ? null : new HashMap<>(encoding.getExtensions()))
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
                .extensions(example.getExtensions() == null ? null : new HashMap<>(example.getExtensions()))
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
                .extensions(requestBody.getExtensions() == null ? null : new HashMap<>(requestBody.getExtensions()))
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
                .extensions(callback.getExtensions() == null ? null : new HashMap<>(callback.getExtensions()))
                .build();
    }

    /**
     * Builds a Link object from the OpenAPI Link object.
     *
     * @param link The OpenAPI Link object
     * @return The internal Link representation
     */
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
                .extensions(link.getExtensions() == null ? null : new HashMap<>(link.getExtensions()))
                .build();
    }
}