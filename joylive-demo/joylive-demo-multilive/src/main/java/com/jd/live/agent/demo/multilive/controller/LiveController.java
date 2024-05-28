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
import com.jd.live.agent.demo.multilive.service.LiveService;
import com.jd.live.agent.demo.multilive.vo.Response;
import com.jd.live.agent.demo.multilive.entity.Workspace;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.policy.live.LiveSpec;
import com.jd.live.agent.governance.policy.service.Service;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Repository
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
    public Response<List<Workspace>> getWorkspaces() {
        List<Workspace> workspaces = liveService.getLiveSpaces();
        Response.Result<List<Workspace>> result = new Response.Result<>(HttpStatus.OK.value(), null, workspaces);
        return new Response<>(String.valueOf(counter.incrementAndGet()), Response.SUCCESS, result);
    }

    @GetMapping("/v1/workspaces/{spaceId}/version/{version}")
    public Response<LiveSpace> getLiveSpace(@PathVariable("spaceId") String spaceId,
                                            @PathVariable("version") long version) {
        Response.Result<LiveSpace> result;
        LiveSpace liveSpace = liveService.getLiveSpace(spaceId);
        if (liveSpace == null) {
            result = new Response.Result<>(HttpStatus.NOT_FOUND.value(), null, null);
        } else {
            LiveSpec liveSpec = liveSpace.getSpec();
            Long ver = liveSpec == null ? null : liveSpec.getVersion();
            ver = ver == null ? 0 : ver;
            if (ver <= version) {
                result = new Response.Result<>(HttpStatus.NOT_MODIFIED.value(), null, null);
            } else {
                result = new Response.Result<>(HttpStatus.OK.value(), null, liveSpace);
            }
        }
        return new Response<>(String.valueOf(counter.incrementAndGet()), Response.SUCCESS, result);
    }

    @GetMapping("/v1/services/{service}/version/{version}")
    public Response<Service> getServiceLivePolicy(@PathVariable("service") String name,
                                                  @PathVariable("version") long version) {
        Response.Result<Service> result;
        Service service = liveService.getService(name);
        if (service == null) {
            result = new Response.Result<>(HttpStatus.NOT_FOUND.value(), null, null);
        } else if (service.getVersion() <= version) {
            result = new Response.Result<>(HttpStatus.NOT_MODIFIED.value(), null, null);
        } else {
            result = new Response.Result<>(HttpStatus.OK.value(), null, service);
        }
        return new Response<>(String.valueOf(counter.incrementAndGet()), Response.SUCCESS, result);
    }

}
