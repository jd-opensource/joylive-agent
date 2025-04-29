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
package com.jd.live.agent.plugin.protection.mongodb.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.interceptor.AbstractDbConnectionInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.util.RedirectAddress;
import com.jd.live.agent.plugin.protection.mongodb.v4.client.LiveMongoClient;
import com.mongodb.client.MongoClient;

import java.util.function.Consumer;

import static com.jd.live.agent.governance.util.RedirectAddress.redirect;

/**
 * MongoClientsInterceptor
 */
public class MongoClientsInterceptor extends AbstractDbConnectionInterceptor<MongoClient, LiveMongoClient> {

    public MongoClientsInterceptor(PolicySupplier policySupplier, Publisher<DatabaseEvent> publisher) {
        super(policySupplier, publisher);
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        MongoClient oldClient = mc.getResult();
        RedirectAddress address = RedirectAddress.getAndRemove();
        if (address != null) {
            mc.setResult(createConnection(oldClient, address));
        }
    }

    @Override
    protected LiveMongoClient doCreateConnection(MongoClient client, RedirectAddress address, Consumer<LiveMongoClient> close) {
        return new LiveMongoClient(client, address, close);
    }

    @Override
    protected void onRedirect(LiveMongoClient connection, String address) {
        redirect(connection.getAddress().newAddress(address), consumer);
    }
}
