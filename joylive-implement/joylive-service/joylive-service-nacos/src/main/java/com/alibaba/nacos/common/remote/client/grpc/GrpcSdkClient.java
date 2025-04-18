/*
 * Copyright 1999-2022 Alibaba Group Holding Ltd.
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
     * Constructor.
     *
     * @param name               name of client.
     * @param threadPoolCoreSize .
     * @param threadPoolMaxSize  .
     * @param labels             .
     */
    public GrpcSdkClient(String name, Integer threadPoolCoreSize, Integer threadPoolMaxSize, Map<String, String> labels) {
        this(name, threadPoolCoreSize, threadPoolMaxSize, labels, null, null);
    }

    public GrpcSdkClient(String name, Integer threadPoolCoreSize, Integer threadPoolMaxSize, Map<String, String> labels, RpcClientTlsConfig tlsConfig) {
        this(name, threadPoolCoreSize, threadPoolMaxSize, labels, tlsConfig, null);
    }

    public GrpcSdkClient(String name, Integer threadPoolCoreSize, Integer threadPoolMaxSize, Map<String, String> labels, RpcClientTlsConfig tlsConfig, Properties properties) {
        super(name, threadPoolCoreSize, threadPoolMaxSize, labels, tlsConfig);
        this.properties = properties;
    }

    @Override
    protected AbilityMode abilityMode() {
        return AbilityMode.SDK_CLIENT;
    }

    @Override
    public int rpcPortOffset() {
        String value = properties == null ? null : properties.getProperty(NACOS_SERVER_GRPC_PORT_OFFSET);
        value = value != null && !value.isEmpty() ? value : System.getProperty(NACOS_SERVER_GRPC_PORT_OFFSET, String.valueOf(Constants.SDK_GRPC_PORT_DEFAULT_OFFSET));
        return Integer.parseInt(value);
    }

}