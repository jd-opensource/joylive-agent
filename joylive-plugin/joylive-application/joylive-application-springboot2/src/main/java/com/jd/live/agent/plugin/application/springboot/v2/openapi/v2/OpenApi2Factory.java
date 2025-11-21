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

import com.jd.live.agent.core.openapi.spec.v3.*;
import com.jd.live.agent.core.openapi.spec.v3.Operation;
import com.jd.live.agent.core.openapi.spec.v3.headers.Header;
import com.jd.live.agent.core.openapi.spec.v3.info.Contact;
import com.jd.live.agent.core.openapi.spec.v3.info.Info;
import com.jd.live.agent.core.openapi.spec.v3.info.License;
import com.jd.live.agent.core.openapi.spec.v3.media.Discriminator;
import com.jd.live.agent.core.openapi.spec.v3.media.MediaType;
import com.jd.live.agent.core.openapi.spec.v3.media.Schema;
import com.jd.live.agent.core.openapi.spec.v3.parameters.Parameter;
import com.jd.live.agent.core.openapi.spec.v3.parameters.RequestBody;
import com.jd.live.agent.core.openapi.spec.v3.responses.ApiResponse;
import com.jd.live.agent.core.openapi.spec.v3.responses.ApiResponses;
import com.jd.live.agent.core.openapi.spec.v3.security.OAuthFlow;
import com.jd.live.agent.core.openapi.spec.v3.security.OAuthFlows;
import com.jd.live.agent.core.openapi.spec.v3.security.SecurityRequirement;
import com.jd.live.agent.core.openapi.spec.v3.security.SecurityScheme;
import com.jd.live.agent.core.openapi.spec.v3.servers.Server;
import com.jd.live.agent.core.openapi.spec.v3.tags.Tag;
import com.jd.live.agent.plugin.application.springboot.v2.openapi.OpenApiFactory;
import io.swagger.models.*;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.BasicAuthDefinition;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.RefParameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;

import static com.alibaba.nacos.api.utils.StringUtils.isEmpty;
import static com.jd.live.agent.core.util.CollectionUtils.*;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.*;

/**
 * Factory implementation for creating OpenApi objects from Swagger 2 specifications.
 */
public class OpenApi2Factory implements OpenApiFactory {

    private static final Set<String> EXCLUDE_KEYS = new HashSet<>(Arrays.asList("x-example", "x-examples", "x-nullable"));

