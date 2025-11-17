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
package com.jd.live.agent.plugin.application.springboot.v2.openapi.v2;

import com.jd.live.agent.core.util.StringUtils;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.openapi.*;
import com.jd.live.agent.governance.openapi.Operation;
import com.jd.live.agent.governance.openapi.examples.Example;
import com.jd.live.agent.governance.openapi.headers.Header;
import com.jd.live.agent.governance.openapi.info.Contact;
import com.jd.live.agent.governance.openapi.info.Info;
import com.jd.live.agent.governance.openapi.info.License;
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
import com.jd.live.agent.governance.openapi.tags.Tag;
import com.jd.live.agent.plugin.application.springboot.v2.openapi.OpenApiFactory;
import io.swagger.models.*;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.BasicAuthDefinition;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.utils.PropertyModelConverter;

import java.util.*;

import static com.alibaba.nacos.api.utils.StringUtils.isEmpty;
import static com.jd.live.agent.core.util.CollectionUtils.toLinkMap;
import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * Factory implementation for creating OpenApi objects from Swagger 2 specifications.
 */
public class OpenApi2Factory implements OpenApiFactory {

    private final PropertyModelConverter converter = new PropertyModelConverter();

    /**
     * Creates an OpenApi object from the provided API specification.
     *
     * @param api The API specification object (expected to be a Swagger instance)
     * @return An OpenApi object if the input is a Swagger instance, null otherwise
     */
    @Override
    public OpenApi create(Object api) {
        if (api instanceof Swagger) {
            build((Swagger) api);
        }
        return null;
    }

    /**
     * Builds an OpenApi object from a Swagger 2 specification.
     *
     * @param swagger The Swagger 2 specification
     * @return The resulting OpenApi object
     */
    protected OpenApi build(Swagger swagger) {
        ContentTypes defaults = new ContentTypes(swagger.getConsumes(), swagger.getProduces());
        return OpenApi.builder()
                .openapi(swagger.getSwagger())
                .info(buildInfo(swagger.getInfo()))
                .externalDocs(buildExternalDoc(swagger.getExternalDocs()))
                .servers(buildServer(swagger))
                .tags(toList(swagger.getTags(), this::buildTag))
                .paths(toLinkMap(swagger.getPaths(), path -> buildPathItem(path, defaults)))
                .components(buildComponents(swagger, defaults))
                .security(toList(swagger.getSecurity(), item -> toLinkMap(item.getRequirements(), v -> new ArrayList<>(v))))
                .extensions(swagger.getVendorExtensions() == null ? null : new HashMap<>(swagger.getVendorExtensions()))
                .build();
    }

    /**
     * Builds an Info object from the OpenAPI Info object.
     *
     * @param info The OpenAPI Info object
     * @return The internal Info representation
     */
    protected Info buildInfo(io.swagger.models.Info info) {
        if (info == null) {
            return null;
        }
        return Info.builder()
                .title(info.getTitle())
                .description(info.getDescription())
                .termsOfService(info.getTermsOfService())
                .version(info.getVersion())
                .contact(buildContact(info.getContact()))
                .license(buildLicense(info.getLicense()))
                .extensions(info.getVendorExtensions() == null ? null : new HashMap<>(info.getVendorExtensions()))
                .build();
    }

    /**
     * Builds a Contact object from the OpenAPI Contact object.
     *
     * @param contact The OpenAPI Contact object
     * @return The internal Contact representation
     */
    protected Contact buildContact(io.swagger.models.Contact contact) {
        if (contact == null) {
            return null;
        }
        return Contact.builder()
                .name(contact.getName())
                .url(contact.getUrl())
                .email(contact.getEmail())
                .extensions(contact.getVendorExtensions() == null ? null : new HashMap<>(contact.getVendorExtensions()))
                .build();
    }

