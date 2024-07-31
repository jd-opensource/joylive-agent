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
package com.jd.live.agent.governance.invoke.matcher.system;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.invoke.matcher.AbstractTagMatcher;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.governance.rule.tag.TagCondition;

import java.util.List;
import java.util.Map;

@Injectable
@Extension(value = "system")
public class SystemTagMatcher extends AbstractTagMatcher {

    @Inject
    private Map<String, SystemTagProvider> providers;

    @Override
    protected List<String> getValues(TagCondition condition, Request request) {
        SystemTagProvider provider = providers.get(condition.getKey());
        return provider == null ? null : provider.getValues(request, condition.getKey());
    }
}
