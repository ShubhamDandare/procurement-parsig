package com.kpmg.rcm.sourcing.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kpmg.rcm.sourcing.common.service.ConfigService;

@Configuration
@EnableConfigurationProperties
public class JsonConfig {

    @Autowired
    private ConfigService configService;

    /*@Bean
    @Qualifier("cfrLinkagesMap")
    public Map<String, List<String>> cfrLinkagesMap() {
        final Map<String, List<String>>[] map = new Map[]{new HashMap<>()};

        configService.getCFRLinkagesMap()
                .doOnNext(ptaResponse ->  {
                     map[0] = ptaResponse.getCfrLinkagesMap();
                }).block();

        return map[0];
    }*/

    @Bean
    @Qualifier("gson")
    public Gson gson() {
        return new GsonBuilder().setPrettyPrinting().create();
    }
}
