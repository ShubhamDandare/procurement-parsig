package com.kpmg.rcm.sourcing.common.config.properties;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties("servicebus")
@Getter
@Setter
public class ServiceBusProperties {
    @NotNull(message = "Queue Name cannot be null")
    @NotEmpty(message = "Queue Name cannot be empty")
    private String queueName;

    @NotNull(message = "Connection string cannot be null")
    @NotEmpty(message = "Connection string cannot be empty")
    private String connectionString;

    @NotNull(message = "isServiceBusEnable cannot be null")
    @NotEmpty(message = "isServiceBusEnable cannot be empty")
    private Boolean isServiceBusEnable;

}
