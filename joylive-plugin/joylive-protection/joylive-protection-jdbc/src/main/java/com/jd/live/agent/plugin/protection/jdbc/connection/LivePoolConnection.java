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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public abstract class LivePoolConnection extends LiveConnectionAdapter {

    protected final LiveDriverConnection driver;

    public LivePoolConnection(Connection delegate, ClusterRedirect address, LiveDriverConnection driver) {
        super(delegate, address);
        this.driver = driver;
    }

    public void close(boolean recycle) throws SQLException {
        if (recycle) {
            close();
        } else {
            try {
                closed = true;
                discard();
                close();
                driver.close();
            } finally {
                doClose();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == driver) return true;
        if (!(o instanceof LivePoolConnection)) return false;
        LivePoolConnection that = (LivePoolConnection) o;
        return Objects.equals(driver, that.driver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(driver);
    }

    protected void discard() {
        driver.getDataSource().discard(delegate);
    }

    protected void doClose() {

    }
}
