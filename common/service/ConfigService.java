package com.kpmg.rcm.sourcing.common.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kpmg.rcm.sourcing.common.dto.SourceFieldMappingResponse;
import com.kpmg.rcm.sourcing.common.response.PTAResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ConfigService {

    @Autowired
    @Qualifier("configServiceClient")
    private RestTemplate configServiceClient;

    public ResponseEntity<SourceFieldMappingResponse> getSourceDetails(String jurisdiction, String sourceName) {

//        HttpEntity<String> entity = new HttpEntity<>(str, headers);
        return configServiceClient.exchange("/source/" + jurisdiction + "/" + sourceName,
                HttpMethod.GET, null, SourceFieldMappingResponse.class);

    }


    public ResponseEntity<PTAResponse> getCFRLinkagesMap() {

//        HttpEntity<String> entity = new HttpEntity<>(str, headers);
        return configServiceClient.exchange("/pta/cfr/map/get",
                HttpMethod.POST, null, PTAResponse.class);

    }

    public ResponseEntity<List> getCFRLinkages(String key) {

//        HttpEntity<String> entity = new HttpEntity<>(str, headers);
        return configServiceClient.exchange("/pta/cfr/get/"+key,
                HttpMethod.GET, null, List.class);
    }

}
