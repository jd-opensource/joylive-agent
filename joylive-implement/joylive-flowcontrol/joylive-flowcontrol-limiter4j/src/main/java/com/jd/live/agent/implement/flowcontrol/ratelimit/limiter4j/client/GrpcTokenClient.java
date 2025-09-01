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
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;
import com.jd.live.agent.implement.flowcontrol.ratelimit.limiter4j.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * gRPC token client implementation
 *
 * @since 1.6.0
 */
public class GrpcTokenClient {

    private static final Logger logger = LoggerFactory.getLogger(GrpcTokenClient.class);
    @Getter
    private final GrpcConfig config;
    private final ManagedChannel channel;
    private final TokenBucketServiceGrpc.TokenBucketServiceBlockingStub blockingStub;
    @Getter
    @Setter
    private volatile long lastAccessTime;

    public GrpcTokenClient(GrpcConfig config) {
        this.config = config;
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
        this.blockingStub = TokenBucketServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);

        logger.info("Created gRPC token client for address: {}", config.getAddress());
    }

    /**
     * Create token bucket
     *
     * @param policy Rate limit policy
     * @param window Sliding window
     * @return Whether creation was successful
     */
    public String createTokenBucket(RateLimitPolicy policy, SlidingWindow window) {
        try {
            CreateTokenBucketRequest.Builder requestBuilder = CreateTokenBucketRequest.newBuilder()
                    .setRate(window.getThreshold())
                    .setTimeWindowMs(window.getTimeWindowInMs());
            if (policy.getParameters() != null) {
                Map<String, String> parameters = new HashMap<>();
                policy.getParameters().forEach((key, value) -> {
                    if (value != null) {
                        parameters.put(key, value.toString());
                    }
                });
                // TODO add application name
                requestBuilder.putAllParameters(parameters);
            }
            CreateTokenBucketRequest request = requestBuilder.build();

            CreateTokenBucketResponse response = blockingStub.createTokenBucket(request);
            if (response.getSuccess()) {
                logger.info("Successfully created token bucket with ID: {} for policy: {}", response.getBucketId(), policy.getId());
                return response.getBucketId();
            } else {
                logger.error("Failed to create token bucket for policy: {}, message: {}", policy.getId(), response.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error creating token bucket for policy: {}", policy.getId(), e);
        }
        return null;
    }

    /**
     * Acquire tokens
     *
     * @param permits Number of tokens requested
     * @return Whether tokens were successfully acquired
     */
    public boolean acquireTokens(String bucketId, int permits, long timeoutMs) {
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
            AcquireTokensResponse response = blockingStub.acquireTokens(request);
            return response.getSuccess();
        } catch (Exception e) {
            logger.error("Error acquiring tokens for bucket: {}, tokens: {}", bucketId, permits, e);
            return true; // Default to allow in case of exception
        }
    }

    /**
     * Delete token bucket
     *
     * @return Whether deletion was successful
     */
    public boolean deleteTokenBucket(String bucketId) {
        try {
            DeleteTokenBucketRequest request = DeleteTokenBucketRequest.newBuilder().setBucketId(bucketId).build();
            DeleteTokenBucketResponse response = blockingStub.deleteTokenBucket(request);
            if (response.getSuccess()) {
                logger.info("Successfully deleted token bucket: {}", bucketId);
                return true;
            } else {
                logger.error("Failed to delete token bucket: {}, message: {}", bucketId, response.getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error deleting token bucket: {}", bucketId, e);
            return false;
        }
    }


    /**
     * Close client
     */
    public void close() {
        // Close channel
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
        logger.info("gRPC token client closed for address: {}", config.getAddress());
    }

    /**
     * Check connection status
     */
    public boolean isConnected() {
        return channel != null && !channel.isShutdown();
    }

}