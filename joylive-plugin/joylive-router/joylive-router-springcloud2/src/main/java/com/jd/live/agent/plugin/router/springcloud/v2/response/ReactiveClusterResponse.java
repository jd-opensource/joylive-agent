package com.jd.live.agent.plugin.router.springcloud.v2.response;

import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.response.AbstractHttpResponse.AbstractHttpOutboundResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * @author: yuanjinzhong
 * @date: 2025/1/3 18:00
 * @description:
 */
public class ReactiveClusterResponse extends AbstractHttpOutboundResponse<ClientResponse> {


    private String body;

    public ReactiveClusterResponse(ClientResponse response) {
        super(response);
        this.headers = new UnsafeLazyObject<>(() -> response.headers().asHttpHeaders());
        this.cookies = new UnsafeLazyObject<>(() -> HttpUtils.parseCookie(response.cookies(), ResponseCookie::getValue));
    }

    public ReactiveClusterResponse(ServiceError error, ErrorPredicate predicate) {
        super(error, predicate);
    }

    @Override
    public String getCode() {
        HttpStatus status = response == null ? null : response.statusCode();
        return status == null ? null : String.valueOf(status.value());
    }

    @Override
    public Object getResult() {
        if (body == null) {
            if (response == null) {
                body = "";
            } else {
                body = response.bodyToMono(String.class).block();
                response = ClientResponse.from(response).body(body).build();
            }
        }
        return body;
    }



}
