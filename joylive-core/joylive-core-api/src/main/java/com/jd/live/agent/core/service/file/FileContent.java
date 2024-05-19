package com.jd.live.agent.core.service.file;

import lombok.Getter;

/**
 * Inner class representing the content of a file including its last modified timestamp,
 * the bytes of its content, and the CRC32 digest of the content.
 */
@Getter
public class FileContent extends FileDigest {

    private final byte[] bytes;

    public FileContent(long lastModified, long crc32, byte[] bytes) {
        super(lastModified, crc32);
        this.bytes = bytes;
    }
}
