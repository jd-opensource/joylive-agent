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
package com.jd.live.agent.governance.db.parser.dm;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.util.network.Address;
import com.jd.live.agent.governance.db.DbUrl.DbUrlBuilder;
import com.jd.live.agent.governance.db.parser.AbstractUrlParser;

import java.util.Map;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.isEmpty;
import static com.jd.live.agent.core.util.StringUtils.splitList;

@Extension("dm")
public class DMUrlParser extends AbstractUrlParser {

    private static final String KEY_HOST = "host";
    private static final String KEY_PORT = "port";
    private static final int DEFAULT_PORT = 5236;
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_ADDRESS = "localhost:" + DEFAULT_PORT;

    @Override
    protected void parse(String url, DbUrlBuilder builder) {
        super.parse(url, builder);
        // jdbc:dm://test?test=(192.168.0.96:5236,192.168.0.96:5237)
        Map<String, String> parameters = builder.getParameters();
        String address = builder.getAddress();
        if (address != null && parameters != null && !parameters.isEmpty()) {
            int pos = address.indexOf(':');
            if (pos == -1) {
                // no port, detect service name
                String addr = parameters.get(address);
                if (!isEmpty(addr) && addr.startsWith("(") && addr.endsWith(")")) {
                    addr = addr.substring(1, addr.length() - 1);
                    builder.nodes(toList(splitList(addr), Address::parse)).addressUpdater(this::updateServiceAddress);
                }
            }
        }
        // TODO dm_svc.conf
    }

    @Override
    protected String getDefaultHost(DbUrlBuilder builder) {
        // jdbc:dm://?host=192.168.0.96&port=5236
        Map<String, String> parameters = builder.getParameters();
        String host = parameters.get(KEY_HOST);
        String port = parameters.get(KEY_PORT);
        if (isEmpty(host)) {
            if (isEmpty(port)) {
                return DEFAULT_ADDRESS;
            } else {
                host = DEFAULT_HOST;
            }
        } else if (isEmpty(port)) {
            port = String.valueOf(DEFAULT_PORT);
        }
        builder.addressPart("").addressUpdater(this::updateParameterAddress).parameterPart(null);
        return host + ":" + port;
    }

    private void updateParameterAddress(String address, DbUrlBuilder builder) {
        Address addr = Address.parse(address, true, DEFAULT_PORT);
        String port = addr.getPort() == null ? null : addr.getPort().toString();
        builder.parameter(KEY_HOST, addr.getHost()).parameter(KEY_PORT, port).parameterPart(null);
    }

    private void updateServiceAddress(String address, DbUrlBuilder builder) {
        builder.parameter(builder.getAddress(), "(" + address + ")").parameterPart(null);
    }


}
