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
package com.jd.live.agent.governance.invoke.matcher.header;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.util.tag.Label;
import com.jd.live.agent.governance.invoke.matcher.AbstractTagMatcher;
import com.jd.live.agent.governance.invoke.matcher.TagMatcher;
import com.jd.live.agent.governance.request.HttpRequest;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.governance.request.RpcRequest;
import com.jd.live.agent.governance.rule.tag.TagCondition;

import java.util.List;

/**
 * HeaderTagMatcher is an implementation of the {@link TagMatcher} interface that matches
 * request tags based on HTTP headers or RPC attachments present in the incoming request.
 *
 * @since 1.0.0
 */
@Extension(value = "header")
public class HeaderTagMatcher extends AbstractTagMatcher {

    @Override
    protected List<String> getValues(TagCondition condition, Request request) {
        List<String> values = null;
        if (request instanceof HttpRequest) {
            values = ((HttpRequest) request).getHeaders(condition.getKey());
        } else if (request instanceof RpcRequest) {
            Object value = ((RpcRequest) request).getAttachment(condition.getKey());
            if (value instanceof String) {
                values = Label.parseValue((String) value);
            }
        }
        return values;
    }
}
