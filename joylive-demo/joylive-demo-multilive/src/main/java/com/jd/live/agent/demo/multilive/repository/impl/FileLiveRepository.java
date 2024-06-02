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
package com.jd.live.agent.demo.multilive.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.live.agent.demo.multilive.entity.Workspace;
import com.jd.live.agent.demo.multilive.repository.LiveRepository;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.policy.live.LiveSpec;
import com.jd.live.agent.governance.policy.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
public class FileLiveRepository implements LiveRepository {

    private final Logger logger = LoggerFactory.getLogger(FileLiveRepository.class);

    private final ObjectMapper mapper;

    private Map<String, Service> serviceMap;

    private Map<String, LiveSpace> liveSpaceMap;

    private List<LiveSpace> liveSpaces;

    public FileLiveRepository(ObjectMapper mapper) {
        this.mapper = mapper;
        CountDownLatch latch = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                serviceMap = loadServices().stream().collect(Collectors.toMap(Service::getName, s -> s));
                liveSpaces = loadSpaces();
                liveSpaceMap = liveSpaces.stream().collect(Collectors.toMap(LiveSpace::getId, s -> s));
                try {
                    latch.await(5000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        });
        thread.setDaemon(true);
        thread.start();
    }

    private List<Service> loadServices() {
        List<Service> result = null;
        URL url = this.getClass().getClassLoader().getResource("microservice.json");
        if (url != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                result = mapper.readValue(reader, new TypeReference<List<Service>>() {
                });

            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return result == null ? new ArrayList<>() : result;
    }

    private List<LiveSpace> loadSpaces() {
        List<LiveSpace> result = null;
        URL url = this.getClass().getClassLoader().getResource("livespaces.json");
        if (url != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                result = mapper.readValue(reader, new TypeReference<List<LiveSpace>>() {
                });

            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return result == null ? new ArrayList<>() : result;
    }

    @Override
    public Service getService(String name) {
        return serviceMap == null ? null : serviceMap.get(name);
    }

    @Override
    public LiveSpace getLiveSpace(String id) {
        return liveSpaceMap == null ? null : liveSpaceMap.get(id);
    }

    @Override
    public List<Workspace> getLiveSpaces() {
        List<Workspace> workspaces = new ArrayList<>();
        for (LiveSpace liveSpace : liveSpaces) {
            LiveSpec liveSpec = liveSpace.getSpec();
            if (liveSpec != null) {
                Workspace workspace = new Workspace();
                workspace.setId(liveSpec.getId());
                workspace.setCode(liveSpec.getCode());
                workspace.setName(liveSpec.getName());
                workspaces.add(workspace);
            }
        }
        return workspaces;
    }
}
