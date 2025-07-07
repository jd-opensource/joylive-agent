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
package com.jd.live.agent.plugin.failover.rocketmq.v4.client;

import com.jd.live.agent.governance.db.mq.AbstractMQClient;
import com.jd.live.agent.governance.db.mq.MQClientRole;
import com.jd.live.agent.governance.db.DbFailover;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.hook.SendMessageHook;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.trace.AsyncTraceDispatcher;
import org.apache.rocketmq.client.trace.TraceDispatcher;
import org.apache.rocketmq.client.trace.hook.EndTransactionTraceHookImpl;
import org.apache.rocketmq.client.trace.hook.SendMessageTraceHookImpl;
import org.apache.rocketmq.remoting.RPCHook;

import java.util.List;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getQuietly;
import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.setValue;
import static com.jd.live.agent.plugin.failover.rocketmq.v4.client.RocketMQConfig.*;

public class ProducerClient extends AbstractMQClient {

    private static final String FIELD_PRODUCER_IMPL = "defaultMQProducerImpl";

    public ProducerClient(Object producer, DbFailover failover) {
        super(producer, failover);
    }

    @Override
    public MQClientRole getRole() {
        return MQClientRole.PRODUCER;
    }

    @Override
    public String getType() {
        return TYPE_ROCKETMQ;
    }

    @Override
    public String getServerAddress() {
        return ((DefaultMQProducer) target).getNamesrvAddr();
    }

    @Override
    public void setServerAddress(String address) {
        ((DefaultMQProducer) target).setNamesrvAddr(address);
    }

    @Override
    protected void doClose() {
        ((DefaultMQProducer) target).shutdown();
    }

    @Override
    protected void doStart() throws MQClientException {
        // reset to restart
        reset();
        ((DefaultMQProducer) target).start();
    }

    /**
     * Replaces producer instance while keeping RPC and tracing intact.
     */
    protected void reset() {
        // inline to fix classloader issue.
        DefaultMQProducerImpl oldProducerImpl = getQuietly(target, FIELD_PRODUCER_IMPL);
        RPCHook rpcHook = getQuietly(oldProducerImpl, FIELD_RPC_HOOK);
        DefaultMQProducer producer = (DefaultMQProducer) target;
        DefaultMQProducerImpl newProducerImpl = new DefaultMQProducerImpl(producer, rpcHook);
        setValue(target, FIELD_PRODUCER_IMPL, newProducerImpl);
        TraceDispatcher oldDispatcher = producer.getTraceDispatcher();
        if (oldDispatcher instanceof AsyncTraceDispatcher) {
            // create new trace dispatcher
            AsyncTraceDispatcher newDispatcher = new AsyncTraceDispatcher(producer.getProducerGroup(),
                    TraceDispatcher.Type.PRODUCE, ((AsyncTraceDispatcher) oldDispatcher).getTraceTopicName(), rpcHook);
            newDispatcher.setHostProducer(newProducerImpl);
            List<SendMessageHook> hooks = getQuietly(newProducerImpl, "sendMessageHookList");
            for (int i = hooks.size() - 1; i >= 0; i--) {
                SendMessageHook hook = hooks.get(i);
                if (hook instanceof SendMessageTraceHookImpl) {
                    TraceDispatcher traceDispatcher = getQuietly(hook, FIELD_TRACE_DISPATCHER);
                    if (oldDispatcher == traceDispatcher) {
                        hooks.remove(i);
                    }
                }
            }
            newProducerImpl.registerSendMessageHook(new SendMessageTraceHookImpl(oldDispatcher));
            newProducerImpl.registerEndTransactionHook(new EndTransactionTraceHookImpl(oldDispatcher));
            setValue(target, FIELD_TRACE_DISPATCHER, newDispatcher);
        }
    }
}
