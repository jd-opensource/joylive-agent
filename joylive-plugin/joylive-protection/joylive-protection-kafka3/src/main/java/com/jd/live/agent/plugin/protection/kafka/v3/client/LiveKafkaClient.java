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
package com.jd.live.agent.plugin.protection.kafka.v3.client;

import org.apache.kafka.clients.ClientRequest;
import org.apache.kafka.clients.ClientResponse;
import org.apache.kafka.clients.KafkaClient;
import org.apache.kafka.clients.RequestCompletionHandler;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.requests.AbstractRequest;

import java.io.IOException;
import java.util.List;

public class LiveKafkaClient implements KafkaClient {

    private final KafkaClient delegate;

    private final String[] addresses;

    public LiveKafkaClient(KafkaClient delegate, String[] addresses) {
        this.delegate = delegate;
        this.addresses = addresses;
    }

    @Override
    public boolean isReady(Node node, long now) {
        return delegate.isReady(node, now);
    }

    @Override
    public boolean ready(Node node, long now) {
        return delegate.ready(node, now);
    }

    @Override
    public long connectionDelay(Node node, long now) {
        return delegate.connectionDelay(node, now);
    }

    @Override
    public long pollDelayMs(Node node, long now) {
        return delegate.pollDelayMs(node, now);
    }

    @Override
    public boolean connectionFailed(Node node) {
        return delegate.connectionFailed(node);
    }

    @Override
    public AuthenticationException authenticationException(Node node) {
        return delegate.authenticationException(node);
    }

    @Override
    public void send(ClientRequest request, long now) {
        delegate.send(request, now);
    }

    @Override
    public List<ClientResponse> poll(long timeout, long now) {
        return delegate.poll(timeout, now);
    }

    @Override
    public void disconnect(String nodeId) {
        delegate.disconnect(nodeId);
    }

    @Override
    public void close(String nodeId) {
        delegate.close(nodeId);
    }

    @Override
    public Node leastLoadedNode(long now) {
        return delegate.leastLoadedNode(now);
    }

    @Override
    public int inFlightRequestCount() {
        return delegate.inFlightRequestCount();
    }

    @Override
    public boolean hasInFlightRequests() {
        return delegate.hasInFlightRequests();
    }

    @Override
    public int inFlightRequestCount(String nodeId) {
        return delegate.inFlightRequestCount(nodeId);
    }

    @Override
    public boolean hasInFlightRequests(String nodeId) {
        return delegate.hasInFlightRequests(nodeId);
    }

    @Override
    public boolean hasReadyNodes(long now) {
        return delegate.hasReadyNodes(now);
    }

    @Override
    public void wakeup() {
        delegate.wakeup();
    }

    @Override
    public ClientRequest newClientRequest(String nodeId, AbstractRequest.Builder<?> requestBuilder, long createdTimeMs, boolean expectResponse) {
        return delegate.newClientRequest(nodeId, requestBuilder, createdTimeMs, expectResponse);
    }

    @Override
    public ClientRequest newClientRequest(String nodeId, AbstractRequest.Builder<?> requestBuilder, long createdTimeMs, boolean expectResponse, int requestTimeoutMs, RequestCompletionHandler callback) {
        return delegate.newClientRequest(nodeId, requestBuilder, createdTimeMs, expectResponse, requestTimeoutMs, callback);
    }

    @Override
    public void initiateClose() {
        delegate.initiateClose();
    }

    @Override
    public boolean active() {
        return delegate.active();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    public String[] getAddresses() {
        return addresses;
    }
}
