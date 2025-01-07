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
package com.jd.live.agent.plugin.transmission.jdkhttp.request;

import com.jd.live.agent.governance.request.header.HeaderReader;
import com.jd.live.agent.governance.request.header.HeaderWriter;
import sun.net.www.MessageHeader;

import java.util.Iterator;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.splitList;

public class MessageHeaderParser implements HeaderReader, HeaderWriter {

    private final MessageHeader header;

    public MessageHeaderParser(MessageHeader header) {
        this.header = header;
    }

    @Override
    public Iterator<String> getNames() {
        return splitList(header.getHeaderNamesInList()).iterator();
    }

    @Override
    public List<String> getHeaders(String key) {
        return toList(header.multiValueIterator(key));
    }

    @Override
    public String getHeader(String key) {
        return header.findValue(key);
    }

    @Override
    public void setHeader(String key, String value) {
        header.set(key, value);
    }
}