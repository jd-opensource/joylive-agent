package com.jd.live.agent.governance.service.sync.api;

import com.jd.live.agent.core.parser.json.JsonAlias;
import com.jd.live.agent.core.util.http.HttpStatus;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.SyncStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResult<T> {

    private int code;

    @JsonAlias("msg")
    private String message;

    private T data;

    public ApiResult() {
    }

    public ApiResult(int code, String message) {
        this(code, message, null);
    }

    public ApiResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public ApiResult(HttpStatus status, T data) {
        this(status.value(), status.getReasonPhrase(), data);
    }

    public HttpStatus getStatus() {
        HttpStatus status = HttpStatus.resolve(code);
        return status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
    }

    public SyncResponse<T> asSyncResponse() {
        switch (getStatus()) {
            case OK:
                return new SyncResponse<>(SyncStatus.SUCCESS, data);
            case NOT_FOUND:
                return new SyncResponse<>(SyncStatus.NOT_FOUND, null);
            case NOT_MODIFIED:
                return new SyncResponse<>(SyncStatus.NOT_MODIFIED, null);
            default:
                return new SyncResponse<>(message);
        }
    }
}
