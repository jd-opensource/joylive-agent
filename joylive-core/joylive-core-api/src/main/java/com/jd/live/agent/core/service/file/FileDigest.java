package com.jd.live.agent.core.service.file;

import lombok.Getter;

/**
 * Inner class representing a digest of a file, which includes the last modified timestamp
 * and the CRC32 digest of the file's content.
 */
@Getter
public class FileDigest {

    protected final long lastModified;

    protected final long crc32;

    public FileDigest(long lastModified, long crc32) {
        this.lastModified = lastModified;
        this.crc32 = crc32;
    }

}
