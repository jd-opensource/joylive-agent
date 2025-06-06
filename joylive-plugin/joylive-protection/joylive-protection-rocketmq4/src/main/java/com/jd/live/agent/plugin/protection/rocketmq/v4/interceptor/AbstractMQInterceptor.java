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
package com.jd.live.agent.plugin.protection.rocketmq.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.interceptor.AbstractDbConnectionInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.protection.rocketmq.v4.client.AbstractMQClient;
import org.apache.rocketmq.client.ClientConfig;

import static com.jd.live.agent.core.util.StringUtils.CHAR_SEMICOLON;
import static com.jd.live.agent.core.util.StringUtils.join;

/**
 * AbstractMQInterceptor
 */
public abstract class AbstractMQInterceptor<T extends ClientConfig, C extends AbstractMQClient<T>> extends AbstractDbConnectionInterceptor<C> {

    public AbstractMQInterceptor(PolicySupplier policySupplier, Publisher<DatabaseEvent> publisher) {
        super(policySupplier, publisher);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        T target = (T) ctx.getTarget();
        // Determine if a cluster failover was triggered
        DbResult result = getMaster(target.getNamesrvAddr());
        if (result != null && !result.isMaster()) {
            result.setNewAddress(join(result.getMaster().getAddresses(), CHAR_SEMICOLON));
            ctx.setAttribute(ATTR_OLD_ADDRESS, result);
            target.setNamesrvAddr(result.getNewAddress());
        }
        super.onEnter(ctx);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        T target = (T) ctx.getTarget();
        DbResult oldDb = ctx.getAttribute(ATTR_OLD_ADDRESS);
        String oldAddress = oldDb != null ? oldDb.getOldAddress() : target.getNamesrvAddr();
        String newAddress = oldDb != null ? oldDb.getNewAddress() : oldAddress;
        addConnection(createClient(target, new ClusterRedirect(oldAddress, newAddress)));
        // Avoid missing events caused by synchronous changes
        DbResult newDb = getMaster(oldAddress);
        if (isChanged(oldDb, newDb)) {
            publisher.offer(new DatabaseEvent());
        }
    }

    @Override
    protected void redirectTo(C client, ClusterAddress address) {
        client.reconnect(address);
        ClusterRedirect.redirect(client.getAddress().newAddress(address), consumer);
    }

    protected abstract C createClient(T target, ClusterRedirect redirect);

}