    /**
     * Builds a License object from the OpenAPI License object.
     *
     * @param license The OpenAPI License object
     * @return The internal License representation
     */
    protected License buildLicense(io.swagger.models.License license) {
        if (license == null) {
            return null;
        }
        return License.builder()
                .name(license.getName())
                .url(license.getUrl())
                .extensions(license.getVendorExtensions() == null ? null : new HashMap<>(license.getVendorExtensions()))
                .build();
    }

    /**
     * Builds an ExternalDocumentation object from the OpenAPI ExternalDocumentation object.
     *
     * @param doc The OpenAPI ExternalDocumentation object
     * @return The internal ExternalDocumentation representation
     */
    protected ExternalDocumentation buildExternalDoc(io.swagger.models.ExternalDocs doc) {
        if (doc == null) {
            return null;
        }
        return ExternalDocumentation.builder()
                .description(doc.getDescription())
                .url(doc.getUrl())
                .extensions(doc.getVendorExtensions() == null ? null : new HashMap<>(doc.getVendorExtensions()))
                .build();
    }

    /**
     * Converts Swagger 2.0 host, basePath, and schemes to OpenAPI 3.0+ Server objects.
     *
     * @param swagger The Swagger 2.0 object containing host, basePath, and schemes
     * @return A list of Server objects for OpenAPI 3.0+
     */
    protected List<Server> buildServer(io.swagger.models.Swagger swagger) {

        String host = swagger.getHost();
        String basePath = swagger.getBasePath();
        if (isEmpty(host)) {
            return null;
        }

        List<Server> servers = new ArrayList<>();
        Set<String> schemes = new LinkedHashSet<>();
        if (swagger.getSchemes() != null) {
            swagger.getSchemes().forEach(scheme -> schemes.add(scheme.name()));
        }

        URI uri = URI.parse(host);
        if (isEmpty(host)) {
            uri = uri.host("localhost");
        }
        if (!isEmpty(basePath)) {
            uri = uri.path(basePath);
        }
        if (!isEmpty(uri.getScheme())) {
            schemes.add(uri.getScheme());
        }
        for (String scheme : schemes) {
            Server server = new Server();
            server.setUrl(uri.scheme(scheme).toString());
            servers.add(server);
        }
        return servers;
    }

    /**
     * Builds a Tag object from the OpenAPI Tag object.
     *
     * @param tag The OpenAPI Tag object
     * @return The internal Tag representation
     */
    protected Tag buildTag(io.swagger.models.Tag tag) {
        if (tag == null) {
            return null;
        }
        return Tag.builder()
                .name(tag.getName())
                .description(tag.getDescription())
                .externalDocs(buildExternalDoc(tag.getExternalDocs()))
                .extensions(tag.getVendorExtensions() == null ? null : new HashMap<>(tag.getVendorExtensions()))
                .build();
    }

    /**
     * Builds a PathItem object from the OpenAPI PathItem object.
     *
     * @param pathItem The OpenAPI PathItem object to convert
     * @param defaults Default content types to use when not specified at operation level
     * @return The internal PathItem representation
     */
    protected PathItem buildPathItem(io.swagger.models.Path pathItem, ContentTypes defaults) {
        if (pathItem == null) {
            return null;
        } else if (pathItem instanceof RefPath) {
            RefPath refPath = (RefPath) pathItem;
            return PathItem.builder().ref(refPath.get$ref()).build();
        }
        return PathItem.builder()
                .parameters(toList(pathItem.getParameters(), this::buildParameter))
                .operations(toLinkMap(pathItem.getOperationMap(), m -> m.name().toLowerCase(), v -> buildOperation(v, defaults)))
                .extensions(pathItem.getVendorExtensions() == null ? null : new HashMap<>(pathItem.getVendorExtensions()))
                .build();
    }

