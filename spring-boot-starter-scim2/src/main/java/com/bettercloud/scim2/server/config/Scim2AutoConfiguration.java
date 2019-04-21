package com.bettercloud.scim2.server.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Scim2Properties.class)
@ComponentScan("com.bettercloud.scim2.server")
public class Scim2AutoConfiguration {
}
