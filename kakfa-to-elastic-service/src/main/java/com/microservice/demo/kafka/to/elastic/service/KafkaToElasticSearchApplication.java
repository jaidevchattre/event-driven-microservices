package com.microservice.demo.kafka.to.elastic.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.microservices.demo")
public class KafkaToElasticSearchApplication {

    public static void main(String[] args){
        SpringApplication.run(KafkaToElasticSearchApplication.class,args);
    }
}
