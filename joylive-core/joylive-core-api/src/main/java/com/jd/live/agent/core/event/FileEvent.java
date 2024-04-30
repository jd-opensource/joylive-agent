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
package com.jd.live.agent.core.event;

import lombok.Getter;

import java.io.File;

/**
 * Represents an event related to file operations, such as creation, modification, or deletion.
 * This class encapsulates the type of the event and the file that the event pertains to.
 */
@Getter
public class FileEvent {

    /**
     * The type of the file event, indicating the nature of the operation performed on the file.
     */
    private final EventType type;

    /**
     * The file that is the subject of this event.
     */
    private final File file;

    /**
     * Constructs a FileEvent with the specified type and file.
     *
     * @param type The type of the file event.
     * @param file The file that is the subject of this event.
     */
    public FileEvent(EventType type, File file) {
        this.type = type;
        this.file = file;
    }

    /**
     * Enumerates the types of file events, including creation, modification, and deletion.
     */
    public enum EventType {
        CREATE,  // File creation event
        MODIFY,  // File modification event
        DELETE   // File deletion event
    }

}
