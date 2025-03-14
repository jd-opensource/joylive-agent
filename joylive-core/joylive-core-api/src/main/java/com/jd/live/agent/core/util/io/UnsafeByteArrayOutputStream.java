/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.core.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * UnsafeByteArrayOutputStream.
 */
public class UnsafeByteArrayOutputStream extends OutputStream {

    protected byte[] mBuf;
    protected int mCount;

    public UnsafeByteArrayOutputStream() {
        this(32);
    }

    public UnsafeByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: " + size);
        }
        mBuf = new byte[size];
    }

    @Override
    public void write(int b) {
        int newCount = mCount + 1;
        if (newCount > mBuf.length) {
            mBuf = copy(mBuf, Math.max(mBuf.length << 1, newCount));
        }
        mBuf[mCount] = (byte) b;
        mCount = newCount;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return;
        }
        int newCount = mCount + len;
        if (newCount > mBuf.length) {
            mBuf = copy(mBuf, Math.max(mBuf.length << 1, newCount));
        }
        System.arraycopy(b, off, mBuf, mCount, len);
        mCount = newCount;
    }

    public int size() {
        return mCount;
    }

    public void reset() {
        mCount = 0;
    }

    public byte[] toByteArray() {
        return copy(mBuf, mCount);
    }

    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(mBuf, 0, mCount);
    }

    public void writeTo(OutputStream out) throws IOException {
        out.write(mBuf, 0, mCount);
    }

    @Override
    public String toString() {
        return new String(mBuf, 0, mCount);
    }

    public String toString(String charset) throws UnsupportedEncodingException {
        return new String(mBuf, 0, mCount, charset);
    }

    @Override
    public void close() throws IOException {
    }

    protected byte[] copy(byte[] src, int length) {
        byte[] dest = new byte[length];
        System.arraycopy(src, 0, dest, 0, Math.min(src.length, length));
        return dest;
    }
}
