package com.jd.live.agent.demo.grpc.consumer;

import com.jd.live.agent.demo.grpc.service.api.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserServiceController {

    @GrpcClient("grpc-provider")
    private UserServiceGrpc.UserServiceBlockingStub userServiceGrpc;

    @GetMapping("/get")
    public String get(@RequestParam("id") Integer id) {
        UserGetRequest request = UserGetRequest.newBuilder().setId(id).build();
        UserGetResponse response = userServiceGrpc.get(request);
        return response.getName();
    }

    @GetMapping("/create")
    public Integer create(@RequestParam("name") String name,
                          @RequestParam("gender") Integer gender) {
        UserCreateRequest request = UserCreateRequest.newBuilder()
                .setName(name)
                .setGender(gender)
                .build();
        UserCreateResponse response = userServiceGrpc.create(request);
        return response.getId();
    }

}
