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
package com.jd.live.agent.plugin.router.gprc.loadbalance;

import io.grpc.CallOptions;
import io.grpc.LoadBalancer.Subchannel;

import java.util.List;
import java.util.function.Function;

public class LivePickerAdvice {

    public static final CallOptions.Key<LivePickerAdvice> KEY_PICKER_ADVICE = CallOptions.Key.create("x-picker-advice");

    private final Subchannel subchannel;

    private final Function<List<Subchannel>, Subchannel> election;

    public LivePickerAdvice(Subchannel subchannel) {
        this(subchannel, null);
    }

    public LivePickerAdvice(Function<List<Subchannel>, Subchannel> election) {
        this(null, election);
    }

    public LivePickerAdvice(Subchannel subchannel, Function<List<Subchannel>, Subchannel> election) {
        this.subchannel = subchannel;
        this.election = election;
    }

    public Subchannel getSubchannel() {
        return subchannel;
    }

    public Function<List<Subchannel>, Subchannel> getElection() {
        return election;
    }
}
