
package com.kpmg.rcm.sourcing.common.json.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.kpmg.rcm.sourcing.common.dto.MemoryManagement;
import com.kpmg.rcm.sourcing.common.util.MemoryUtil;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({
    "id",
    "commonId",
    "version",
    "jurisdiction",
    "sourceName",
    "sourceUrl",
    "docType",
    "granuleType"
})
@Builder @AllArgsConstructor @Data @NoArgsConstructor
public class System implements Serializable, MemoryManagement<System> {

    @JsonProperty("id")
    public String id;
    @JsonProperty("commonId")
    public String commonId;
    @JsonProperty("version")
    public String version;
    @JsonProperty("jurisdiction")
    public String jurisdiction;
    @JsonProperty("sourceName")
    public String sourceName;
    @JsonProperty("sourceUrl")
    public String sourceUrl;
    @JsonProperty("docType")
    public String docType;
    @JsonProperty("granuleType")
    public String granuleType;
    
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = 1693577913820564184L;

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public void clear() {
        MemoryUtil.clear(this.additionalProperties);
    }
}
