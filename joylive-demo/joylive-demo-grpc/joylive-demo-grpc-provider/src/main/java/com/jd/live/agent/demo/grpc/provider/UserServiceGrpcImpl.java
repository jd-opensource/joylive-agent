package com.jd.live.agent.demo.grpc.provider;

import com.jd.live.agent.demo.grpc.service.api.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class UserServiceGrpcImpl extends UserServiceGrpc.UserServiceImplBase {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void get(UserGetRequest request, StreamObserver<UserGetResponse> responseObserver) {
        UserGetResponse.Builder builder = UserGetResponse.newBuilder();
        builder.setId(request.getId())
                .setName("index ：" + request.getId() + " time : " + System.currentTimeMillis())
                .setGender(request.getId() % 2 + 1);
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
