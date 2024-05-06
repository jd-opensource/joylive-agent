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
package com.jd.live.agent.implement.bytekit.bytebuddy;

import com.jd.live.agent.bootstrap.classloader.ResourcerType;
import com.jd.live.agent.core.bytekit.ByteBuilder;
import com.jd.live.agent.core.bytekit.ByteSupplier;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.extension.condition.ConditionMatcher;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.InjectLoader;
import com.jd.live.agent.core.inject.annotation.Injectable;

import java.util.List;

/**
 * BuddySupplier
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Injectable
@Extension(value = "BuddySupplier", provider = "ByteBuddy")
public class BuddySupplier implements ByteSupplier {

    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private List<BuilderHandler> handlers;

    @Inject(value = ConditionMatcher.COMPONENT_CONDITION_MATCHER)
    private ConditionMatcher conditionMatcher;

    @Override
    public ByteBuilder create() {
        return new BuddyBuilder(handlers, conditionMatcher);
    }
}

