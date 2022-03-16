package com.kpmg.rcm.sourcing.common.config.properties;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties("azure")
@Getter
@Setter
public class AzureProperties {

    @NotNull(message = "Container Name cannot be null")
    @NotEmpty(message = "Container Name cannot be empty")
    private String containerName;

    @NotNull(message = "Connection string cannot be null")
    @NotEmpty(message = "Connection string cannot be empty")
    private String connectionString;

    @NotNull(message = "isAzureFileUploadEnable cannot be null")
    @NotEmpty(message = "isAzureFileUploadEnable cannot be empty")
    private Boolean isAzureFileUploadEnable;
    

}
