package com.jd.live.agent.plugin.router.springcloud.v4.request;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.ClientRequest;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A class that implements the HttpRequest interface and wrap a ReactiveClusterRequest.
 */
public class DegradeHttpRequest implements HttpRequest {

    private final ClientRequest request;

    // fix for spring-web 6.2
    private Map<String, Object> attributes;

    public DegradeHttpRequest(ClientRequest request) {
        this.request = request;
    }

    @Override
    @NonNull
    public HttpMethod getMethod() {
        return request.method();
    }

    @Override
    @NonNull
    public URI getURI() {
        return request.url();
    }

    @Override
    @NonNull
    public HttpHeaders getHeaders() {
        return request.headers();
    }

    public Map<String, Object> getAttributes() {
        // fix for spring-web 6.2
        if (attributes == null) {
            attributes = new LinkedHashMap<>();
        }
        return attributes;
    }
}
