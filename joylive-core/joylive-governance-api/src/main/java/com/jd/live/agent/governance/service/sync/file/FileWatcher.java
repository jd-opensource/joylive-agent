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

import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.EventHandler;
import com.jd.live.agent.core.event.FileEvent;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Daemon;
import com.jd.live.agent.core.util.IOUtils;
import com.jd.live.agent.core.util.Waiter;
import com.jd.live.agent.governance.service.sync.SyncKey.FileKey;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.SyncStatus;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.governance.service.sync.file.FileWatchEvent.EventType;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.zip.CRC32;

/**
 * A class for watching changes to files and notifying listeners of those changes.
 */
public class FileWatcher implements AutoCloseable {

    protected String name;

    protected SyncConfig config;

    protected Publisher<FileEvent> publisher;

    protected final EventHandler<FileEvent> handler = this::onFileEvent;

    protected final Map<File, FileListener> subscriptions = new ConcurrentHashMap<>();

    protected final Map<File, FileDigest> digests = new ConcurrentHashMap<>();

    protected final Waiter.MutexWaiter waiter = new Waiter.MutexWaiter();

    protected final AtomicBoolean started = new AtomicBoolean(true);

    protected final AtomicLong counter = new AtomicLong();

    protected final Daemon daemon;

    public FileWatcher(String name, SyncConfig config, Publisher<FileEvent> publisher) {
        this.name = name;
        this.config = config;
        this.publisher = publisher;
        this.daemon = Daemon.builder()
                .name(name)
                .delay(config.getDelay())
                .fault(config.getFault())
                .interval(config.getInterval())
                .waiter(waiter)
                .condition(this::isStarted)
                .runnable(this::run)
                .build();
        this.daemon.start();
        if (publisher != null) {
            publisher.addHandler(handler);
        }
    }

    @Override
    public void close() throws Exception {
        if (started.compareAndSet(true, false)) {
            waiter.wakeup();
            Close.instance().close(daemon).closeIfExists(publisher, p -> p.removeHandler(handler));
        }
    }

    /**
     * Subscribes the specified listener to receive file update events for the specified file.
     *
     * @param file     The file to subscribe to.
     * @param listener The listener to notify when the file is updated.
     */
    public void subscribe(File file, FileListener listener) {
        if (file != null && listener != null) {
            if (subscriptions.putIfAbsent(file, listener) == null) {
                load(file, listener);
            }
        }
    }

    /**
     * Unsubscribes the specified listener from receiving file update events for the specified file.
     *
     * @param file The file to unsubscribe from.
     */
    public void unsubscribe(File file) {
        if (file != null) {
            subscriptions.remove(file);
        }
    }

    protected boolean isStarted() {
        return started.get();
    }

    /**
     * Handles file events, such as create, modify, and delete, and signals changes.
     *
     * @param events a list of file events to process
     */
    protected void onFileEvent(List<Event<FileEvent>> events) {
        List<File> files = new LinkedList<>();
        for (Event<FileEvent> event : events) {
            FileEvent fileEvent = event.getData();
            File file = fileEvent.getFile();
            if (subscriptions.containsKey(file)) {
                switch (fileEvent.getType()) {
                    case CREATE:
                    case MODIFY:
                    case DELETE:
                        files.add(file);
                }

            }
        }
        if (!files.isEmpty()) {
            waiter.wakeup();
        }
    }

    /**
     * The main run method executed by the daemon thread. It performs synchronization and updates the
     * state based on the result.
     */
    protected void run() {
        while (isStarted()) {
            counter.incrementAndGet();
            try {
                for (Map.Entry<File, FileListener> entry : subscriptions.entrySet()) {
                    File file = entry.getKey();
                    FileListener listener = entry.getValue();
                    load(file, listener);
                }
                waiter.await(config.getInterval(), TimeUnit.MILLISECONDS, null);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Reads the file and calculates its digest.
     *
     * @param file the file.
     * @return a FileContent object with the file's last modified timestamp, bytes, and CRC32 digest
     * @throws IOException if an error occurs while reading the file
     */
    protected FileContent load(File file) throws IOException {
        if (file == null || !file.exists()) {
            return null;
        }
        long lastModified = file.lastModified();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            IOUtils.copy(bis, bos);
        }
        byte[] bytes = bos.toByteArray();
        CRC32 crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return new FileContent(lastModified, crc32.getValue(), bytes);
    }

    /**
     * Loads the content of a file and notifies the specified listener of any changes.
     *
     * @param file     The file to load.
     * @param listener The listener to notify of any changes.
     */
    protected void load(File file, FileListener listener) {
        try {
            FileContent content = load(file);
            FileDigest digest = digests.put(file, new FileDigest(content));
            if (digest == null) {
                listener.onUpdate(content == null
                        ? new FileWatchEvent(EventType.DELETE, file, null)
                        : new FileWatchEvent(EventType.UPDATE, file, content.getBytes()));
            } else if (content == null) {
                listener.onUpdate(new FileWatchEvent(EventType.DELETE, file, null));
            } else if (content.getLastModified() != digest.getLastModified() || content.getCrc32() != digest.getCrc32()) {
                listener.onUpdate(new FileWatchEvent(EventType.UPDATE, file, content.getBytes()));
            }
        } catch (IOException e) {
            listener.onUpdate(new FileWatchEvent(file, e));
        }
    }

    /**
     * Creates a new Syncer object that can be used to synchronize data between a local file and a remote source.
     *
     * @param file     The file object to synchronize.
     * @param function A function that takes the content of the file as input and returns a list of objects of type T.
     * @param <T>      The type of the objects in the list returned by the parser function.
     * @return A new Syncer object that can be used to synchronize data between a local file and a remote source.
     */
    public <T> Syncer<FileKey, List<T>> createSyncer(File file, Function<byte[], List<T>> function) {
        return subscription -> {
            try {
                subscribe(file, event -> {
                    switch (event.getType()) {
                        case UPDATE:
                            subscription.onUpdate(new SyncResponse<>(SyncStatus.SUCCESS, function.apply(event.getContent())));
                            break;
                        case DELETE:
                            subscription.onUpdate(new SyncResponse<>(SyncStatus.NOT_FOUND, null));
                            break;
                        case ERROR:
                            subscription.onUpdate(new SyncResponse<>(event.getThrowable()));
                            break;
                    }
                });
            } catch (Throwable e) {
                subscription.onUpdate(new SyncResponse<>(e));
            }
        };
    }
}
