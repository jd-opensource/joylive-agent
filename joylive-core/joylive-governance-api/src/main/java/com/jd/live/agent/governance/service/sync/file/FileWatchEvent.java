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

import lombok.Getter;

import java.io.File;
import java.io.IOException;

/**
 * A class representing an event that occurs when a file is watched.
 * <p>
 * This class contains information about the type of event, the file involved, and the content of the file (if applicable).
 * It also includes an optional IOException that may have occurred during the event.
 * </p>
 */
@Getter
public class FileWatchEvent {
    /**
     * The type of event that occurred.
     */
    private final EventType type;

    /**
     * The file involved in the event.
     */
    private final File file;

    /**
     * The content of the file (if applicable).
     */
    private final byte[] content;

    /**
     * An optional IOException that may have occurred during the event.
     */
    private final IOException throwable;

    public FileWatchEvent(EventType type, File file, byte[] content) {
        this(type, file, content, null);
    }

    public FileWatchEvent(File file, IOException throwable) {
        this(EventType.ERROR, file, null, throwable);
    }

    public FileWatchEvent(EventType type, File file, byte[] content, IOException throwable) {
        this.type = type;
        this.file = file;
        this.content = content;
        this.throwable = throwable;
    }

    /**
     * An enum representing the possible types of events that can occur when watching a file.
     */
    public enum EventType {
        /**
         * The file has been updated.
         */
        UPDATE,

        /**
         * The file has been deleted.
         */
        DELETE,

        /**
         * An error occurred while watching the file.
         */
        ERROR,
    }
}