    /**
     * Builds a Parameter object from the OpenAPI Parameter object.
     *
     * @param parameter The OpenAPI Parameter object
     * @return The internal Parameter representation
     */
    protected Parameter buildParameter(io.swagger.models.parameters.Parameter parameter) {
        if (parameter == null) {
            return null;
        }

        Schema schema = null;
        if (parameter instanceof SerializableParameter) {
            schema = buildSchema((SerializableParameter) parameter);
        }

        return Parameter.builder()
                .name(parameter.getName())
                .in(parameter.getIn())
                .description(parameter.getDescription())
                .required(parameter.getRequired())
                .schema(schema)
                .extensions(parameter.getVendorExtensions() == null ? null : new HashMap<>(parameter.getVendorExtensions()))
                .build();
    }

    /**
     * Builds an Operation object from the OpenAPI Operation object.
     *
     * @param operation The OpenAPI Operation object to convert
     * @param defaults  Default content types to use when not specified at operation level
     * @return The internal Operation representation
     */
    protected Operation buildOperation(io.swagger.models.Operation operation, ContentTypes defaults) {
        if (operation == null) {
            return null;
        }
        ContentTypes types = defaults.of(operation.getConsumes(), operation.getProduces());
        Map<String, io.swagger.models.Response> map = operation.getResponses() == null ? operation.getResponsesObject() : operation.getResponses();
        ParameterList parameters = new ParameterList(operation.getParameters());
        return Operation.builder()
                .operationId(operation.getOperationId())
                .summary(operation.getSummary())
                .description(operation.getDescription())
                .externalDocs(buildExternalDoc(operation.getExternalDocs()))
                .tags(toList(operation.getTags(), v -> v))
                .parameters(toList(parameters.getParameters(), this::buildParameter))
                //.requestBody(buildRequestBody(operation.getRequestBody()))
                .responses(toLinkMap(map, v -> buildApiResponse(v, types)))
                .security(toList(operation.getSecurity(),
                        v -> new SecurityRequirement(toLinkMap(v, k -> new ArrayList(k)))))
                .deprecated(operation.isDeprecated())
                .extensions(operation.getVendorExtensions() == null ? null : new HashMap<>(operation.getVendorExtensions()))
                .build();
    }

    /**
     * Builds an ApiResponse object from the OpenAPI ApiResponse object.
     *
     * @param response The OpenAPI ApiResponse object
     * @return The internal ApiResponse representation
     */
    protected ApiResponse buildApiResponse(io.swagger.models.Response response, ContentTypes types) {
        if (response == null) {
            return null;
        }
        if (response instanceof RefResponse) {
            RefResponse ref = (RefResponse) response;
            return ApiResponse.builder().ref(ref.get$ref()).build();
        }
        Schema schema = buildSchema(response.getResponseSchema());
        List<String> produces = types.getProduces();
        return ApiResponse.builder()
                .description(response.getDescription())
                .content(toLinkMap(produces, v -> v, v -> {
                    Object value = response.getExamples().get(v);
                    return MediaType.builder()
                            .schema(schema)
                            .example(value == null ? null : Example.builder().value(value).build())
                            .exampleSetFlag(value != null)
                            .build();
                }))
                .headers(toLinkMap(response.getHeaders(), this::buildHeader))
                .extensions(response.getVendorExtensions() == null ? null : new HashMap<>(response.getVendorExtensions()))
                .build();
    }

    /**
     * Builds a Header object from the OpenAPI Header object.
     *
     * @param parameter The OpenAPI Header object
     * @return The internal Header representation
     */
    protected Header buildHeader(io.swagger.models.parameters.HeaderParameter parameter) {
        if (parameter == null) {
            return null;
        }
        return Header.builder()
                //.ref(parameter.get$ref())
                .description(parameter.getDescription())
                .required(parameter.getRequired())
                //.deprecated(parameter.getDeprecated())
                //.explode(parameter.getExplode())
                //.style(parameter.getStyle() != null ? parameter.getStyle().name() : null)
                //.schema(buildSchema(parameter.getSchema()))
                //.example(toLinkMap(parameter.getExamples(), this::buildExample))
                .example(parameter.getExample())
                //.content(toLinkMap(parameter.getContent(), this::buildMediaType))
                .extensions(parameter.getVendorExtensions() == null ? null : new HashMap<>(parameter.getVendorExtensions()))
                .build();
    }

