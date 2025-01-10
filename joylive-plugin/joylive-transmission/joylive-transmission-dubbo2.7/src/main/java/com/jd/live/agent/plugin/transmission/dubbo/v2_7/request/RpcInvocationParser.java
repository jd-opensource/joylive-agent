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
package com.jd.live.agent.plugin.transmission.dubbo.v2_7.request;

import com.jd.live.agent.governance.request.header.HeaderReader;
import com.jd.live.agent.governance.request.header.HeaderWriter;
import org.apache.dubbo.rpc.RpcInvocation;

import java.util.Collections;
import java.util.Iterator;

public class RpcInvocationParser implements HeaderWriter, HeaderReader {

    private final RpcInvocation request;

    public RpcInvocationParser(RpcInvocation request) {
        this.request = request;
    }

    @Override
    public Iterator<String> getNames() {
        return request.getObjectAttachments().keySet().iterator();
    }

    @Override
    public Iterable<String> getHeaders(String key) {
        Object value = request.getObjectAttachment(key);
        return value == null ? null : Collections.singletonList(value.toString());
    }

    @Override
    public String getHeader(String key) {
        Object value = request.getObjectAttachment(key);
        return value == null ? null : value.toString();
    }

    @Override
    public boolean isDuplicable() {
        return false;
    }

    @Override
    public void addHeader(String key, String value) {
        request.setObjectAttachment(key, value);
    }

    @Override
    public void setHeader(String key, String value) {
        request.setObjectAttachment(key, value);
    }
}
