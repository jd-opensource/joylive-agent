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
package com.jd.live.agent.demo.multilive.controller;

import com.jd.live.agent.core.util.http.HttpStatus;
import com.jd.live.agent.demo.multilive.entity.Workspace;
import com.jd.live.agent.demo.multilive.service.LiveService;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.policy.live.LiveSpec;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.service.sync.api.ApiError;
import com.jd.live.agent.governance.service.sync.api.ApiResponse;
import com.jd.live.agent.governance.service.sync.api.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class LiveController {

    private final AtomicLong counter = new AtomicLong(0);

    @Resource
    private LiveService liveService;

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }

    @GetMapping("/v1/workspaces")
    public ApiResponse<ApiResult<List<Workspace>>> getWorkspaces() {
        List<Workspace> workspaces = liveService.getLiveSpaces();
        return new ApiResponse<>(String.valueOf(counter.incrementAndGet()), new ApiResult<>(HttpStatus.OK, workspaces));
    }

    @GetMapping("/v1/workspaces/{spaceId}/version/{version}")
    public ApiResponse<ApiResult<LiveSpace>> getLiveSpace(@PathVariable("spaceId") String spaceId,
                                            @PathVariable("version") long version) {
        LiveSpace liveSpace = liveService.getLiveSpace(spaceId);
        if (liveSpace == null) {
            return new ApiResponse<>(String.valueOf(counter.incrementAndGet()), new ApiError(HttpStatus.NOT_FOUND));
        } else {
            LiveSpec liveSpec = liveSpace.getSpec();
            Long ver = liveSpec == null ? null : liveSpec.getVersion();
            ver = ver == null ? 0 : ver;
            if (ver <= version) {
                return new ApiResponse<>(String.valueOf(counter.incrementAndGet()), new ApiError(HttpStatus.NOT_MODIFIED));
            } else {
                return new ApiResponse<>(String.valueOf(counter.incrementAndGet()), new ApiResult<>(HttpStatus.OK, liveSpace));
            }
        }
    }

    @GetMapping("/v1/services/{service}/version/{version}")
    public ApiResponse<ApiResult<Service>> getServiceLivePolicy(@PathVariable("service") String name,
                                                  @PathVariable("version") long version) {
        Service service = liveService.getService(name);
        if (service == null) {
            return new ApiResponse<>(String.valueOf(counter.incrementAndGet()), new ApiError(HttpStatus.NOT_FOUND));
        } else if (service.getVersion() <= version) {
            return new ApiResponse<>(String.valueOf(counter.incrementAndGet()), new ApiError(HttpStatus.NOT_MODIFIED));
        } else {
            return new ApiResponse<>(String.valueOf(counter.incrementAndGet()), new ApiResult<>(HttpStatus.OK, service));
        }
    }

}
