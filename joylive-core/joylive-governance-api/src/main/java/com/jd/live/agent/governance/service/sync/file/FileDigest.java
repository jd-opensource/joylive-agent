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

/**
 * Inner class representing a digest of a file, which includes the last modified timestamp
 * and the CRC32 digest of the file's content.
 */
@Getter
public class FileDigest {

    private final long lastModified;

    private final long crc32;

    public FileDigest(long lastModified, long crc32) {
        this.lastModified = lastModified;
        this.crc32 = crc32;
    }

    public FileDigest(FileDigest digest) {
        this.lastModified = digest == null ? 0 : digest.lastModified;
        this.crc32 = digest == null ? 0 : digest.crc32;

    }

}
