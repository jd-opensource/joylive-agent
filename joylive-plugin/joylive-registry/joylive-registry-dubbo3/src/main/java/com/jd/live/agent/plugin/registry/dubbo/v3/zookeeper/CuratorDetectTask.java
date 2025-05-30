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
package com.jd.live.agent.plugin.registry.dubbo.v3.zookeeper;

import com.jd.live.agent.governance.probe.HealthProbe;

/**
 * A detect task that tests connectivity to ZooKeeper servers.
 * Requires consecutive successful checks to confirm recovery.
 */
public class CuratorDetectTask extends AbstractCuratorDetectTask {

    private final CuratorFailoverAddressList addressList;

    public CuratorDetectTask(CuratorFailoverAddressList addressList,
                             HealthProbe probe,
                             int successThreshold,
                             boolean connected,
                             CuratorDetectTaskListener listener) {
        super(probe, connected ? successThreshold : 1, connected ? 0 : addressList.size(), connected, listener);
        this.addressList = addressList;
    }

    @Override
    public Boolean call() {
        String current = addressList.current();
        switch (detect(current)) {
            case SUCCESS_EXCEEDED:
                onSuccess();
                return true;
            case FAILURE_MAX_RETRIES:
                onFailure();
                return true;
            case SUCCESS_BELOW:
                return false;
            case FAILURE:
            default:
                addressList.next();
                return false;

        }
    }

    @Override
    public long getRetryInterval() {
        return !connected ? 0 : super.getRetryInterval();
    }
}
