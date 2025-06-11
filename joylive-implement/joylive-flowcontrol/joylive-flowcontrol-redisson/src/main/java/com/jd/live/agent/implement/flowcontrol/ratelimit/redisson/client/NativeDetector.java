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
package com.jd.live.agent.implement.flowcontrol.ratelimit.redisson.client;

import lombok.Getter;

public class NativeDetector {

    @Getter
    private static final boolean isIoUringAvailable = detectIoUring();

    @Getter
    private static final boolean isEpollAvailable = detectEpoll();

    @Getter
    private static final boolean isKqueueAvailable = detectKqueue();

    private static boolean detectIoUring() {
        // TODO support iouring
//        try {
//            Class.forName("io.netty.channel.uring.IoUring");
//            return io.netty.channel.uring.IoUring.isAvailable();
//        } catch (ClassNotFoundException e) {
//            return false;
//        }
        return false;
    }

    private static boolean detectEpoll() {
        try {
            Class.forName("io.netty.channel.epoll.Epoll");
            return io.netty.channel.epoll.Epoll.isAvailable();
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean detectKqueue() {
        try {
            Class.forName("io.netty.channel.kqueue.KQueue");
            return io.netty.channel.kqueue.KQueue.isAvailable();
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
