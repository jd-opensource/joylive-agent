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
package com.jd.live.agent.core.service.sync;

import com.jd.live.agent.core.config.ConfigEvent;
import com.jd.live.agent.core.config.ConfigEvent.EventType;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.EventHandler;
import com.jd.live.agent.core.event.FileEvent;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.util.Futures;
import lombok.Getter;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.CRC32;

import static com.jd.live.agent.core.service.sync.AbstractFileSyncer.FileDigest;

/**
 * Abstract class that provides a framework for synchronizing files with some external source.
 * It handles the basic lifecycle and synchronization logic, subclasses should provide
 * parsing and event handling specific to their needs.
 */
public abstract class AbstractFileSyncer extends AbstractConfigSyncer<String, FileDigest> {

    @Inject(Publisher.CONFIG)
    protected Publisher<FileEvent> publisher;

    protected final EventHandler<FileEvent> handler = this::onFileEvent;

    protected File file;

    @Override
    protected CompletableFuture<Void> doStart() {
        file = getConfigFile();
        if (file == null) {
            return Futures.future(new FileNotFoundException("File is not found. " + getResource()));
        }
        if (publisher != null) {
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
    public SyncResult<String, FileDigest> doSynchronize(FileDigest last) throws IOException {
        if (file != null) {
            FileContent content = readFile(last);
            if (content != null) {
                return new SyncResult<>(new String(content.getBytes()), new FileDigest(content.getLastModified(), content.getCrc32()));
            }
        }
        return null;
    }

    @Override
    protected ConfigEvent create(String value) {
        ConfigEvent result = super.create(value);
        result.setType(EventType.UPDATE_ALL);
        return result;
    }

    /**
     * Retrieves the resource URL from the sync configuration.
     * @return the resource URL as a String
     */
    protected String getResource() {
        String result = getSyncConfig().getUrl();
        return isResource(result) ? result : getDefaultResource();
    }

    /**
     * Gets the default resource for this configuration.
     *
     * @return The default resource for this configuration.
     */
    protected abstract String getDefaultResource();

    /**
     * Checks if the given file path represents a valid configuration file.
     *
     * @param file The file path to check.
     * @return true if the file is a valid configuration file, false otherwise.
     */
    protected boolean isResource(String file) {
        if (file == null || file.isEmpty()) {
            return false;
        } else if (file.startsWith("http://") || file.startsWith("https://")) {
            return false;
        } else if (file.startsWith("${") && file.endsWith("}")) {
            return false;
        } else {
            URL resource = getClass().getClassLoader().getResource(file);
            return resource != null;
        }
    }

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
        long lastModified = file.lastModified();
        if (last != null && last.getLastModified() == lastModified) {
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            copy(bis, bos);
        }
        byte[] bytes = bos.toByteArray();
        CRC32 crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        if (last != null && last.getCrc32() == crc32.getValue()) {
            return null;
        }
        return new FileContent(lastModified, crc32.getValue(), bytes);
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

    /**
     * Retrieves the configuration file associated with the specified resource.
     *
     * @return The configuration file if it exists, otherwise null.
     */
    protected File getConfigFile() {
        String resource = getResource();
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

    /**
     * Inner class representing the content of a file including its last modified timestamp,
     * the bytes of its content, and the CRC32 digest of the content.
     */
    @Getter
    public static class FileContent extends FileDigest {

        private final byte[] bytes;

        public FileContent(long lastModified, long crc32, byte[] bytes) {
            super(lastModified, crc32);
            this.bytes = bytes;
        }
    }
}
