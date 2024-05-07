package com.jd.live.agent.plugin.router.springcloud.v3.response;

import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * ClientHttpOutboundResponse
 *
 * @since 1.0.0
 */
public class ClientHttpOutboundResponse extends AbstractHttpOutboundResponse<ClientHttpResponse> {

    public ClientHttpOutboundResponse(ClientHttpResponse response, Throwable throwable) {
        super(response, throwable);
    }

    @Override
    public String getCode() {
        try {
            return response == null ? "500" : String.valueOf(response.getStatusCode().value());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
