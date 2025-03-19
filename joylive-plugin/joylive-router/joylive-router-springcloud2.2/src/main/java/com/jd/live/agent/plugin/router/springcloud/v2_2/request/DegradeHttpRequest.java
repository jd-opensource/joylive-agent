package com.jd.live.agent.plugin.router.springcloud.v2_2.request;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.ClientRequest;

import java.net.URI;

/**
 * A class that implements the HttpRequest interface and wrap a ReactiveClusterRequest.
 */
public class DegradeHttpRequest implements HttpRequest {

    private final ClientRequest request;

    public DegradeHttpRequest(ClientRequest request) {
        this.request = request;
    }

    @Override
    @NonNull
    public String getMethodValue() {
        return request.method().name();
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
}
