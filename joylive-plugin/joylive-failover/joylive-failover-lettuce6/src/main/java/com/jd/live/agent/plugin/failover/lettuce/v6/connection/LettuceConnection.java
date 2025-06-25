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
package com.jd.live.agent.plugin.failover.lettuce.v6.connection;

import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import io.lettuce.core.RedisURI;

public interface LettuceConnection extends DbConnection {

    /**
     * Redirects the connection to a new cluster address.
     *
     * @param newAddress the new cluster address to redirect to
     * @return ClusterRedirect object representing the redirection
     */
    ClusterRedirect redirect(ClusterAddress newAddress);

    static RedisURI.Builder builder(RedisURI uri) {
        return RedisURI.builder(uri)
                .withAuthentication(uri)
                .withSsl(uri)
                .withDatabase(uri.getDatabase())
                .withTimeout(uri.getTimeout())
                .withVerifyPeer(uri.isVerifyPeer())
                .withVerifyPeer(uri.getVerifyMode());
    }
}
