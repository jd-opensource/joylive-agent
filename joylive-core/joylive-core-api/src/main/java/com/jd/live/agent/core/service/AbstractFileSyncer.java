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
package com.jd.live.agent.core.service;

import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.EventHandler;
import com.jd.live.agent.core.event.FileEvent;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.parser.ObjectParser;
import lombok.Getter;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.CRC32;

/**
 * Abstract class that provides a framework for synchronizing files with some external source.
 * It handles the basic lifecycle and synchronization logic, subclasses should provide
 * parsing and event handling specific to their needs.
 *
 * @param <T> the type of object that the file's content will be parsed into
 */
public abstract class AbstractFileSyncer<T> extends AbstractSyncService<T, AbstractFileSyncer.FileDigest> {

    @Inject(ObjectParser.JSON)
    protected ObjectParser jsonParser;

    @Inject(Publisher.CONFIG)
    protected Publisher<FileEvent> publisher;

    protected final EventHandler<FileEvent> handler = this::onFileEvent;
    protected File file;

    @Override
    protected CompletableFuture<Void> doStart() {
        config = getSyncConfig();
        file = getConfigFile();
        if (file != null && publisher != null) {
            publisher.addHandler(handler);
        }
        return super.doStart();
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        if (file != null && publisher != null) {
            publisher.removeHandler(handler);
        }
        return super.doStop();
    }

    @Override
    protected long getDelay() {
        return 0;
    }


    @Override
    public SyncResult<T, FileDigest> sync(SyncConfig config, FileDigest last) throws IOException {
        if (file != null) {
            FileContent content = readFile(last);
            if (content != null) {
                T result = parse(new InputStreamReader(new ByteArrayInputStream(content.bytes)));
                return new SyncResult<>(result, new FileDigest(content.lastModified, content.crc32.getValue()));
            }
        }
        return null;
    }

    /**
     * Retrieves the resource URL from the sync configuration.
     * @param config the sync configuration to get the resource from
     * @return the resource URL as a String
     */
    protected String getResource(SyncConfig config) {
        return config.getUrl();
    }

    /**
     * Parses the file content into an object of type T.
     * @param reader an InputStreamReader for the file content
     * @return the parsed object
     * @throws IOException if an error occurs during parsing
     */
    protected abstract T parse(InputStreamReader reader) throws IOException;

    /**
     * Reads the file and calculates its digest.
     * @param last the last known digest of the file
     * @return a FileContent object with the file's last modified timestamp, bytes, and CRC32 digest
     * @throws IOException if an error occurs while reading the file
     */
    protected FileContent readFile(FileDigest last) throws IOException {
        if (file == null || !file.exists()) {
            return null;
        }
        long lastModified = 0;
        lastModified = file.lastModified();
        if (last != null && last.getLastModified() == lastModified) {
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            copy(bis, bos);
        }
        byte[] bytes = bos.toByteArray();
        CRC32 crc32 = new CRC32();
        crc32.update(bytes);
        if (last != null && last.getCrc32() == crc32.getValue()) {
            return null;
        }
        return new FileContent(lastModified, bytes, crc32);
    }

    /**
     * Copies data from an InputStream to an OutputStream.
     * @param is the InputStream to copy from
     * @param os the OutputStream to copy to
     * @throws IOException if an error occurs during the copy
     */
    protected void copy(final InputStream is, final OutputStream os) throws IOException {
        if (is == null || os == null) {
            return;
        }
        byte[] buffer = new byte[1024 * 4];
        int c;
        while ((c = is.read(buffer, 0, buffer.length)) >= 0) {
            os.write(buffer, 0, c);
        }
    }

    /**
     * Handles file events, such as create, modify, and delete, and signals changes.
     * @param events a list of file events to process
     */
    protected void onFileEvent(List<Event<FileEvent>> events) {
        boolean changed = false;
        FileEvent fileEvent;
        for (Event<FileEvent> event : events) {
            fileEvent = event.getData();
            if (fileEvent.getFile().equals(file)) {
                switch (fileEvent.getType()) {
                    case CREATE:
                    case MODIFY:
                    case DELETE:
                        changed = true;
                }

            }
        }
        if (changed && waiter != null) {
            waiter.wakeup();
        }
    }

    protected File getConfigFile() {
        String resource = getResource(getSyncConfig());
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
     * Inner class representing the content of a file including its last modified timestamp,
     * the bytes of its content, and the CRC32 digest of the content.
     */
    protected static class FileContent {
        protected final long lastModified;
        protected final byte[] bytes;
        protected final CRC32 crc32;

        FileContent(long lastModified, byte[] bytes, CRC32 crc32) {
            this.lastModified = lastModified;
            this.bytes = bytes;
            this.crc32 = crc32;
        }
    }

    /**
     * Inner class representing a digest of a file, which includes the last modified timestamp
     * and the CRC32 digest of the file's content.
     */
    @Getter
    public static class FileDigest {

        protected final long lastModified;

        protected final long crc32;

        public FileDigest(long lastModified, long crc32) {
            this.lastModified = lastModified;
            this.crc32 = crc32;
        }

    }

}
