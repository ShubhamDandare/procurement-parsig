
package com.kpmg.rcm.sourcing.common.json.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
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
    "type",
    "heading",
    "content",
    "QuotedContent",
    "id"
})
@Builder @AllArgsConstructor @Data @NoArgsConstructor
public class Note implements Serializable
{
    @JsonProperty("type")
    public String type;
    @JsonProperty("heading")
    public String heading;
    @JsonProperty("content")
    public String content;
    @JsonProperty("id")
    public String id;
    
    @JsonProperty("QuotedContent")
    public List<QuotedContent> QuotedContent;
    
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = 7772545485653701397L;

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
