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
package com.jd.live.agent.plugin.failover.jedis.v6.connection;

import lombok.Getter;
import org.springframework.data.redis.connection.*;

import java.util.List;

@SuppressWarnings({"NullableProblems"})
public class JedisConnectionAdapter<T extends RedisConnection> implements RedisConnection {

    @Getter
    protected final T delegate;

    protected volatile boolean closed;

    public JedisConnectionAdapter(T delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object getNativeConnection() {
        return delegate.getNativeConnection();
    }

    @Override
    public boolean isQueueing() {
        return delegate.isQueueing();
    }

    @Override
    public boolean isPipelined() {
        return delegate.isPipelined();
    }

    @Override
    public void openPipeline() {
        delegate.openPipeline();
    }

    @Override
    public List<Object> closePipeline() throws RedisPipelineException {
        return delegate.closePipeline();
    }

    @Override
    public RedisSentinelConnection getSentinelConnection() {
        return delegate.getSentinelConnection();
    }

    @Override
    public Object execute(String command, byte[]... args) {
        return delegate.execute(command, args);
    }

    @Override
    public RedisCommands commands() {
        return delegate.commands();
    }

    @Override
    public RedisGeoCommands geoCommands() {
        return delegate.geoCommands();
    }

    @Override
    public RedisHashCommands hashCommands() {
        return delegate.hashCommands();
    }

    @Override
    public RedisHyperLogLogCommands hyperLogLogCommands() {
        return delegate.hyperLogLogCommands();
    }

    @Override
    public RedisKeyCommands keyCommands() {
        return delegate.keyCommands();
    }

    @Override
    public RedisListCommands listCommands() {
        return delegate.listCommands();
    }

    @Override
    public RedisSetCommands setCommands() {
        return delegate.setCommands();
    }

    @Override
    public RedisScriptingCommands scriptingCommands() {
        return delegate.scriptingCommands();
    }

    @Override
    public RedisServerCommands serverCommands() {
        return delegate.serverCommands();
    }

    @Override
    public RedisStreamCommands streamCommands() {
        return delegate.streamCommands();
    }

    @Override
    public RedisStringCommands stringCommands() {
        return delegate.stringCommands();
    }

    @Override
    public RedisZSetCommands zSetCommands() {
        return delegate.zSetCommands();
    }

    @Override
    public void select(int dbIndex) {
        delegate.select(dbIndex);
    }

    @Override
    public byte[] echo(byte[] message) {
        return delegate.echo(message);
    }

    @Override
    public String ping() {
        return delegate.ping();
    }

    @Override
    public boolean isSubscribed() {
        return delegate.isSubscribed();
    }

    @Override
    public Subscription getSubscription() {
        return delegate.getSubscription();
    }

    @Override
    public Long publish(byte[] channel, byte[] message) {
        return delegate.publish(channel, message);
    }

    @Override
    public void subscribe(MessageListener listener, byte[]... channels) {
        delegate.subscribe(listener, channels);
    }

    @Override
    public void pSubscribe(MessageListener listener, byte[]... patterns) {
        delegate.pSubscribe(listener, patterns);
    }

    @Override
    public void multi() {
        delegate.multi();
    }

    @Override
    public List<Object> exec() {
        return delegate.exec();
    }

    @Override
    public void discard() {
        delegate.discard();
    }

    @Override
    public void watch(byte[]... keys) {
        delegate.watch(keys);
    }

    @Override
    public void unwatch() {
        delegate.unwatch();
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            doClose();
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    protected void doClose() {
        delegate.close();
    }

}
