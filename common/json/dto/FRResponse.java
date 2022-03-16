
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

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({
    "_id",
    "_created",
    "_lastModified",
    "key",
    "system",
    "source",
    "subGranules"
})
@Builder @AllArgsConstructor
public class FRResponse implements Serializable
{

    @JsonProperty("_id")
    public String id;
    @JsonProperty("_created")
    public String created;
    @JsonProperty("_lastModified")
    public String lastModified;
    @JsonProperty("key")
    public String key;
    @JsonProperty("system")
    public System system;
    @JsonProperty("source")
    public Source source;
    @JsonProperty("subGranules")
    public List<SubGranule> subGranules = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = 6779143940039295870L;

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
