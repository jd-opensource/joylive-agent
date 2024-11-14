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
package com.jd.live.agent.implement.service.config;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.AgentPath;
import com.jd.live.agent.core.event.FileEvent;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.service.AbstractService;
import com.jd.live.agent.core.util.Daemon;
import com.jd.live.agent.core.util.Waiter.Waiting;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;

@Injectable
@Extension("WatchDog")
@ConditionalOnProperty("agent.watchdog.enabled")
public class WatchDog extends AbstractService {

    private static final Logger logger = LoggerFactory.getLogger(WatchDog.class);

    @Config("agent.watchdog")
    private WatchDogConfig watchDogConfig = new WatchDogConfig();
    @Inject
    private AgentPath agentPath;
    @Inject("config")
    private Publisher<FileEvent> publisher;
    private Daemon daemon;
    private WatchService watchService;

    public WatchDog() {
        this(null, null);
    }

    public WatchDog(AgentPath agentPath, Publisher<FileEvent> publisher) {
        this.agentPath = agentPath;
        this.publisher = publisher;
    }

    @Override
    protected CompletableFuture<Void> doStart() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path path = agentPath.getConfigPath().toPath();
            path.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
            daemon = Daemon.builder().name(getName()).delay(watchDogConfig.getDelay()).interval(watchDogConfig.getInterval()).
                    condition(this::isStarted).callable(this::pollEvent).build();
            daemon.start();
            future.complete(null);
        } catch (Throwable e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        if (daemon != null) {
            daemon.stop();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignore) {
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private Waiting pollEvent() throws InterruptedException {
        WatchKey key = watchService.poll(watchDogConfig.getTimeout(), TimeUnit.MILLISECONDS);
        if (key != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                Kind<?> kind = event.kind();
                Path pathName = (Path) event.context();
                File file = new File(agentPath.getConfigPath(), pathName.toString());
                if (file.isFile()) {
                    FileEvent.EventType type = null;
                    if (kind == ENTRY_CREATE) {
                        type = FileEvent.EventType.CREATE;
                    } else if (kind == ENTRY_MODIFY) {
                        type = FileEvent.EventType.MODIFY;
                    } else if (kind == ENTRY_DELETE) {
                        type = FileEvent.EventType.DELETE;
                    }
                    if (type != null) {
                        logger.info("detected file changes. " + file.getPath());
                        publisher.offer(new FileEvent(type, file));
                    }
                }
            }
            key.reset();
        }
        return new Waiting(watchDogConfig.getInterval());
    }

}
