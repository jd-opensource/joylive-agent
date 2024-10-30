package com.jd.live.agent.demo.grpc.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class GrpcProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrpcProviderApplication.class, args);
    }

}
