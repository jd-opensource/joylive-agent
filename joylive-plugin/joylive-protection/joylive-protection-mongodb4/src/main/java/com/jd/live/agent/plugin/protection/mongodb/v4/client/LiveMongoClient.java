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
package com.jd.live.agent.plugin.protection.mongodb.v4.client;

import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.db.DbFailoverResponse;
import com.jd.live.agent.governance.db.DbAddress;
import com.jd.live.agent.governance.db.DbFailover;
import com.mongodb.ClientSessionOptions;
import com.mongodb.client.*;
import com.mongodb.connection.ClusterDescription;
import lombok.Getter;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.function.Consumer;

public class LiveMongoClient implements MongoClient, DbConnection {

    private volatile MongoClient delegate;

    @Getter
    private volatile DbFailover failover;

    private final MongoClientFactory factory;

    private final Consumer<LiveMongoClient> onClose;

    private volatile boolean closed;

    public LiveMongoClient(MongoClient delegate,
                           DbFailover failover,
                           MongoClientFactory factory,
                           Consumer<LiveMongoClient> onClose) {
        this.delegate = delegate;
        this.failover = failover;
        this.factory = factory;
        this.onClose = onClose;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public MongoDatabase getDatabase(String s) {
        return delegate.getDatabase(s);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ClientSession startSession() {
        return delegate.startSession();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ClientSession startSession(ClientSessionOptions clientSessionOptions) {
        return delegate.startSession(clientSessionOptions);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized void close() {
        closed = true;
        delegate.close();
        if (onClose != null) {
            onClose.accept(this);
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public MongoIterable<String> listDatabaseNames() {
        return delegate.listDatabaseNames();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public MongoIterable<String> listDatabaseNames(ClientSession clientSession) {
        return delegate.listDatabaseNames(clientSession);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ListDatabasesIterable<Document> listDatabases() {
        return delegate.listDatabases();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ListDatabasesIterable<Document> listDatabases(ClientSession clientSession) {
        return delegate.listDatabases(clientSession);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public <TResult> ListDatabasesIterable<TResult> listDatabases(Class<TResult> aClass) {
        return delegate.listDatabases(aClass);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public <TResult> ListDatabasesIterable<TResult> listDatabases(ClientSession clientSession, Class<TResult> aClass) {
        return delegate.listDatabases(clientSession, aClass);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ChangeStreamIterable<Document> watch() {
        return delegate.watch();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(Class<TResult> aClass) {
        return delegate.watch(aClass);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ChangeStreamIterable<Document> watch(List<? extends Bson> list) {
        return delegate.watch(list);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> list, Class<TResult> aClass) {
        return delegate.watch(list, aClass);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ChangeStreamIterable<Document> watch(ClientSession clientSession) {
        return delegate.watch(clientSession);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, Class<TResult> aClass) {
        return delegate.watch(clientSession, aClass);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ChangeStreamIterable<Document> watch(ClientSession clientSession, List<? extends Bson> list) {
        return delegate.watch(clientSession, list);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, List<? extends Bson> list, Class<TResult> aClass) {
        return delegate.watch(clientSession, list, aClass);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ClusterDescription getClusterDescription() {
        return delegate.getClusterDescription();
    }

    @Override
    public synchronized DbFailoverResponse failover(DbAddress newAddress) {
        if (closed) {
            return DbFailoverResponse.NONE;
        }
        MongoClient old = this.delegate;
        this.failover = failover.newAddress(newAddress);
        this.delegate = factory.create(newAddress);
        old.close();
        return DbFailoverResponse.SUCCESS;
    }
}
