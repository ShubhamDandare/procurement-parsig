package com.kpmg.rcm.sourcing.common.config.properties;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties("filepath")
@Getter
@Setter
public class FilePathProperties {

	@NotNull(message = "Downloaded File Location cannot be null")
	@NotEmpty(message = "Downloaded File Location cannot be empty")
	private String downloadedFileLocation;

	@NotNull(message = "Temp File Location cannot be null")
	@NotEmpty(message = "Temp File Location cannot be empty")
	private String tempStoragePathForRecordChange;

}