    /**
     * Converts vendor extensions by removing specific x- extensions.
     */
    private static final Predicate<String> VENDOR_EXTENSIONS_PREDICATE = key -> !EXCLUDE_KEYS.contains(key);

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
                .openapi("3.1")
                .info(buildInfo(swagger.getInfo()))
                .externalDocs(buildExternalDoc(swagger.getExternalDocs()))
                .servers(buildServer(swagger.getSchemes(), swagger.getHost(), swagger.getBasePath()))
                .tags(toList(swagger.getTags(), this::buildTag))
                .components(buildComponents(swagger, defaults)) // handle components first
                .paths(toLinkMap(swagger.getPaths(), path -> buildPathItem(swagger, path, defaults)))
                .specVersion("V31")
                .security(toList(swagger.getSecurity(), item -> toLinkMap(item.getRequirements(), v -> new ArrayList<>(v))))
                .extensions(copy(swagger.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
                .extension("x-original-swagger-version", swagger.getSwagger())
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
                .extensions(copy(info.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
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
                .extensions(copy(contact.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
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
                .extensions(copy(license.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
                .build();
    }

    /**
     * Builds an ExternalDoc object from the OpenAPI ExternalDoc object.
     *
     * @param doc The OpenAPI ExternalDoc object
     * @return The internal ExternalDoc representation
     */
    protected ExternalDoc buildExternalDoc(io.swagger.models.ExternalDocs doc) {
        if (doc == null) {
            return null;
        }
        return ExternalDoc.builder()
                .description(doc.getDescription())
                .url(doc.getUrl())
                .extensions(copy(doc.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
                .build();
    }

    /**
     * Builds a list of server objects from Swagger 2.0 schemes, host, and basePath.
     *
     * @param schemes the protocol schemes (http, https, etc.)
     * @param host the host name or IP
     * @param basePath the base path of the API
     * @return a list of Server objects representing API servers
     */
    protected List<Server> buildServer(List<Scheme> schemes, String host, String basePath) {
        List<Server> servers = new ArrayList<>();
        String baseUrl = !isEmpty(basePath) ? basePath : "/";
        if (!isEmpty(host)) {
            baseUrl = host + baseUrl;
        }
        if (!baseUrl.startsWith("/") && schemes != null && !schemes.isEmpty()) {
            for (Scheme scheme : schemes) {
                servers.add(Server.builder().url(scheme.toValue() + "://" + baseUrl).build());
            }
        } else {
            if (!baseUrl.startsWith("/") && !"/".equals(baseUrl)) {
                baseUrl = "//" + baseUrl;
            }
            servers.add(Server.builder().url(baseUrl).build());
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
                .extensions(copy(tag.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
                .build();
    }

    /**
     * Builds a PathItem object from the OpenAPI PathItem object.
     *
     * @param pathItem The OpenAPI PathItem object to convert
     * @param defaults Default content types to use when not specified at operation level
     * @return The internal PathItem representation
     */
    protected PathItem buildPathItem(Swagger swagger, io.swagger.models.Path pathItem, ContentTypes defaults) {
        if (pathItem == null) {
            return null;
        } else if (pathItem instanceof RefPath) {
            RefPath refPath = (RefPath) pathItem;
            return PathItem.builder().ref(refPath.get$ref()).build();
        }
        return PathItem.builder()
                .parameters(toList(pathItem.getParameters(), v -> buildParameter(swagger, v)))
                .operations(toLinkMap(pathItem.getOperationMap(), m -> m.name().toLowerCase(), v -> buildOperation(swagger, v, defaults)))
                .extensions(copy(pathItem.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
                .build();
    }

    /**
     * Builds a Parameter object from the OpenAPI Parameter object.
     *
     * @param parameter The OpenAPI Parameter object
     * @return The internal Parameter representation
     */
    protected Parameter buildParameter(Swagger swagger, io.swagger.models.parameters.Parameter parameter) {
        if (parameter == null) {
            return null;
        }
        if (parameter instanceof io.swagger.models.parameters.RefParameter) {
            return Parameter.builder().ref(getRef(swagger, (RefParameter) parameter)).build();
        }
        Schema schema = null;
        String style = null;
        Boolean explode = null;
        if (parameter instanceof io.swagger.models.parameters.SerializableParameter) {
            SerializableParameter sp = (SerializableParameter) parameter;
            schema = buildSchema(sp);
            // array
            if ("array".equals(sp.getType())) {
                String cf = sp.getCollectionFormat();
                cf = isEmpty(cf) ? "csv" : cf;
                switch (cf) {
                    case "ssv":
                        if ("query".equals(sp.getIn())) {
                            style = SPACEDELIMITED.toString();
                        }
                        break;
                    case "pipes":
                        if ("query".equals(sp.getIn())) {
                            style = PIPEDELIMITED.toString();
                        }
                        break;
                    case "tsv":
                        break;
                    case "multi":
                        if ("query".equals(sp.getIn())) {
                            style = FORM.toString();
                            explode = true;
                        }
                        break;
                    case "csv":
                    default:
                        if ("query".equals(sp.getIn())) {
                            style = FORM.toString();
                            explode = false;
                        } else if ("header".equals(sp.getIn()) || "path".equals(sp.getIn())) {
                            style = SIMPLE.toString();
                            explode = false;
                        }
                }
            }
        }
        return Parameter.builder()
                .name(parameter.getName())
                .in(parameter.getIn())
                .description(parameter.getDescription())
                .required(parameter.getRequired())
                .style(style)
                .explode(explode)
                .schema(schema)
                .extensions(copy(parameter.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
                .build();
    }

    /**
     * Builds an Operation object from the OpenAPI Operation object.
     *
     * @param operation The OpenAPI Operation object to convert
     * @param defaults  Default content types to use when not specified at operation level
     * @return The internal Operation representation
     */
    protected Operation buildOperation(Swagger swagger, io.swagger.models.Operation operation, ContentTypes defaults) {
        if (operation == null) {
            return null;
        }
        ContentTypes consumes = defaults.of(operation.getConsumes(), operation.getConsumes());
        ContentTypes produces = defaults.of(operation.getProduces(), operation.getProduces());
        ParameterList parameters = new ParameterList(swagger, operation.getParameters());
        Map<String, ApiResponse> responses = toLinkMap(operation.getResponses(), v -> buildApiResponse(v, produces));
        return Operation.builder()
                .operationId(operation.getOperationId())
                .summary(operation.getSummary())
                .description(operation.getDescription())
                .externalDocs(buildExternalDoc(operation.getExternalDocs()))
                .tags(copy(operation.getTags()))
                .parameters(toList(parameters.getParameters(), v -> buildParameter(swagger, v)))
                .requestBody(parameters.getRefBody() != null ?
                        buildRequestBody(parameters.getRefBody(), consumes) :
                        (parameters.getBody() != null
                                ? buildRequestBody(parameters.getBody(), consumes)
                                : buildRequestBody(parameters.getForms(), consumes)))
                .responses(responses == null ? null : new ApiResponses(responses))
                .security(toList(operation.getSecurity(),
                        v -> new SecurityRequirement(toLinkMap(v, k -> new ArrayList(k)))))
                .deprecated(operation.isDeprecated())
                .extensions(copy(operation.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
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
            String ref = ((RefResponse) response).get$ref();
            if (ref.indexOf("#/responses/") == 0) {
                ref = Components.COMPONENTS_RESPONSES_REF + ref.substring("#/responses/".length());
            }
            return ApiResponse.builder().ref(ref).build();
        }
        Schema schema = buildSchema(response.getResponseSchema());
        List<String> produces = types.getProduces();
        produces = produces == null || produces.isEmpty() ? singletonList("*/*") : produces;
        return ApiResponse.builder()
                .description(response.getDescription())
                .content(toLinkMap(produces, v -> v, v -> MediaType.builder()
                        .schema(schema)
                        .example(response.getExamples().get(v))
                        .build()))
                .headers(toLinkMap(response.getHeaders(), this::buildHeader))
                .extensions(copy(response.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
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
        return Header.builder()
                .description(property.getDescription())
                .required(property.getRequired())
                .schema(buildSchema(property))
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
        // OpenAPI 3.0 specification no longer includes BodyParameter and FormParameter
        ParameterComponents params = new ParameterComponents(swagger.getParameters());
        Map<String, Schema> schemas = toLinkMap(swagger.getDefinitions(), this::buildSchema);
        Map<String, FormParameter> forms = params.getForms();
        if (forms != null && !forms.isEmpty()) {
            if (schemas == null) {
                schemas = new LinkedHashMap<>();
            }
            for (Map.Entry<String, FormParameter> entry : forms.entrySet()) {
                // formData_ is added not to overwrite existing schemas
                schemas.put("formData_" + entry.getKey(), buildSchema(entry.getValue()));
            }
        }
        return Components.builder()
                .schemas(schemas)
                .parameters(toLinkMap(params.getParameters(), v -> buildParameter(swagger, v)))
                .requestBodies(toLinkMap(params.getBodies(), v -> buildRequestBody(v, defaults)))
                .pathItems(toLinkMap(swagger.getPaths(), v -> buildPathItem(swagger, v, defaults)))
                .responses(toLinkMap(swagger.getResponses(), v -> buildApiResponse(v, defaults)))
                .securitySchemes(toLinkMap(swagger.getSecurityDefinitions(), this::buildSecurityScheme))
                .extensions(copy(swagger.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
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
                .in(definition.getIn().toValue())
                .extensions(copy(definition.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
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
                .extensions(copy(definition.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
                .build();
    }

    protected SecurityScheme buildSecurityScheme(io.swagger.models.auth.OAuth2Definition definition) {
        if (definition == null) {
            return null;
        }
        OAuthFlows flows = null;
        Map<String, String> scopes = copy(definition.getScopes());
        String flow = definition.getFlow();
        if ("implicit".equals(flow)) {
            flows = OAuthFlows.builder().implicit(OAuthFlow.builder()
                    .authorizationUrl(definition.getAuthorizationUrl())
                    .scopes(scopes).build()).build();
        } else if ("password".equals(flow)) {
            flows = OAuthFlows.builder().password(OAuthFlow.builder()
                    .tokenUrl(definition.getTokenUrl())
                    .scopes(scopes).build()).build();
        } else if ("application".equals(flow)) {
            flows = OAuthFlows.builder().clientCredentials(OAuthFlow.builder()
                    .tokenUrl(definition.getTokenUrl())
                    .scopes(scopes).build()).build();
        } else if ("accessCode".equals(flow)) {
            flows = OAuthFlows.builder().authorizationCode(OAuthFlow.builder()
                    .authorizationUrl(definition.getAuthorizationUrl())
                    .tokenUrl(definition.getTokenUrl())
                    .scopes(scopes).build()).build();
        }
        return SecurityScheme.builder()
                .description(definition.getDescription())
                .type(definition.getType())
                .flows(flows)
                .extensions(copy(definition.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
                .build();
    }

    /**
     * Converts an OpenAPI BodyParameter to an internal RequestBody object.
     *
     * @param parameter The source OpenAPI body parameter
     * @param contentTypes Contains content type information (consumes)
     * @return A constructed RequestBody object or null if parameter is null
     */
    protected RequestBody buildRequestBody(io.swagger.models.parameters.BodyParameter parameter, ContentTypes contentTypes) {
        if (parameter == null) {
            return null;
        }

        List<String> mediaTypes = contentTypes.consumes == null || contentTypes.consumes.isEmpty()
                ? singletonList("*/*")
                : contentTypes.consumes;
        Map<String, String> examples = parameter.getExamples();
        Schema schema = buildSchema(parameter.getSchema());
        return RequestBody.builder()
                .description(parameter.getDescription())
                .content(toMap(mediaTypes, k -> k, k -> MediaType.builder()
                        .schema(schema)
                        .example(examples.get(k))
                        .build()))
                .required(parameter.getRequired())
                .extensions(copy(parameter.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE))
                .build();
    }

    /**
     * Converts OpenAPI RefParameter to internal RequestBody.
     *
     * @param parameter    source parameter
     * @param contentTypes content type information
     * @return request body or null
     */
    protected RequestBody buildRequestBody(io.swagger.models.parameters.RefParameter parameter, ContentTypes contentTypes) {
        if (parameter == null) {
            return null;
        }
        String ref = parameter.get$ref();
        if (ref.indexOf("#/parameters/") == 0) {
            ref = Components.COMPONENTS_REQUEST_BODIES_REF + ref.substring("#/parameters/".length());
        }
        return RequestBody.builder().name(parameter.getName()).ref(ref).build();
    }

    /**
     * Converts form parameters to multipart RequestBody.
     *
     * @param parameters   form parameters
     * @param contentTypes content types
     * @return request body with form schema
     */
    protected RequestBody buildRequestBody(List<io.swagger.models.parameters.FormParameter> parameters, ContentTypes contentTypes) {
        if (parameters == null) {
            return null;
        }
        List<String> mediaTypes = contentTypes.consumes == null || contentTypes.consumes.isEmpty()
                ? singletonList("multipart/form-data")
                : contentTypes.consumes;
        Schema.SchemaBuilder builder = Schema.builder();
        for (io.swagger.models.parameters.FormParameter parameter : parameters) {
            builder.property(parameter.getName(), buildSchema(parameter));
            if (parameter.getRequired()) {
                builder.required(parameter.getName());
            }
        }
        Schema schema = builder.build();

        return RequestBody.builder()
                .content(toMap(mediaTypes, k -> k, k -> MediaType.builder().schema(schema).build()))
                .build();
    }

    /**
     * Converts a Swagger SerializableParameter to an OpenAPI Schema object.
     *
     * @param parameter The SerializableParameter to convert
     * @return A Schema object with type, format and items properties set
     */
    protected Schema buildSchema(io.swagger.models.parameters.SerializableParameter parameter) {
        return Schema.builder()
                // integer/number/boolean/string/object
                .type(parameter.getType())
                // byte/binary/date/date-time/password/email/uuid...
                .format(parameter.getFormat())
                .items(buildSchema(parameter.getItems()))
                .multipleOf(parameter.getMultipleOf() == null ? null : new BigDecimal(parameter.getMultipleOf().toString()))
                .maximum(parameter.getMaximum())
                .exclusiveMaximum(parameter.isExclusiveMaximum())
                .minimum(parameter.getMinimum())
                .exclusiveMinimum(parameter.isExclusiveMinimum())
                .minLength(parameter.getMinLength())
                .maxLength(parameter.getMaxLength())
                .pattern(parameter.getPattern())
                .build();
    }

    protected Schema buildSchema(io.swagger.models.Model model) {
        if (model == null) {
            return null;
        } else if (model instanceof BooleanValueModel || model.getBooleanValue() != null) {
            return Schema.builder().booleanSchemaValue(model.getBooleanValue()).build();
        } else if (model instanceof RefModel) {
            return buildSchema((RefModel) model);
        } else if (model instanceof ArrayModel) {
            return buildSchema((ArrayModel) model);
        } else if (model instanceof ComposedModel) {
            return buildSchema((ComposedModel) model);
        } else if (model instanceof ModelImpl) {
            return buildSchema((ModelImpl) model);
        }
        return builder(model).build();
    }

    protected Schema buildSchema(io.swagger.models.RefModel model) {
        String ref = model.get$ref();
        // Convert from #/definitions/Model to #/components/schemas/Model
        if (ref.startsWith("#/definitions/")) {
            ref = Components.COMPONENTS_SCHEMAS_REF + ref.substring("#/definitions/".length());
        }
        return Schema.builder().ref(ref).build();
    }

    protected Schema buildSchema(io.swagger.models.ArrayModel model) {
        return builder(model)
                .type(model.getType())
                .items(buildSchema(model.getItems()))
                .uniqueItems(model.getUniqueItems())
                .minItems(model.getMinItems())
                .maxItems(model.getMaxItems())
                .build();
    }

    protected Schema buildSchema(io.swagger.models.ComposedModel model) {
        return builder(model).allOf(toList(model.getAllOf(), this::buildSchema)).build();
    }

    protected Schema buildSchema(io.swagger.models.ModelImpl model) {
        Property additional = model.getAdditionalProperties();
        Boolean bool = additional == null ? null : additional.getBooleanValue();
        return builder(model)
                .type("file".equals(model.getType()) ? "string" : model.getType())
                .format("file".equals(model.getType()) ? "binary" : model.getFormat())
                .additionalProperties(bool != null ? bool : buildSchema(additional))
                .uniqueItems(model.getUniqueItems())
                .defaultValue(model.getDefaultValue())
                .enums(copy(model.getEnum()))
                .discriminator(isEmpty(model.getDiscriminator())
                        ? null
                        : Discriminator.builder().propertyName(model.getDiscriminator()).build())
                .build();
    }

    protected Schema buildSchema(io.swagger.models.properties.Property property) {
        if (property == null) {
            return null;
        } else if (property instanceof BooleanProperty || property.getBooleanValue() != null) {
            return Schema.builder().booleanSchemaValue(property.getBooleanValue()).build();
        } else if (property instanceof RefProperty) {
            return buildSchema((RefProperty) property);
        } else if (property instanceof ObjectProperty) {
            return buildSchema((ObjectProperty) property);
        } else if (property instanceof ArrayProperty) {
            return buildSchema((ArrayProperty) property);
        } else if (property instanceof AbstractNumericProperty) {
            return buildSchema((AbstractNumericProperty) property);
        } else if (property instanceof StringProperty) {
            // ByteArrayProperty/EmailProperty
            return buildSchema((StringProperty) property);
        } else if (property instanceof MapProperty) {
            return buildSchema((MapProperty) property);
        } else if (property instanceof DateProperty) {
            return buildSchema((DateProperty) property);
        } else if (property instanceof DateTimeProperty) {
            return buildSchema((DateTimeProperty) property);
        } else if (property instanceof PasswordProperty) {
            return buildSchema((PasswordProperty) property);
        } else if (property instanceof UUIDProperty) {
            return buildSchema((UUIDProperty) property);
        } else if (property instanceof ComposedProperty) {
            return buildSchema((ComposedProperty) property);
        } else if (property instanceof BinaryProperty) {
            return buildSchema((BinaryProperty) property);
        } else if (property instanceof FileProperty) {
            return buildSchema((FileProperty) property);
        } else {
            return builder(property).build();
        }
    }

    /**
     * Builds a Schema object from a map property.
     *
     * @param property the map property object
     * @return built Schema object with map configuration
     */
    protected Schema buildSchema(MapProperty property) {
        Boolean value = property.getAdditionalProperties().getBooleanValue();
        return builder(property)
                .minProperties(property.getMinProperties())
                .maxProperties(property.getMaxProperties())
                .additionalProperties(value != null ? value : buildSchema(property.getAdditionalProperties()))
                .build();
    }

    /**
     * Builds a Schema object from a string property.
     *
     * @param property the string property object
     * @return built Schema object with string configuration
     */
    protected Schema buildSchema(StringProperty property) {
        return builder(property)
                .maxLength(property.getMaxLength())
                .minLength(property.getMinLength())
                .pattern(property.getPattern())
                .defaultValue(property.getDefault())
                .enums(copy(property.getEnum()))
                .build();
    }

    /**
     * Builds a Schema object from a password property.
     *
     * @param property the password property object
     * @return built Schema object with password configuration
     */
    protected Schema buildSchema(PasswordProperty property) {
        return builder(property)
                .maxLength(property.getMaxLength())
                .minLength(property.getMinLength())
                .pattern(property.getPattern())
                .defaultValue(property.getDefault())
                .enums(copy(property.getEnum()))
                .build();
    }

    /**
     * Builds a Schema object from a uuid property.
     *
     * @param property the uuid property object
     * @return built Schema object with uuid configuration
     */
    protected Schema buildSchema(UUIDProperty property) {
        return builder(property)
                .maxLength(property.getMaxLength())
                .minLength(property.getMinLength())
                .pattern(property.getPattern())
                .defaultValue(property.getDefault())
                .enums(copy(property.getEnum()))
                .build();
    }

    /**
     * Builds a Schema object from a binary property.
     *
     * @param property the binary property object
     * @return built Schema object with binary configuration
     */
    protected Schema buildSchema(BinaryProperty property) {
        return builder(property)
                .maxLength(property.getMaxLength())
                .minLength(property.getMinLength())
                .pattern(property.getPattern())
                .defaultValue(property.getDefault())
                .enums(copy(property.getEnum()))
                .build();
    }

    /**
     * Builds a Schema object from a date property.
     *
     * @param property the binary date object
     * @return built Schema object with date configuration
     */
    protected Schema buildSchema(DateProperty property) {
        return builder(property).enums(copy(property.getEnum())).build();
    }

    /**
     * Builds a Schema object from a date time property.
     *
     * @param property the binary date object
     * @return built Schema object with date time configuration
     */
    protected Schema buildSchema(DateTimeProperty property) {
        return builder(property).enums(copy(property.getEnum())).build();
    }

    /**
     * Builds a Schema object from a numeric property.
     *
     * @param property the numeric property object
     * @return built Schema object with numeric configuration
     */
    protected Schema buildSchema(AbstractNumericProperty property) {
        return builder(property)
                .maximum(property.getMaximum())
                .minimum(property.getMinimum())
                .multipleOf(property.getMultipleOf())
                .exclusiveMaximum(property.getExclusiveMaximum())
                .exclusiveMinimum(property.getExclusiveMinimum())
                .build();
    }

    /**
     * Builds a Schema object from a file property.
     *
     * @param property the file property object
     * @return built Schema object with file configuration
     */
    protected Schema buildSchema(FileProperty property) {
        return builder(property).build();
    }

    /**
     * Builds a Schema object from an array property.
     *
     * @param property the array property object
     * @return built Schema object with array configuration
     */
    protected Schema buildSchema(ArrayProperty property) {
        return builder(property)
                .maxItems(property.getMaxItems())
                .minItems(property.getMinItems())
                .uniqueItems(property.getUniqueItems())
                .items(buildSchema(property.getItems()))
                .build();
    }

    /**
     * Builds a Schema object from a object property.
     *
     * @param property the object property object
     * @return built Schema object with object configuration
     */
    protected Schema buildSchema(ObjectProperty property) {
        return builder(property).properties(toMap(property.getProperties(), this::buildSchema)).build();
    }

    /**
     * Builds a Schema object from a composed property.
     *
     * @param property the composed property object
     * @return built Schema object with composed configuration
     */
    protected Schema buildSchema(ComposedProperty property) {
        return builder(property).allOf(toList(property.getAllOf(), this::buildSchema)).build();
    }

    /**
     * Builds a Schema object from a reference property.
     * Converts OpenAPI 2.0 reference path (#/definitions) to OpenAPI 3.0 format (#/components/schemas).
     *
     * @param property the reference property object
     * @return built Schema object with converted reference path
     */
    protected Schema buildSchema(RefProperty property) {
        RefProperty refProperty = property;
        String ref = refProperty.get$ref();
        if (ref.indexOf("#/definitions/") == 0) {
            ref = Components.COMPONENTS_SCHEMAS_REF + ref.substring("#/definitions/".length());
        }
        return Schema.builder().type(refProperty.getType()).ref(ref).build();
    }

    /**
     * Creates a Schema builder with basic property configuration.
     *
     * @param property the property object
     * @return Schema builder with property attributes
     */
    protected Schema.SchemaBuilder builder(Property property) {
        return Schema.builder()
                .name(property.getName())
                .type(property.getType())
                .format(property.getFormat())
                .title(property.getTitle())
                .description(property.getDescription())
                .readOnly(property.getReadOnly())
                .example(property.getExample())
                .extensions(copy(property.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE));
    }

    /**
     * Builds schema from Swagger model.
     *
     * @param model source model
     * @return schema builder
     */
    protected Schema.SchemaBuilder builder(io.swagger.models.Model model) {
        Schema.SchemaBuilder builder = Schema.builder()
                .title(model.getTitle())
                .description(model.getDescription())
                .properties(toLinkMap(model.getProperties(), this::buildSchema))
                .example(model.getExample())
                .extensions(copy(model.getVendorExtensions(), VENDOR_EXTENSIONS_PREDICATE));
        if (model instanceof AbstractModel) {
            AbstractModel am = (AbstractModel) model;
            return builder
                    .externalDocs(buildExternalDoc(am.getExternalDocs()))
                    .maximum(am.getMaximum())
                    .minimum(am.getMinimum())
                    .multipleOf(am.getMultipleOf())
                    .exclusiveMaximum(am.getExclusiveMaximum())
                    .exclusiveMinimum(am.getExclusiveMinimum())
                    .maxLength(am.getMaxLength())
                    .minLength(am.getMinLength())
                    .pattern(am.getPattern())
                    .required(copy(am.getRequired()));
        }
        return builder;
    }

    /**
     * Resolves the appropriate component reference for a parameter based on its type.
     *
     * @param swagger   The Swagger document containing parameter definitions
     * @param parameter The reference parameter to process
     * @return Updated reference string pointing to the correct OpenAPI 3.0 component
     */
    protected String getRef(Swagger swagger, io.swagger.models.parameters.RefParameter parameter) {
        io.swagger.models.parameters.Parameter target = swagger == null ? null : swagger.getParameter(parameter.getSimpleRef());
        String ref = parameter.get$ref();
        if (ref.indexOf("#/parameters/") == 0) {
            String id = ref.substring("#/parameters/".length());
            if (target instanceof BodyParameter) {
                ref = Components.COMPONENTS_REQUEST_BODIES_REF + id;
            } else if (target instanceof FormParameter) {
                // TODO form parameter
                ref = Components.COMPONENTS_PARAMETERS_REF + id;
            } else {
                ref = Components.COMPONENTS_PARAMETERS_REF + id;
            }
        }
        return ref;
    }

    /**
     * Represents content type information for API operations.
     */
    @Getter
    @AllArgsConstructor
    private static class ContentTypes {

        public static final String DEFAULT_CONTENT_TYPE = "application/json";
        /**
         * List of MIME types that the operation can consume.
         */
        private final List<String> consumes;

        /**
         * List of MIME types that the operation can produce.
         */
        private final List<String> produces;

        public ContentTypes of(List<String> consumes, List<String> produces) {
            consumes = consumes == null || consumes.isEmpty() ? this.consumes : consumes;
            consumes = consumes == null || consumes.isEmpty() ? Collections.singletonList(DEFAULT_CONTENT_TYPE) : consumes;
            produces = produces == null || produces.isEmpty() ? this.produces : produces;
            produces = produces == null || produces.isEmpty() ? Collections.singletonList(DEFAULT_CONTENT_TYPE) : produces;
            return new ContentTypes(consumes, produces);
        }
    }

    /**
     * Categorizes API parameters by their type for simplified processing.
     */
    @Getter
    private static class ParameterComponents {

        /**
         * Standard parameters (excluding body and header types)
         */
        private Map<String, io.swagger.models.parameters.Parameter> parameters;

        /**
         * Request body parameters
         */
        private Map<String, BodyParameter> bodies;

        /**
         * Form parameters
         */
        private Map<String, FormParameter> forms;

        /**
         * Constructs component container and sorts parameters by type
         *
         * @param parameters Raw parameter map to categorize
         */
        ParameterComponents(Map<String, io.swagger.models.parameters.Parameter> parameters) {
            Map<String, io.swagger.models.parameters.Parameter> params = null;
            Map<String, BodyParameter> bodies = null;
            Map<String, FormParameter> forms = null;
            if (parameters != null && !parameters.isEmpty()) {
                params = new LinkedHashMap<>(parameters.size());
                bodies = new LinkedHashMap<>();
                forms = new LinkedHashMap<>();
                for (Map.Entry<String, io.swagger.models.parameters.Parameter> entry : parameters.entrySet()) {
                    io.swagger.models.parameters.Parameter parameter = entry.getValue();
                    if (parameter instanceof BodyParameter) {
                        bodies.put(entry.getKey(), (BodyParameter) parameter);
                    } else if (parameter instanceof FormParameter) {
                        forms.put(entry.getKey(), (FormParameter) parameter);
                    } else {
                        params.put(entry.getKey(), parameter);
                    }
                }
            }
            this.parameters = params;
            this.bodies = bodies;
            this.forms = forms;
        }
    }

    /**
     * Container class for categorizing Swagger parameters into reference, body, and other parameters.
     */
    @Getter
    private static class ParameterList {

        /**
         * Reference parameter if present in the parameter list.
         */
        private List<RefParameter> refs;

        private RefParameter refBody;

        private List<RefParameter> refForms;

        /**
         * Body parameter if present in the parameter list.
         */
        private BodyParameter body;

        private List<FormParameter> forms;

        /**
         * List of parameters that are neither reference nor body parameters.
         */
        private List<io.swagger.models.parameters.Parameter> parameters;

        /**
         * Constructs a Parameters object by categorizing the provided parameter list.
         *
         * @param parameters List of Swagger parameters to categorize
         */
        ParameterList(Swagger swagger, List<io.swagger.models.parameters.Parameter> parameters) {
            List<RefParameter> refs = new LinkedList<>();
            RefParameter refBodies = null;
            List<RefParameter> refForms = new LinkedList<>();
            BodyParameter bodies = null;
            List<FormParameter> forms = new LinkedList<>();

            List<io.swagger.models.parameters.Parameter> others = new LinkedList<>();
            if (parameters != null && !parameters.isEmpty()) {
                others = new ArrayList<>(parameters.size());
                for (io.swagger.models.parameters.Parameter parameter : parameters) {
                    if (parameter instanceof BodyParameter) {
                        bodies = (BodyParameter) parameter;
                    } else if (parameter instanceof RefParameter) {
                        RefParameter rp = (RefParameter) parameter;
                        io.swagger.models.parameters.Parameter target = swagger.getParameter(rp.getSimpleRef());
                        if (target instanceof BodyParameter) {
                            refBodies = rp;
                        } else if (target instanceof FormParameter) {
                            refForms.add(rp);
                        } else {
                            refs.add(rp);
                        }
                    } else if (parameter instanceof FormParameter) {
                        forms.add((FormParameter) parameter);
                    } else {
                        others.add(parameter);
                    }
                }
            }
            this.refs = refs.isEmpty() ? null : refs;
            this.refBody = refBodies;
            this.refForms = refForms.isEmpty() ? null : refForms;
            // only one body parameter
            this.body = refBodies != null ? null : bodies;
            // body and form parameters generally cannot coexist in the same operation
            this.forms = refBody != null || bodies != null || forms.isEmpty() ? null : forms;
            this.parameters = others.isEmpty() ? null : others;
        }
    }
}
