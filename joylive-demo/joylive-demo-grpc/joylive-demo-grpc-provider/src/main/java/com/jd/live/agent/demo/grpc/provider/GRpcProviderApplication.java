package com.jd.live.agent.demo.grpc.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GRpcProviderApplication {

    public static void main(String[] args) {
        System.out.println("----> start provider ");
        SpringApplication.run(GRpcProviderApplication.class, args);
    }

}
