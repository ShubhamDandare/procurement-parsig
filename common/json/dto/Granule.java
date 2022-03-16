
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
import com.kpmg.rcm.sourcing.common.dto.MemoryManagement;
import com.kpmg.rcm.sourcing.common.util.MemoryUtil;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({
    "_id",
    "_created",
    "_lastModified",
    "key",
    "system",
    "source",
    "subgranulesList",
    "subGranules"
})
@Builder @AllArgsConstructor @Data @NoArgsConstructor
// TODO [Harsh] - implement clear method to release memory
public class Granule implements Serializable, MemoryManagement<Granule> {

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
    @JsonProperty("subgranulesList")
    public List<String> subgranulesList = null;
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

    @Override
    public void clear() {
        MemoryUtil.clear(this.additionalProperties);
        MemoryUtil.clear(this.system, this.source);

    }
}
