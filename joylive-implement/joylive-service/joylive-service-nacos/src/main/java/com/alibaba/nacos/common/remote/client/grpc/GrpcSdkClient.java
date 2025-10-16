/*
 * Copyright 1999-2020 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.nacos.common.remote.client.grpc;

import com.alibaba.nacos.api.ability.constant.AbilityMode;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.common.remote.client.RpcClientTlsConfig;
import com.jd.live.agent.core.util.option.CompositeOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.core.util.option.PropertiesOption;

import java.util.Map;
import java.util.Properties;

/**
 * gRPC client for sdk.
 *
 * @author liuzunfei
 * @version $Id: GrpcSdkClient.java, v 0.1 2020年09月07日 11:05 AM liuzunfei Exp $
 */
public class GrpcSdkClient extends GrpcClient {

    private static final String NACOS_SERVER_GRPC_PORT_OFFSET = "nacos.server.grpc.port.offset";

    private Properties properties;

    /**
     * Constructor.
     *
     * @param name name of client.
     */
    public GrpcSdkClient(String name) {
        super(name);
    }

    /**
     * constructor.
     *
     * @param config of GrpcClientConfig.
     */
    public GrpcSdkClient(GrpcClientConfig config) {
        super(config);
    }

    /**
     * constructor.
     *
     * @param config of GrpcClientConfig.
     */
    public GrpcSdkClient(GrpcClientConfig config, Properties properties) {
        super(config);
        this.properties = properties;
    }

    /**
     * Constructor.
     *
     * @param name               name of client.
     * @param threadPoolCoreSize .
     * @param threadPoolMaxSize  .
     * @param labels             .
     */
    @Deprecated
    public GrpcSdkClient(String name, Integer threadPoolCoreSize, Integer threadPoolMaxSize, Map<String, String> labels) {
        this(name, threadPoolCoreSize, threadPoolMaxSize, labels, null);
    }

    @Deprecated
    public GrpcSdkClient(String name, Integer threadPoolCoreSize, Integer threadPoolMaxSize, Map<String, String> labels, RpcClientTlsConfig tlsConfig) {
        super(name, threadPoolCoreSize, threadPoolMaxSize, labels, tlsConfig);
    }

    @Override
    protected AbilityMode abilityMode() {
        return AbilityMode.SDK_CLIENT;
    }

    @Override
    public int rpcPortOffset() {
        // parse rpc port by live
        Option option = CompositeOption.of(PropertiesOption.of(properties), PropertiesOption.ofSystemProperties());
        return option.getPositive(NACOS_SERVER_GRPC_PORT_OFFSET, Constants.SDK_GRPC_PORT_DEFAULT_OFFSET);
    }

}