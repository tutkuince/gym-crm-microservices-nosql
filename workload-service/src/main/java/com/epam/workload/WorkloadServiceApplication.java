package com.epam.workload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class WorkloadServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkloadServiceApplication.class, args);
    }

}
