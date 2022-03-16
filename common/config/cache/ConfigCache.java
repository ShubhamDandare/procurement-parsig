package com.kpmg.rcm.sourcing.common.config.cache;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kpmg.rcm.sourcing.common.service.ConfigService;

@Component
public class ConfigCache {

    @Autowired
    private ConfigService configService;

    private ConcurrentHashMap<String, List<String>> cfrLinkagesMap = null;

    public List<String> getLinkageCodes(String key){
        List<String> values = null;
        if(cfrLinkagesMap != null){
            values = cfrLinkagesMap.get(key);
        }
        if(values == null){
            values = (List<String>) configService.getCFRLinkages(key).getBody();
            
            if(values != null && !values.isEmpty()){
                putLinkageCodes(key, values);
            }
        }
        return values;
    }

    public void putLinkageCodes(String key, List<String> values){
        if(cfrLinkagesMap == null){
            cfrLinkagesMap = new ConcurrentHashMap<>();
        }
        cfrLinkagesMap.put(key, values);
    }
}
