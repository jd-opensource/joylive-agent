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
package com.jd.live.agent.plugin.protection.mariadb.v3.request;

import org.mariadb.jdbc.Statement;
import org.mariadb.jdbc.client.Completion;
import org.mariadb.jdbc.client.Context;
import org.mariadb.jdbc.client.socket.Reader;
import org.mariadb.jdbc.client.socket.Writer;
import org.mariadb.jdbc.client.util.ClosableLock;
import org.mariadb.jdbc.export.ExceptionFactory;
import org.mariadb.jdbc.message.ClientMessage;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.function.Consumer;

public class LiveClientMessage implements ClientMessage {

    private final ClientMessage delegate;

    public LiveClientMessage(ClientMessage delegate) {
        this.delegate = delegate;
    }

    @Override
    public int encode(Writer writer, Context context) throws IOException, SQLException {
        return delegate.encode(writer, context);
    }

    @Override
    public int batchUpdateLength() {
        return delegate.batchUpdateLength();
    }

    @Override
    public String description() {
        return delegate.description();
    }

    @Override
    public boolean binaryProtocol() {
        return delegate.binaryProtocol();
    }

    @Override
    public boolean canSkipMeta() {
        return delegate.canSkipMeta();
    }

    @Override
    public Completion readPacket(Statement stmt,
                                 int fetchSize,
                                 long maxRows,
                                 int resultSetConcurrency,
                                 int resultSetType,
                                 boolean closeOnCompletion,
                                 Reader reader,
                                 Writer writer,
                                 Context context,
                                 ExceptionFactory exceptionFactory,
                                 ClosableLock lock,
                                 boolean traceEnable,
                                 ClientMessage message,
                                 Consumer<String> redirectFct) throws IOException, SQLException {
        return delegate.readPacket(stmt, fetchSize, maxRows, resultSetConcurrency, resultSetType,
                closeOnCompletion, reader, writer, context, exceptionFactory,
                lock, traceEnable, message, redirectFct);
    }

    @Override
    public InputStream getLocalInfileInputStream() {
        return delegate.getLocalInfileInputStream();
    }

    @Override
    public boolean mightBeBulkResult() {
        return delegate.mightBeBulkResult();
    }

    @Override
    public boolean validateLocalFileName(String fileName, Context context) {
        return delegate.validateLocalFileName(fileName, context);
    }
}
