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
package com.jd.live.agent.plugin.failover.jedis.v3.connection;

import lombok.Getter;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.core.types.RedisClientInfo;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("NullableProblems")
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
    public Long geoAdd(byte[] key, Point point, byte[] member) {
        return delegate.geoAdd(key, point, member);
    }

    @Override
    public Long geoAdd(byte[] key, Map<byte[], Point> memberCoordinateMap) {
        return delegate.geoAdd(key, memberCoordinateMap);
    }

    @Override
    public Long geoAdd(byte[] key, Iterable<GeoLocation<byte[]>> locations) {
        return delegate.geoAdd(key, locations);
    }

    @Override
    public Distance geoDist(byte[] key, byte[] member1, byte[] member2) {
        return delegate.geoDist(key, member1, member2);
    }

    @Override
    public Distance geoDist(byte[] key, byte[] member1, byte[] member2, Metric metric) {
        return delegate.geoDist(key, member1, member2, metric);
    }

    @Override
    public List<String> geoHash(byte[] key, byte[]... members) {
        return delegate.geoHash(key, members);
    }

    @Override
    public List<Point> geoPos(byte[] key, byte[]... members) {
        return delegate.geoPos(key, members);
    }

    @Deprecated
    @Override
    public GeoResults<GeoLocation<byte[]>> geoRadius(byte[] key, Circle within) {
        return delegate.geoRadius(key, within);
    }

    @Deprecated
    @Override
    public GeoResults<GeoLocation<byte[]>> geoRadius(byte[] key, Circle within, GeoRadiusCommandArgs args) {
        return delegate.geoRadius(key, within, args);
    }

    @Override
    public GeoResults<GeoLocation<byte[]>> geoRadiusByMember(byte[] key, byte[] member, Distance radius) {
        return delegate.geoRadiusByMember(key, member, radius);
    }

    @Override
    public GeoResults<GeoLocation<byte[]>> geoRadiusByMember(byte[] key, byte[] member, Distance radius, GeoRadiusCommandArgs args) {
        return delegate.geoRadiusByMember(key, member, radius, args);
    }

    @Override
    public Long geoRemove(byte[] key, byte[]... members) {
        return delegate.geoRemove(key, members);
    }

    @Override
    public GeoResults<GeoLocation<byte[]>> geoSearch(byte[] key, GeoReference<byte[]> reference, GeoShape predicate, GeoSearchCommandArgs args) {
        return delegate.geoSearch(key, reference, predicate, args);
    }

    @Override
    public Long geoSearchStore(byte[] destKey, byte[] key, GeoReference<byte[]> reference, GeoShape predicate, GeoSearchStoreCommandArgs args) {
        return delegate.geoSearchStore(destKey, key, reference, predicate, args);
    }

    @Override
    public Boolean hSet(byte[] key, byte[] field, byte[] value) {
        return delegate.hSet(key, field, value);
    }

    @Override
    public Boolean hSetNX(byte[] key, byte[] field, byte[] value) {
        return delegate.hSetNX(key, field, value);
    }

    @Override
    public byte[] hGet(byte[] key, byte[] field) {
        return delegate.hGet(key, field);
    }

    @Override
    public List<byte[]> hMGet(byte[] key, byte[]... fields) {
        return delegate.hMGet(key, fields);
    }

    @Override
    public void hMSet(byte[] key, Map<byte[], byte[]> hashes) {
        delegate.hMSet(key, hashes);
    }

    @Override
    public Long hIncrBy(byte[] key, byte[] field, long delta) {
        return delegate.hIncrBy(key, field, delta);
    }

    @Override
    public Double hIncrBy(byte[] key, byte[] field, double delta) {
        return delegate.hIncrBy(key, field, delta);
    }

    @Override
    public Boolean hExists(byte[] key, byte[] field) {
        return delegate.hExists(key, field);
    }

    @Override
    public Long hDel(byte[] key, byte[]... fields) {
        return delegate.hDel(key, fields);
    }

    @Override
    public Long hLen(byte[] key) {
        return delegate.hLen(key);
    }

    @Override
    public Set<byte[]> hKeys(byte[] key) {
        return delegate.hKeys(key);
    }

    @Override
    public List<byte[]> hVals(byte[] key) {
        return delegate.hVals(key);
    }

    @Override
    public Map<byte[], byte[]> hGetAll(byte[] key) {
        return delegate.hGetAll(key);
    }

    @Override
    public byte[] hRandField(byte[] key) {
        return delegate.hRandField(key);
    }

    @Override
    public Map.Entry<byte[], byte[]> hRandFieldWithValues(byte[] key) {
        return delegate.hRandFieldWithValues(key);
    }

    @Override
    public List<byte[]> hRandField(byte[] key, long count) {
        return delegate.hRandField(key, count);
    }

    @Override
    public List<Map.Entry<byte[], byte[]>> hRandFieldWithValues(byte[] key, long count) {
        return delegate.hRandFieldWithValues(key, count);
    }

    @Override
    public Cursor<Map.Entry<byte[], byte[]>> hScan(byte[] key, ScanOptions options) {
        return delegate.hScan(key, options);
    }

    @Override
    public Long hStrLen(byte[] key, byte[] field) {
        return delegate.hStrLen(key, field);
    }

    @Override
    public Long pfAdd(byte[] key, byte[]... values) {
        return delegate.pfAdd(key, values);
    }

    @Override
    public Long pfCount(byte[]... keys) {
        return delegate.pfCount(keys);
    }

    @Override
    public void pfMerge(byte[] destinationKey, byte[]... sourceKeys) {
        delegate.pfMerge(destinationKey, sourceKeys);
    }

    @Override
    public Boolean copy(byte[] sourceKey, byte[] targetKey, boolean replace) {
        return delegate.copy(sourceKey, targetKey, replace);
    }

    @Override
    public Long exists(byte[]... keys) {
        return delegate.exists(keys);
    }

    @Override
    public Long del(byte[]... keys) {
        return delegate.del(keys);
    }

    @Override
    public Long unlink(byte[]... keys) {
        return delegate.unlink(keys);
    }

    @Override
    public DataType type(byte[] key) {
        return delegate.type(key);
    }

    @Override
    public Long touch(byte[]... keys) {
        return delegate.touch(keys);
    }

    @Override
    public Set<byte[]> keys(byte[] pattern) {
        return delegate.keys(pattern);
    }

    @Override
    public Cursor<byte[]> scan(ScanOptions options) {
        return delegate.scan(options);
    }

    @Override
    public byte[] randomKey() {
        return delegate.randomKey();
    }

    @Override
    public void rename(byte[] oldKey, byte[] newKey) {
        delegate.rename(oldKey, newKey);
    }

    @Override
    public Boolean renameNX(byte[] oldKey, byte[] newKey) {
        return delegate.renameNX(oldKey, newKey);
    }

    @Override
    public Boolean expire(byte[] key, long seconds) {
        return delegate.expire(key, seconds);
    }

    @Override
    public Boolean pExpire(byte[] key, long millis) {
        return delegate.pExpire(key, millis);
    }

    @Override
    public Boolean expireAt(byte[] key, long unixTime) {
        return delegate.expireAt(key, unixTime);
    }

    @Override
    public Boolean pExpireAt(byte[] key, long unixTimeInMillis) {
        return delegate.pExpireAt(key, unixTimeInMillis);
    }

    @Override
    public Boolean persist(byte[] key) {
        return delegate.persist(key);
    }

    @Override
    public Boolean move(byte[] key, int dbIndex) {
        return delegate.move(key, dbIndex);
    }

    @Override
    public Long ttl(byte[] key) {
        return delegate.ttl(key);
    }

    @Override
    public Long ttl(byte[] key, TimeUnit timeUnit) {
        return delegate.ttl(key, timeUnit);
    }

    @Override
    public Long pTtl(byte[] key) {
        return delegate.pTtl(key);
    }

    @Override
    public Long pTtl(byte[] key, TimeUnit timeUnit) {
        return delegate.pTtl(key, timeUnit);
    }

    @Override
    public List<byte[]> sort(byte[] key, SortParameters params) {
        return delegate.sort(key, params);
    }

    @Override
    public Long sort(byte[] key, SortParameters params, byte[] storeKey) {
        return delegate.sort(key, params, storeKey);
    }

    @Override
    public byte[] dump(byte[] key) {
        return delegate.dump(key);
    }

    @Override
    public void restore(byte[] key, long ttlInMillis, byte[] serializedValue, boolean replace) {
        delegate.restore(key, ttlInMillis, serializedValue, replace);
    }

    @Override
    public ValueEncoding encodingOf(byte[] key) {
        return delegate.encodingOf(key);
    }

    @Override
    public Duration idletime(byte[] key) {
        return delegate.idletime(key);
    }

    @Override
    public Long refcount(byte[] key) {
        return delegate.refcount(key);
    }

    @Override
    public Long rPush(byte[] key, byte[]... values) {
        return delegate.rPush(key, values);
    }

    @Override
    public List<Long> lPos(byte[] key, byte[] element, Integer rank, Integer count) {
        return delegate.lPos(key, element, rank, count);
    }

    @Override
    public Long lPush(byte[] key, byte[]... values) {
        return delegate.lPush(key, values);
    }

    @Override
    public Long rPushX(byte[] key, byte[] value) {
        return delegate.rPushX(key, value);
    }

    @Override
    public Long lPushX(byte[] key, byte[] value) {
        return delegate.lPushX(key, value);
    }

    @Override
    public Long lLen(byte[] key) {
        return delegate.lLen(key);
    }

    @Override
    public List<byte[]> lRange(byte[] key, long start, long end) {
        return delegate.lRange(key, start, end);
    }

    @Override
    public void lTrim(byte[] key, long start, long end) {
        delegate.lTrim(key, start, end);
    }

    @Override
    public byte[] lIndex(byte[] key, long index) {
        return delegate.lIndex(key, index);
    }

    @Override
    public Long lInsert(byte[] key, Position where, byte[] pivot, byte[] value) {
        return delegate.lInsert(key, where, pivot, value);
    }

    @Override
    public byte[] lMove(byte[] sourceKey, byte[] destinationKey, Direction from, Direction to) {
        return delegate.lMove(sourceKey, destinationKey, from, to);
    }

    @Override
    public byte[] bLMove(byte[] sourceKey, byte[] destinationKey, Direction from, Direction to, double timeout) {
        return delegate.bLMove(sourceKey, destinationKey, from, to, timeout);
    }

    @Override
    public void lSet(byte[] key, long index, byte[] value) {
        delegate.lSet(key, index, value);
    }

    @Override
    public Long lRem(byte[] key, long count, byte[] value) {
        return delegate.lRem(key, count, value);
    }

    @Override
    public byte[] lPop(byte[] key) {
        return delegate.lPop(key);
    }

    @Override
    public List<byte[]> lPop(byte[] key, long count) {
        return delegate.lPop(key, count);
    }

    @Override
    public byte[] rPop(byte[] key) {
        return delegate.rPop(key);
    }

    @Override
    public List<byte[]> rPop(byte[] key, long count) {
        return delegate.rPop(key, count);
    }

    @Override
    public List<byte[]> bLPop(int timeout, byte[]... keys) {
        return delegate.bLPop(timeout, keys);
    }

    @Override
    public List<byte[]> bRPop(int timeout, byte[]... keys) {
        return delegate.bRPop(timeout, keys);
    }

    @Override
    public byte[] rPopLPush(byte[] srcKey, byte[] dstKey) {
        return delegate.rPopLPush(srcKey, dstKey);
    }

    @Override
    public byte[] bRPopLPush(int timeout, byte[] srcKey, byte[] dstKey) {
        return delegate.bRPopLPush(timeout, srcKey, dstKey);
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
    public void scriptFlush() {
        delegate.scriptFlush();
    }

    @Override
    public void scriptKill() {
        delegate.scriptKill();
    }

    @Override
    public String scriptLoad(byte[] script) {
        return delegate.scriptLoad(script);
    }

    @Override
    public List<Boolean> scriptExists(String... scriptShas) {
        return delegate.scriptExists(scriptShas);
    }

    @Override
    public <T> T eval(byte[] script, ReturnType returnType, int numKeys, byte[]... keysAndArgs) {
        return delegate.eval(script, returnType, numKeys, keysAndArgs);
    }

    @Override
    public <T> T evalSha(String scriptSha, ReturnType returnType, int numKeys, byte[]... keysAndArgs) {
        return delegate.evalSha(scriptSha, returnType, numKeys, keysAndArgs);
    }

    @Override
    public <T> T evalSha(byte[] scriptSha, ReturnType returnType, int numKeys, byte[]... keysAndArgs) {
        return delegate.evalSha(scriptSha, returnType, numKeys, keysAndArgs);
    }

    @Override
    public void bgReWriteAof() {
        delegate.bgReWriteAof();
    }

    @Override
    public void bgSave() {
        delegate.bgSave();
    }

    @Override
    public Long lastSave() {
        return delegate.lastSave();
    }

    @Override
    public void save() {
        delegate.save();
    }

    @Override
    public Long dbSize() {
        return delegate.dbSize();
    }

    @Override
    public void flushDb() {
        delegate.flushDb();
    }

    @Override
    public void flushDb(FlushOption option) {
        delegate.flushDb(option);
    }

    @Override
    public void flushAll() {
        delegate.flushAll();
    }

    @Override
    public void flushAll(FlushOption option) {
        delegate.flushAll(option);
    }

    @Override
    public Properties info() {
        return delegate.info();
    }

    @Override
    public Properties info(String section) {
        return delegate.info(section);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public void shutdown(ShutdownOption option) {
        delegate.shutdown(option);
    }

    @Override
    public Properties getConfig(String pattern) {
        return delegate.getConfig(pattern);
    }

    @Override
    public void setConfig(String param, String value) {
        delegate.setConfig(param, value);
    }

    @Override
    public void resetConfigStats() {
        delegate.resetConfigStats();
    }

    @Override
    public void rewriteConfig() {
        delegate.rewriteConfig();
    }

    @Override
    public Long time(TimeUnit timeUnit) {
        return delegate.time(timeUnit);
    }

    @Override
    public void killClient(String host, int port) {
        delegate.killClient(host, port);
    }

    @Override
    public void setClientName(byte[] name) {
        delegate.setClientName(name);
    }

    @Override
    public String getClientName() {
        return delegate.getClientName();
    }

    @Override
    public List<RedisClientInfo> getClientList() {
        return delegate.getClientList();
    }

    @Override
    public void slaveOf(String host, int port) {
        delegate.slaveOf(host, port);
    }

    @Override
    public void slaveOfNoOne() {
        delegate.slaveOfNoOne();
    }

    @Override
    public void migrate(byte[] key, RedisNode target, int dbIndex, MigrateOption option) {
        delegate.migrate(key, target, dbIndex, option);
    }

    @Override
    public void migrate(byte[] key, RedisNode target, int dbIndex, MigrateOption option, long timeout) {
        delegate.migrate(key, target, dbIndex, option, timeout);
    }

    @Override
    public Long sAdd(byte[] key, byte[]... values) {
        return delegate.sAdd(key, values);
    }

    @Override
    public Long sRem(byte[] key, byte[]... values) {
        return delegate.sRem(key, values);
    }

    @Override
    public byte[] sPop(byte[] key) {
        return delegate.sPop(key);
    }

    @Override
    public List<byte[]> sPop(byte[] key, long count) {
        return delegate.sPop(key, count);
    }

    @Override
    public Boolean sMove(byte[] srcKey, byte[] destKey, byte[] value) {
        return delegate.sMove(srcKey, destKey, value);
    }

    @Override
    public Long sCard(byte[] key) {
        return delegate.sCard(key);
    }

    @Override
    public Boolean sIsMember(byte[] key, byte[] value) {
        return delegate.sIsMember(key, value);
    }

    @Override
    public List<Boolean> sMIsMember(byte[] key, byte[]... values) {
        return delegate.sMIsMember(key, values);
    }

    @Override
    public Set<byte[]> sDiff(byte[]... keys) {
        return delegate.sDiff(keys);
    }

    @Override
    public Long sDiffStore(byte[] destKey, byte[]... keys) {
        return delegate.sDiffStore(destKey, keys);
    }

    @Override
    public Set<byte[]> sInter(byte[]... keys) {
        return delegate.sInter(keys);
    }

    @Override
    public Long sInterStore(byte[] destKey, byte[]... keys) {
        return delegate.sInterStore(destKey, keys);
    }

    @Override
    public Set<byte[]> sUnion(byte[]... keys) {
        return delegate.sUnion(keys);
    }

    @Override
    public Long sUnionStore(byte[] destKey, byte[]... keys) {
        return delegate.sUnionStore(destKey, keys);
    }

    @Override
    public Set<byte[]> sMembers(byte[] key) {
        return delegate.sMembers(key);
    }

    @Override
    public byte[] sRandMember(byte[] key) {
        return delegate.sRandMember(key);
    }

    @Override
    public List<byte[]> sRandMember(byte[] key, long count) {
        return delegate.sRandMember(key, count);
    }

    @Override
    public Cursor<byte[]> sScan(byte[] key, ScanOptions options) {
        return delegate.sScan(key, options);
    }

    @Override
    public Long xAck(byte[] key, String group, RecordId... recordIds) {
        return delegate.xAck(key, group, recordIds);
    }

    @Override
    public RecordId xAdd(MapRecord<byte[], byte[], byte[]> record, XAddOptions options) {
        return delegate.xAdd(record, options);
    }

    @Override
    public List<RecordId> xClaimJustId(byte[] key, String group, String newOwner, XClaimOptions options) {
        return delegate.xClaimJustId(key, group, newOwner, options);
    }

    @Override
    public List<ByteRecord> xClaim(byte[] key, String group, String newOwner, XClaimOptions options) {
        return delegate.xClaim(key, group, newOwner, options);
    }

    @Override
    public Long xDel(byte[] key, RecordId... recordIds) {
        return delegate.xDel(key, recordIds);
    }

    @Override
    public String xGroupCreate(byte[] key, String groupName, ReadOffset readOffset) {
        return delegate.xGroupCreate(key, groupName, readOffset);
    }

    @Override
    public String xGroupCreate(byte[] key, String groupName, ReadOffset readOffset, boolean mkStream) {
        return delegate.xGroupCreate(key, groupName, readOffset, mkStream);
    }

    @Override
    public Boolean xGroupDelConsumer(byte[] key, Consumer consumer) {
        return delegate.xGroupDelConsumer(key, consumer);
    }

    @Override
    public Boolean xGroupDestroy(byte[] key, String groupName) {
        return delegate.xGroupDestroy(key, groupName);
    }

    @Override
    public StreamInfo.XInfoStream xInfo(byte[] key) {
        return delegate.xInfo(key);
    }

    @Override
    public StreamInfo.XInfoGroups xInfoGroups(byte[] key) {
        return delegate.xInfoGroups(key);
    }

    @Override
    public StreamInfo.XInfoConsumers xInfoConsumers(byte[] key, String groupName) {
        return delegate.xInfoConsumers(key, groupName);
    }

    @Override
    public Long xLen(byte[] key) {
        return delegate.xLen(key);
    }

    @Override
    public PendingMessagesSummary xPending(byte[] key, String groupName) {
        return delegate.xPending(key, groupName);
    }

    @Override
    public PendingMessages xPending(byte[] key, String groupName, XPendingOptions options) {
        return delegate.xPending(key, groupName, options);
    }

    @Override
    public List<ByteRecord> xRange(byte[] key, org.springframework.data.domain.Range<String> range, Limit limit) {
        return delegate.xRange(key, range, limit);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ByteRecord> xRead(StreamReadOptions readOptions, StreamOffset<byte[]>... streams) {
        return delegate.xRead(readOptions, streams);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ByteRecord> xReadGroup(Consumer consumer, StreamReadOptions readOptions, StreamOffset<byte[]>... streams) {
        return delegate.xReadGroup(consumer, readOptions, streams);
    }

    @Override
    public List<ByteRecord> xRevRange(byte[] key, org.springframework.data.domain.Range<String> range, Limit limit) {
        return delegate.xRevRange(key, range, limit);
    }

    @Override
    public Long xTrim(byte[] key, long count) {
        return delegate.xTrim(key, count);
    }

    @Override
    public Long xTrim(byte[] key, long count, boolean approximateTrimming) {
        return delegate.xTrim(key, count, approximateTrimming);
    }

    @Override
    public byte[] get(byte[] key) {
        return delegate.get(key);
    }

    @Override
    public byte[] getDel(byte[] key) {
        return delegate.getDel(key);
    }

    @Override
    public byte[] getEx(byte[] key, Expiration expiration) {
        return delegate.getEx(key, expiration);
    }

    @Override
    public byte[] getSet(byte[] key, byte[] value) {
        return delegate.getSet(key, value);
    }

    @Override
    public List<byte[]> mGet(byte[]... keys) {
        return delegate.mGet(keys);
    }

    @Override
    public Boolean set(byte[] key, byte[] value) {
        return delegate.set(key, value);
    }

    @Override
    public Boolean set(byte[] key, byte[] value, Expiration expiration, SetOption option) {
        return delegate.set(key, value, expiration, option);
    }

    @Override
    public Boolean setNX(byte[] key, byte[] value) {
        return delegate.setNX(key, value);
    }

    @Override
    public Boolean setEx(byte[] key, long seconds, byte[] value) {
        return delegate.setEx(key, seconds, value);
    }

    @Override
    public Boolean pSetEx(byte[] key, long milliseconds, byte[] value) {
        return delegate.pSetEx(key, milliseconds, value);
    }

    @Override
    public Boolean mSet(Map<byte[], byte[]> tuple) {
        return delegate.mSet(tuple);
    }

    @Override
    public Boolean mSetNX(Map<byte[], byte[]> tuple) {
        return delegate.mSetNX(tuple);
    }

    @Override
    public Long incr(byte[] key) {
        return delegate.incr(key);
    }

    @Override
    public Long incrBy(byte[] key, long value) {
        return delegate.incrBy(key, value);
    }

    @Override
    public Double incrBy(byte[] key, double value) {
        return delegate.incrBy(key, value);
    }

    @Override
    public Long decr(byte[] key) {
        return delegate.decr(key);
    }

    @Override
    public Long decrBy(byte[] key, long value) {
        return delegate.decrBy(key, value);
    }

    @Override
    public Long append(byte[] key, byte[] value) {
        return delegate.append(key, value);
    }

    @Override
    public byte[] getRange(byte[] key, long start, long end) {
        return delegate.getRange(key, start, end);
    }

    @Override
    public void setRange(byte[] key, byte[] value, long offset) {
        delegate.setRange(key, value, offset);
    }

    @Override
    public Boolean getBit(byte[] key, long offset) {
        return delegate.getBit(key, offset);
    }

    @Override
    public Boolean setBit(byte[] key, long offset, boolean value) {
        return delegate.setBit(key, offset, value);
    }

    @Override
    public Long bitCount(byte[] key) {
        return delegate.bitCount(key);
    }

    @Override
    public Long bitCount(byte[] key, long start, long end) {
        return delegate.bitCount(key, start, end);
    }

    @Override
    public List<Long> bitField(byte[] key, BitFieldSubCommands subCommands) {
        return delegate.bitField(key, subCommands);
    }

    @Override
    public Long bitOp(BitOperation op, byte[] destination, byte[]... keys) {
        return delegate.bitOp(op, destination, keys);
    }

    @Override
    public Long bitPos(byte[] key, boolean bit, org.springframework.data.domain.Range<Long> range) {
        return delegate.bitPos(key, bit, range);
    }

    @Override
    public Long strLen(byte[] key) {
        return delegate.strLen(key);
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
    public Boolean zAdd(byte[] key, double score, byte[] value, ZAddArgs args) {
        return delegate.zAdd(key, score, value, args);
    }

    @Override
    public Long zAdd(byte[] key, Set<Tuple> tuples, ZAddArgs args) {
        return delegate.zAdd(key, tuples, args);
    }

    @Override
    public Long zRem(byte[] key, byte[]... values) {
        return delegate.zRem(key, values);
    }

    @Override
    public Double zIncrBy(byte[] key, double increment, byte[] value) {
        return delegate.zIncrBy(key, increment, value);
    }

    @Override
    public byte[] zRandMember(byte[] key) {
        return delegate.zRandMember(key);
    }

    @Override
    public List<byte[]> zRandMember(byte[] key, long count) {
        return delegate.zRandMember(key, count);
    }

    @Override
    public Tuple zRandMemberWithScore(byte[] key) {
        return delegate.zRandMemberWithScore(key);
    }

    @Override
    public List<Tuple> zRandMemberWithScore(byte[] key, long count) {
        return delegate.zRandMemberWithScore(key, count);
    }

    @Override
    public Long zRank(byte[] key, byte[] value) {
        return delegate.zRank(key, value);
    }

    @Override
    public Long zRevRank(byte[] key, byte[] value) {
        return delegate.zRevRank(key, value);
    }

    @Override
    public Set<byte[]> zRange(byte[] key, long start, long end) {
        return delegate.zRange(key, start, end);
    }

    @Override
    public Set<Tuple> zRangeWithScores(byte[] key, long start, long end) {
        return delegate.zRangeWithScores(key, start, end);
    }

    @Override
    public Set<Tuple> zRangeByScoreWithScores(byte[] key, Range range, Limit limit) {
        return delegate.zRangeByScoreWithScores(key, range, limit);
    }

    @Override
    public Set<byte[]> zRevRange(byte[] key, long start, long end) {
        return delegate.zRevRange(key, start, end);
    }

    @Override
    public Set<Tuple> zRevRangeWithScores(byte[] key, long start, long end) {
        return delegate.zRevRangeWithScores(key, start, end);
    }

    @Override
    public Set<byte[]> zRevRangeByScore(byte[] key, Range range, Limit limit) {
        return delegate.zRevRangeByScore(key, range, limit);
    }

    @Override
    public Set<Tuple> zRevRangeByScoreWithScores(byte[] key, Range range, Limit limit) {
        return delegate.zRevRangeByScoreWithScores(key, range, limit);
    }

    @Override
    public Long zCount(byte[] key, Range range) {
        return delegate.zCount(key, range);
    }

    @Override
    public Long zLexCount(byte[] key, Range range) {
        return delegate.zLexCount(key, range);
    }

    @Override
    public Tuple zPopMin(byte[] key) {
        return delegate.zPopMin(key);
    }

    @Override
    public Set<Tuple> zPopMin(byte[] key, long count) {
        return delegate.zPopMin(key, count);
    }

    @Override
    public Tuple bZPopMin(byte[] key, long timeout, TimeUnit unit) {
        return delegate.bZPopMin(key, timeout, unit);
    }

    @Override
    public Tuple zPopMax(byte[] key) {
        return delegate.zPopMax(key);
    }

    @Override
    public Set<Tuple> zPopMax(byte[] key, long count) {
        return delegate.zPopMax(key, count);
    }

    @Override
    public Tuple bZPopMax(byte[] key, long timeout, TimeUnit unit) {
        return delegate.bZPopMax(key, timeout, unit);
    }

    @Override
    public Long zCard(byte[] key) {
        return delegate.zCard(key);
    }

    @Override
    public Double zScore(byte[] key, byte[] value) {
        return delegate.zScore(key, value);
    }

    @Override
    public List<Double> zMScore(byte[] key, byte[]... values) {
        return delegate.zMScore(key, values);
    }

    @Override
    public Long zRemRange(byte[] key, long start, long end) {
        return delegate.zRemRange(key, start, end);
    }

    @Override
    public Long zRemRangeByLex(byte[] key, Range range) {
        return delegate.zRemRangeByLex(key, range);
    }

    @Override
    public Long zRemRangeByScore(byte[] key, Range range) {
        return delegate.zRemRangeByScore(key, range);
    }

    @Override
    public Set<byte[]> zDiff(byte[]... sets) {
        return delegate.zDiff(sets);
    }

    @Override
    public Set<Tuple> zDiffWithScores(byte[]... sets) {
        return delegate.zDiffWithScores(sets);
    }

    @Override
    public Long zDiffStore(byte[] destKey, byte[]... sets) {
        return delegate.zDiffStore(destKey, sets);
    }

    @Override
    public Set<byte[]> zInter(byte[]... sets) {
        return delegate.zInter(sets);
    }

    @Override
    public Set<Tuple> zInterWithScores(byte[]... sets) {
        return delegate.zInterWithScores(sets);
    }

    @Override
    public Set<Tuple> zInterWithScores(Aggregate aggregate, Weights weights, byte[]... sets) {
        return delegate.zInterWithScores(aggregate, weights, sets);
    }

    @Override
    public Long zInterStore(byte[] destKey, byte[]... sets) {
        return delegate.zInterStore(destKey, sets);
    }

    @Override
    public Long zInterStore(byte[] destKey, Aggregate aggregate, Weights weights, byte[]... sets) {
        return delegate.zInterStore(destKey, aggregate, weights, sets);
    }

    @Override
    public Set<byte[]> zUnion(byte[]... sets) {
        return delegate.zUnion(sets);
    }

    @Override
    public Set<Tuple> zUnionWithScores(byte[]... sets) {
        return delegate.zUnionWithScores(sets);
    }

    @Override
    public Set<Tuple> zUnionWithScores(Aggregate aggregate, Weights weights, byte[]... sets) {
        return delegate.zUnionWithScores(aggregate, weights, sets);
    }

    @Override
    public Long zUnionStore(byte[] destKey, byte[]... sets) {
        return delegate.zUnionStore(destKey, sets);
    }

    @Override
    public Long zUnionStore(byte[] destKey, Aggregate aggregate, Weights weights, byte[]... sets) {
        return delegate.zUnionStore(destKey, aggregate, weights, sets);
    }

    @Override
    public Cursor<Tuple> zScan(byte[] key, ScanOptions options) {
        return delegate.zScan(key, options);
    }

    @Override
    public Set<byte[]> zRangeByScore(byte[] key, String min, String max, long offset, long count) {
        return delegate.zRangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<byte[]> zRangeByScore(byte[] key, Range range, Limit limit) {
        return delegate.zRangeByScore(key, range, limit);
    }

    @Override
    public Set<byte[]> zRangeByLex(byte[] key, Range range, Limit limit) {
        return delegate.zRangeByLex(key, range, limit);
    }

    @Override
    public Set<byte[]> zRevRangeByLex(byte[] key, Range range, Limit limit) {
        return delegate.zRevRangeByLex(key, range, limit);
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
