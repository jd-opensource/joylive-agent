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

import com.jd.live.agent.governance.util.network.ClusterRedirect;
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

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.setValue;

public class ProducerClient extends AbstractMQClient<DefaultMQProducer> {

    private static final String FIELD_PRODUCER_IMPL = "defaultMQProducerImpl";

    public ProducerClient(DefaultMQProducer producer, ClusterRedirect address) {
        super(producer, address);
    }

    @Override
    protected String getType() {
        return "producer";
    }

    @Override
    protected void doClose() {
        target.shutdown();
    }

    @Override
    protected void doStart() throws MQClientException {
        // reset to restart
        reset();
        target.start();
    }

    /**
     * Replaces producer instance while keeping RPC and tracing intact.
     */
    protected void reset() {
        DefaultMQProducerImpl producerImpl = getQuietly(target, FIELD_PRODUCER_IMPL);
        RPCHook rpcHook = getQuietly(producerImpl, FIELD_RPC_HOOK);
        producerImpl = new DefaultMQProducerImpl(target, rpcHook);
        setValue(target, FIELD_PRODUCER_IMPL, producerImpl);
        resetTrace(rpcHook, producerImpl);
    }

    /**
     * Replaces the trace dispatcher if it exists and is of type AsyncTraceDispatcher.
     *
     * @param rpcHook      original hook to preserve
     * @param producerImpl new producer instance to associate
     */
    private void resetTrace(RPCHook rpcHook, DefaultMQProducerImpl producerImpl) {
        TraceDispatcher dispatcher = target.getTraceDispatcher();
        if (dispatcher instanceof AsyncTraceDispatcher) {
            // create new trace dispatcher
            setValue(target, FIELD_TRACE_DISPATCHER, createTraceDispatcher((AsyncTraceDispatcher) dispatcher, rpcHook, producerImpl));
        }
    }

    /**
     * Creates a new trace dispatcher instance and migrates tracing hooks.
     *
     * @param dispatcher   old dispatcher instance (for configuration)
     * @param rpcHook      original hook to preserve
     * @param producerImpl new producer to associate
     * @return new AsyncTraceDispatcher instance
     */
    private TraceDispatcher createTraceDispatcher(AsyncTraceDispatcher dispatcher, RPCHook rpcHook, DefaultMQProducerImpl producerImpl) {
        AsyncTraceDispatcher result = new AsyncTraceDispatcher(target.getProducerGroup(), TraceDispatcher.Type.PRODUCE, dispatcher.getTraceTopicName(), rpcHook);
        result.setHostProducer(producerImpl);
        List<SendMessageHook> hooks = getQuietly(producerImpl, "sendMessageHookList");
        for (int i = hooks.size() - 1; i >= 0; i--) {
            SendMessageHook hook = hooks.get(i);
            if (hook instanceof SendMessageTraceHookImpl) {
                TraceDispatcher traceDispatcher = getQuietly(hook, FIELD_TRACE_DISPATCHER);
                if (dispatcher == traceDispatcher) {
                    hooks.remove(i);
                }
            }
        }
        producerImpl.registerSendMessageHook(new SendMessageTraceHookImpl(dispatcher));
        producerImpl.registerEndTransactionHook(new EndTransactionTraceHookImpl(dispatcher));
        return result;
    }
}
