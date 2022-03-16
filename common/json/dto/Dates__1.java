
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({
    "effective",
    "expire",
    "published",
    "updated",
    "compliance"
})
@Builder @AllArgsConstructor @Data @NoArgsConstructor
public class Dates__1 implements Serializable
{

    @JsonProperty("effective")
    public String effective;
    @JsonProperty("expire")
    public String expire;
    @JsonProperty("published")
    public String published;
    @JsonProperty("updated")
    public String updated;
    @JsonProperty("compliance")
    public String compliance;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = -2825519087740188710L;

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
