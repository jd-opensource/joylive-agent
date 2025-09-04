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
package com.jd.live.agent.implement.flowcontrol.ratelimit.limiter4j.client;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.implement.flowcontrol.ratelimit.limiter4j.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * gRPC token client implementation
 *
 * @since 1.6.0
 */
public class GrpcTokenClient {

    private static final Logger logger = LoggerFactory.getLogger(GrpcTokenClient.class);
    @Getter
    private final GrpcConfig config;
    private final Consumer<GrpcTokenClient> consumer;
    private final ManagedChannel channel;
    private final TokenBucketServiceGrpc.TokenBucketServiceBlockingStub blockingStub;
    private final AtomicLong counter = new AtomicLong(1);
    @Getter
    @Setter
    private volatile long lastAccessTime;

    public GrpcTokenClient(GrpcConfig config, Consumer<GrpcTokenClient> consumer) {
        this.config = config;
        this.consumer = consumer;
        this.lastAccessTime = System.currentTimeMillis();

        // Create gRPC channel
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(config.getHost(), config.getPort()).usePlaintext();
        // Configure connection parameters
        if (channelBuilder instanceof NettyChannelBuilder) {
            NettyChannelBuilder nettyBuilder = (NettyChannelBuilder) channelBuilder;
            nettyBuilder.keepAliveTime(config.getKeepAliveTimeMs(), TimeUnit.MILLISECONDS)
                    .keepAliveTimeout(config.getKeepAliveTimeoutMs(), TimeUnit.MILLISECONDS)
                    .keepAliveWithoutCalls(true)
                    .maxInboundMessageSize(1024 * 1024); // 1MB
        }

        this.channel = channelBuilder.build();
        this.blockingStub = TokenBucketServiceGrpc.newBlockingStub(channel);

        logger.info("Created gRPC token client for address: {}", config.getAddress());
    }

    public String getAddress() {
        return config.getAddress();
    }

    /**
     * Creates a token bucket with specified configuration.
     *
     * @param name         the token bucket name
     * @param rate         the token generation rate
     * @param timeWindowMs the time window in milliseconds
     * @param parameters   additional configuration parameters, may be null
     * @return the bucket ID if successful, null otherwise
     */
    public String createTokenBucket(String name, int rate, long timeWindowMs, Map<String, String> parameters) {
        try {
            CreateTokenBucketRequest.Builder requestBuilder = CreateTokenBucketRequest.newBuilder()
                    .setName(name)
                    .setRate(rate)
                    .setTimeWindowMs(timeWindowMs);
            if (parameters != null) {
                requestBuilder.putAllParameters(parameters);
            }
            CreateTokenBucketRequest request = requestBuilder.build();
            CreateTokenBucketResponse response = blockingStub.createTokenBucket(request);
            if (response.getSuccess()) {
                logger.info("Successfully created token bucket with ID: {} for {}", response.getBucketId(), name);
                return response.getBucketId();
            } else {
                logger.error("Failed to create token bucket for {}, message: {}", name, response.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error creating token bucket for {}", name, e);
        }
        return null;
    }

    /**
     * Acquire tokens
     *
     * @param permits Number of tokens requested
     * @return Whether tokens were successfully acquired
     */
    public boolean acquireTokens(final String bucketId, final int permits, final long timeoutMs) {
        if (bucketId == null) {
            // Default to allow when token bucket is not created
            logger.warn("Token bucket not created, cannot acquire tokens");
            return true;
        }
        try {
            AcquireTokensRequest request = AcquireTokensRequest.newBuilder()
                    .setBucketId(bucketId)
                    .setTokens(permits)
                    .setTimeoutMs(timeoutMs)
                    .build();
            AcquireTokensResponse response = timeoutMs > 0
                    ? blockingStub.withDeadlineAfter(Duration.ofMillis(timeoutMs)).acquireTokens(request)
                    : blockingStub.acquireTokens(request);
            return response.getSuccess();
        } catch (Exception e) {
            logger.error("Error acquiring tokens for bucket: {}, tokens: {}", bucketId, permits, e);
            return true; // Default to allow in case of exception
        }
    }

    /**
     * Checks if the endpoint is considered useless based on the counter value.
     *
     * @return true if the counter is zero, indicating the endpoint is useless; false otherwise
     */
    public boolean isUseless() {
        return counter.get() == 0;
    }

    /**
     * Close client
     */
    public void close() {
        if (counter.decrementAndGet() == 0) {
            consumer.accept(this);
        }
    }

    /**
     * Checks if the Redis client has expired based on the specified timeout.
     *
     * @param timeout the timeout duration in milliseconds
     * @return true if the client has expired, false otherwise
     */
    public boolean isExpired(long timeout) {
        return isUseless() && System.currentTimeMillis() - lastAccessTime >= timeout;
    }

    /**
     * Check connection status
     */
    public boolean isConnected() {
        return channel != null && !channel.isShutdown();
    }

    /**
     * Shuts down the Redis client.
     * This method should be called when the client is no longer needed to release resources.
     */
    protected void shutdown() {
        // Close channel
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
        logger.info("gRPC token client closed for address: {}", config.getAddress());
    }

}