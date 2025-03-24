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
package com.jd.live.agent.governance.invoke.loadbalance;

import com.jd.live.agent.governance.instance.counter.Counter;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Candidate<T> {

    protected final T target;

    protected final int index;

    protected final Counter counter;

    @Setter
    protected Integer weight;

    public Candidate(T target, int index) {
        this(target, index, null, null);
    }

    public Candidate(T target, int index, Integer weight) {
        this(target, index, null, weight);
    }

    public Candidate(T target, int index, Counter counter, Integer weight) {
        this.target = target;
        this.index = index;
        this.counter = counter;
        this.weight = weight;
    }
}
