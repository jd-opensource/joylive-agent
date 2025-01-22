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
package com.jd.live.agent.demo.grpc.provider.service;

import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.demo.grpc.service.api.*;
import com.jd.live.agent.demo.response.LiveTransmission;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.concurrent.ThreadLocalRandom;

@GrpcService
public class UserServiceGrpcImpl extends UserServiceGrpc.UserServiceImplBase {

    @Override
    public void get(UserGetRequest request, StreamObserver<UserGetResponse> responseObserver) {
        if (request.getId() <= -100) {
            responseObserver.onError(Status.INTERNAL.withDescription("Server error!").asException());
            return;
        }
        if (request.getId() < 0 && ThreadLocalRandom.current().nextInt(3) == 0) {
            responseObserver.onError(Status.INTERNAL.withDescription("Server error!").asException());
            return;
        }
        if (request.getId() >= 100) {
            try {
                Thread.sleep(request.getId());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        UserGetResponse.Builder builder = UserGetResponse.newBuilder();
        builder.setId(request.getId())
                .setName("index:" + request.getId() + ", time:" + System.currentTimeMillis())
                .setGender(request.getId() % 2 + 1)
                .setUnit(System.getProperty(LiveTransmission.X_LIVE_UNIT, ""))
                .setCell(System.getProperty(LiveTransmission.X_LIVE_CELL, ""))
                .setCluster(System.getProperty(LiveTransmission.X_LIVE_CLUSTER, ""))
                .setCloud(System.getProperty(LiveTransmission.X_LIVE_CLOUD, ""))
                .setIp(Ipv4.getLocalIp());
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void create(UserCreateRequest request, StreamObserver<UserCreateResponse> responseObserver) {
        UserCreateResponse.Builder builder = UserCreateResponse.newBuilder();
        builder.setId((int) (System.currentTimeMillis() / 1000));
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}
