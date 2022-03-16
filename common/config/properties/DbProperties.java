package com.kpmg.rcm.sourcing.common.config.properties;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties("datasource")
@Getter
@Setter
public class DbProperties {

	@NotNull(message = "DB user cannot be null")
	@NotEmpty(message = "DB user cannot be empty")
	private String user;

	@NotNull(message = "DB password cannot be null")
	@NotEmpty(message = "DB password cannot be empty")
	private String password;

	@NotNull(message = "DB host cannot be null")
	@NotEmpty(message = "DB host cannot be empty")
	private String host;

	@NotNull(message = "DB port cannot be null")
	private Integer port;

	@NotNull(message = "DB Name cannot be null")
	@NotEmpty(message = "DB Name cannot be empty")
	private String dbName;
}