    /**
     * Builds a Header object from the OpenAPI Header object.
     *
     * @param property The OpenAPI Header object
     * @return The internal Header representation
     */
    protected Header buildHeader(io.swagger.models.properties.Property property) {
        if (property == null) {
            return null;
        }
        if (property instanceof RefProperty) {
            return Header.builder().ref(((RefProperty) property).get$ref()).build();
        }
        return Header.builder()
                .description(property.getDescription())
                .required(property.getRequired())
                //.style(property.getStyle() != null ? property.getStyle().name() : null)
                //.schema(buildSchema(property.getSchema()))
                //.example(toLinkMap(property.getExamples(), this::buildExample))
                .example(property.getExample())
                //.content(toLinkMap(property.getContent(), this::buildMediaType))
                .extensions(property.getVendorExtensions() == null ? null : new HashMap<>(property.getVendorExtensions()))
                .build();
    }

    /**
     * Converts Swagger model to internal Components representation.
     *
     * @param swagger  The Swagger specification object
     * @param defaults Default content types to use when not specified
     * @return Structured Components object with all API elements
     */
    protected Components buildComponents(Swagger swagger, ContentTypes defaults) {
        ParameterComponents params = new ParameterComponents(swagger.getParameters());
        return Components.builder()
                .schemas(toLinkMap(swagger.getDefinitions(), this::buildSchema))
                .parameters(toLinkMap(params.getParameters(), this::buildParameter))
                .requestBodies(toLinkMap(params.getBodies(), v -> buildRequestBody(v, defaults)))
                .headers(toLinkMap(params.getHeaders(), this::buildHeader))
                .pathItems(toLinkMap(swagger.getPaths(), v -> buildPathItem(v, defaults)))
                .responses(toLinkMap(swagger.getResponses(), v -> buildApiResponse(v, defaults)))
                .securitySchemes(toLinkMap(swagger.getSecurityDefinitions(), this::buildSecurityScheme))
                .extensions(swagger.getVendorExtensions() == null ? null : new HashMap<>(swagger.getVendorExtensions()))
                .build();
    }

    /**
     * Builds a SecurityScheme object from the OpenAPI SecurityScheme object.
     *
     * @param definition The OpenAPI SecurityScheme object
     * @return The internal SecurityScheme representation
     */
    protected SecurityScheme buildSecurityScheme(io.swagger.models.auth.SecuritySchemeDefinition definition) {
        if (definition == null) {
            return null;
        } else if (definition instanceof ApiKeyAuthDefinition) {
            return buildSecurityScheme((ApiKeyAuthDefinition) definition);
        } else if (definition instanceof BasicAuthDefinition) {
            return buildSecurityScheme((BasicAuthDefinition) definition);
        } else if (definition instanceof OAuth2Definition) {
            return buildSecurityScheme((OAuth2Definition) definition);
        }
        return null;
    }

    protected SecurityScheme buildSecurityScheme(io.swagger.models.auth.ApiKeyAuthDefinition definition) {
        if (definition == null) {
            return null;
        }
        return SecurityScheme.builder()
                .description(definition.getDescription())
                .type(definition.getType())
                .name(definition.getName())
                .in(definition.getIn() == null ? null : definition.getIn().toValue())
                .extensions(definition.getVendorExtensions() == null ? null : new HashMap<>(definition.getVendorExtensions()))
                .build();
    }

    protected SecurityScheme buildSecurityScheme(io.swagger.models.auth.BasicAuthDefinition definition) {
        if (definition == null) {
            return null;
        }
        return SecurityScheme.builder()
                .description(definition.getDescription())
                .type(definition.getType())
                .scheme("basic")
                .extensions(definition.getVendorExtensions() == null ? null : new HashMap<>(definition.getVendorExtensions()))
                .build();
    }

