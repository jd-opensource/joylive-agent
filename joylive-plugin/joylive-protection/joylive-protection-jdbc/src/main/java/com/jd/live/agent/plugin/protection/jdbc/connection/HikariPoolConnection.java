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

import com.jd.live.agent.governance.util.network.ClusterRedirect;
import lombok.Getter;

import java.sql.Connection;
import java.util.function.Consumer;

public class HikariPoolConnection extends LivePoolConnection {

    private final Consumer<HikariPoolConnection> onClose;

    @Getter
    private final Object poolEntry;

    public HikariPoolConnection(Connection delegate,
                                ClusterRedirect address,
                                LiveDriverConnection driver,
                                Consumer<HikariPoolConnection> onClose,
                                Object poolEntry) {
        super(delegate, address, driver);
        this.onClose = onClose;
        this.poolEntry = poolEntry;
    }

    @Override
    protected void doClose() {
        onClose.accept(this);
    }

    @Override
    protected void discard() {
        driver.getDataSource().discard(this);
    }
}
