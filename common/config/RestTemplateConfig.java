package com.kpmg.rcm.sourcing.common.config;

import java.time.Duration;

import javax.net.ssl.SSLException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RestTemplateConfig {

	@Value("${config.service.api.url}")
	private String configServiceApiURL;

	@Bean
	@Qualifier("configServiceClient")
	public RestTemplate configServiceClient() throws SSLException{

//		 getBuilder()
//				// TODO .baseUrl(configServiceApiURL)
//				.filter(ExchangeFilterFunction.ofResponseProcessor(this::renderApiErrorResponse))
//				//.filter(ExchangeFilterFunction.ofRequestProcessor(this::modifyRequest))
//				.exchangeStrategies(ExchangeStrategies.builder()
//						.codecs(configurer -> configurer
//								.defaultCodecs()
//								// TODO .maxInMemorySize(65000000))
//						.build())
//				.build();

		//		RestTemplate restClient = new RestTemplate(

//				new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
//
//		restClient.setInterceptors(Collections.singletonList((request, body, execution) -> {
//
//			log.debug("Intercepting...");
//			return execution.execute(request, body);
//		}));

		return new RestTemplateBuilder()
				.setConnectTimeout(Duration.ofMillis(300000))
				.setReadTimeout(Duration.ofMillis(300000))
				.rootUri(configServiceApiURL)
				.build();
	}
}