    protected SecurityScheme buildSecurityScheme(io.swagger.models.auth.OAuth2Definition definition) {
        if (definition == null) {
            return null;
        }
        Map<String, Object> extensions = definition.getVendorExtensions() == null ? null : new HashMap<>(definition.getVendorExtensions());
        String bearFormat = extensions == null || extensions.isEmpty() ? null : StringUtils.choose(
                (String) extensions.get("x-bearer-type"),
                (String) extensions.get("x-jwt"),
                (String) extensions.get("x-token-format")
        );
        bearFormat = isEmpty(bearFormat) ? "JWT" : bearFormat;

        return SecurityScheme.builder()
                .description(definition.getDescription())
                .type(definition.getType())
                .bearerFormat(bearFormat)
                .flows(buildOAuthFlows(definition))
                .extensions(extensions)
                .build();
    }

    /**
     * Builds a RequestBody object from the OpenAPI RequestBody object.
     *
     * @param parameter The OpenAPI RequestBody object
     * @return The internal RequestBody representation
     */
    protected RequestBody buildRequestBody(io.swagger.models.parameters.BodyParameter parameter, ContentTypes defaults) {
        if (parameter == null) {
            return null;
        }

        Schema schema = buildSchema(parameter.getSchema());
        if (schema != null && !isEmpty(schema.getRef())) {
            return RequestBody.builder().ref(schema.getRef()).build();
        }

        return RequestBody.builder()
                .description(parameter.getDescription())
                .required(parameter.getRequired())
                .extensions(parameter.getVendorExtensions() == null ? null : new HashMap<>(parameter.getVendorExtensions()))
                .build();

    }

    protected Schema buildSchema(io.swagger.models.parameters.SerializableParameter parameter) {
        Schema schema = new Schema();
        schema.setType(parameter.getType());
        schema.setFormat(parameter.getFormat());
        schema.setItems(buildSchema(converter.propertyToModel(parameter.getItems())));
        return schema;
    }

    protected Schema buildSchema(Model model) {
        if (model == null) {
            return null;
        }

        Schema schema = new Schema();
        // Handle reference models
        if (model instanceof RefModel) {
            RefModel refModel = (RefModel) model;
            String ref = refModel.get$ref();
            // Convert from #/definitions/Model to #/components/schemas/Model
            if (ref.startsWith("#/definitions/")) {
                // TODO schemas/header/....
                ref = ref.replace("#/definitions/", "#/components/schemas/");
            }
            schema.setRef(ref);
            return schema;
        }
        // TODO other fields
        return schema;
    }

    protected OAuthFlows buildOAuthFlows(OAuth2Definition definition) {
        OAuthFlows flows = null;
        String flow = definition.getFlow();
        if ("implicit".equals(flow)) {
            flows = OAuthFlows.builder()
                    .implicit(OAuthFlow.builder()
                            .authorizationUrl(definition.getAuthorizationUrl())
                            .scopes(definition.getScopes() == null ? null : new HashMap<>(definition.getScopes()))
                            .build())
                    .build();
        } else if ("password".equals(flow)) {
            flows = OAuthFlows.builder()
                    .password(OAuthFlow.builder()
                            .tokenUrl(definition.getTokenUrl())
                            .scopes(definition.getScopes() == null ? null : new HashMap<>(definition.getScopes()))
                            .build())
                    .build();
        } else if ("application".equals(flow)) {
            flows = OAuthFlows.builder()
                    .clientCredentials(OAuthFlow.builder()
                            .tokenUrl(definition.getTokenUrl())
                            .scopes(definition.getScopes() == null ? null : new HashMap<>(definition.getScopes()))
                            .build())
                    .build();
        } else if ("accessCode".equals(flow)) {
            flows = OAuthFlows.builder()
                    .authorizationCode(OAuthFlow.builder()
                            .authorizationUrl(definition.getAuthorizationUrl())
                            .tokenUrl(definition.getTokenUrl())
                            .scopes(definition.getScopes() == null ? null : new HashMap<>(definition.getScopes()))
                            .build())
                    .build();
        }
        return flows;
    }
}
