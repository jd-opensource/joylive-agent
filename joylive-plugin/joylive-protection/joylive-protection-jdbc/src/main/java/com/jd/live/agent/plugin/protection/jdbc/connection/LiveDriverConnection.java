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
package com.jd.live.agent.plugin.protection.jdbc.connection;

import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.protection.jdbc.datasource.LiveDataSource;
import lombok.Getter;
import lombok.Setter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * Concrete database connection implementation extending {@link LiveConnectionAdapter}.
 * Provides driver-specific behaviors while maintaining standard connection interface.
 */
public class LiveDriverConnection extends LiveConnectionAdapter {

    @Getter
    private final LiveDataSource dataSource;

    @Setter
    private Consumer<DbConnection> onClose;

    public LiveDriverConnection(Connection delegate, ClusterRedirect address, LiveDataSource dataSource) {
        super(delegate, address);
        this.dataSource = dataSource;
    }

    @Override
    public void close() throws SQLException {
        try {
            super.close();
        } finally {
            closed = true;
            if (onClose != null) {
                onClose.accept(this);
            }
        }
    }
}
