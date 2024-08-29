package com.jd.live.agent.demo.sofarpc.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SofaRpcConsumerApplication {


    /**
     * Environment variables:
     * NACOS_ADDR=127.0.0.1:8848;NACOS_NAMESPACE=f1cd8761-5073-4f09-af40-ba10b6c3eae4
     */
    public static void main(String[] args) {
        SpringApplication.run(SofaRpcConsumerApplication.class, args);
    }

}
