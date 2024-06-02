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
package com.jd.live.agent.demo.multilive.service.impl;

import com.jd.live.agent.demo.multilive.entity.Workspace;
import com.jd.live.agent.demo.multilive.repository.LiveRepository;
import com.jd.live.agent.demo.multilive.service.LiveService;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.policy.service.Service;

import java.util.List;

@org.springframework.stereotype.Service
public class LiveServiceImpl implements LiveService {

    private final LiveRepository liveRepository;

    public LiveServiceImpl(LiveRepository liveRepository) {
        this.liveRepository = liveRepository;
    }

    @Override
    public Service getService(String name) {
        return liveRepository.getService(name);
    }

    @Override
    public LiveSpace getLiveSpace(String id) {
        return liveRepository.getLiveSpace(id);
    }

    @Override
    public List<Workspace> getLiveSpaces() {
        return liveRepository.getLiveSpaces();
    }
}
