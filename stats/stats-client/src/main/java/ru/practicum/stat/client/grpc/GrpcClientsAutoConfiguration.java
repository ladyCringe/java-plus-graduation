package ru.practicum.stat.client.grpc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "net.devh.boot.grpc.client.inject.GrpcClient")
@ComponentScan(basePackages = "ru.practicum.stat.client.grpc")
public class GrpcClientsAutoConfiguration {
}
