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
package com.jd.live.agent.governance.service.sync.file;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.ConfigEvent;
import com.jd.live.agent.core.config.ConfigEvent.EventType;
import com.jd.live.agent.core.event.FileEvent;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.service.sync.AbstractSyncer;
import com.jd.live.agent.governance.service.sync.Subscription;
import com.jd.live.agent.governance.service.sync.SyncKey.FileKey;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * An abstract class that provides a base implementation for file synchronization.
 *
 * @param <T> The type of the data being synchronized.
 */
public abstract class AbstractFileSyncer<T> extends AbstractSyncer<FileKey, T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractFileSyncer.class);

    @Inject(value = Publisher.CONFIG, nullable = true)
    protected Publisher<FileEvent> publisher;

    protected File file;

    protected FileWatcher fileWatcher;

    protected Subscription<FileKey, T> subscription;

    @Override
    protected CompletableFuture<Void> doStart() {
        String resource = getSyncConfig().getResource(getDefaultResource());
        file = createFile(resource);
        if (file == null) {
            return Futures.future(new FileNotFoundException("File is not found. " + resource));
        }
        return super.doStart();
    }

    @Override
    protected void startSync() throws Exception {
        subscription = new Subscription<>(getName(), createFileKey(file), response -> {
            switch (response.getStatus()) {
                case SUCCESS:
                    onSuccess(response.getData());
                    break;
                case NOT_FOUND:
                    onNotFound(file);
                    break;
                case NOT_MODIFIED:
                    break;
                case ERROR:
                    onError(response.getError(), response.getThrowable());
                    break;
            }
        });
        subscriptions.put(file.getName(), subscription);
        syncer.sync(subscription);
    }

    @Override
    protected void stopSync() {
        Close.instance().close(fileWatcher);
    }

    /**
     * Returns the default resource for this file syncer.
     *
     * @return The default resource for this file syncer.
     */
    protected abstract String getDefaultResource();

    /**
     * Creates a new {@link FileKey} object for the specified file.
     *
     * @param file The file for which to create a {@link FileKey} object.
     * @return A new {@link FileKey} object representing the specified file and its type.
     */
    protected FileKey createFileKey(File file) {
        return new FileKey(file, getType());
    }

    /**
     * Creates a new file object from the specified resource.
     *
     * @param resource The name of the resource to load.
     * @return A file object representing the specified resource, or null if the resource does not exist or an error occurs.
     */
    protected File createFile(String resource) {
        URL url = this.getClass().getClassLoader().getResource(resource);
        if (url != null) {
            try {
                File file = new File(url.toURI());
                if (file.exists()) {
                    return file;
                }
            } catch (IllegalArgumentException | URISyntaxException ignore) {
            }
        }
        return null;
    }

    /**
     * Handles a successful synchronization operation.
     *
     * @param data The data to publish with the configuration event.
     */
    protected void onSuccess(T data) {
        publish(createEvent(data));
    }

    /**
     * Handles a situation where the file to synchronize does not exist.
     *
     * @param file The file that does not exist.
     */
    protected void onNotFound(File file) {
        publish(createEvent(null));
    }

    protected ConfigEvent createEvent(T data) {
        return new ConfigEvent(EventType.UPDATE_ALL, "", data, getType(), getName());
    }

    /**
     * Handles an error that occurs during the synchronization process.
     *
     * @param error The error message to log.
     * @param e     The exception that occurred.
     */
    protected void onError(String error, Throwable e) {
        logger.error(error, e);
    }

}
