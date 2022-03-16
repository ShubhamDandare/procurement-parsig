package com.kpmg.rcm.sourcing.russia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication(scanBasePackages = { "com.kpmg.rcm.sourcing.russia", "com.kpmg.rcm.sourcing.common" })
@EnableJpaRepositories("com.kpmg.rcm.sourcing.common.repository")
@EntityScan("com.kpmg.rcm.sourcing.common.entity")
@EnableRetry
public class RussiaSourceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RussiaSourceApplication.class, args);
	}
	
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true);

		return mapper;
	}

}
