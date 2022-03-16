
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
    "language",
    "citation",
    "country",
    "inForce",
    "heading",
    "teaser",
    "abstract",
    "content",
    "notes",
    "dates",
    "linkages",
    "orgs",
    "topics",
    "geographies",
    "files",
    "parent",
    "child",
    "checksum",
    "priority1content",
    "priority2Content",
    "priority3Content",
    "originalText",
    "changed",
    "changedFields",
    "enrichments"
})
@Builder @AllArgsConstructor @Data @NoArgsConstructor
public class Source implements Serializable, MemoryManagement<Source> {

    @JsonProperty("language")
    public Language language;
    @JsonProperty("citation")
    public Citation citation;
    @JsonProperty("country")
    public Country country;
    @JsonProperty("inForce")
    public Boolean inForce;
    @JsonProperty("heading")
    public String heading;
    @JsonProperty("teaser")
    public String teaser;
    @JsonProperty("abstract")
    public String _abstract;
    @JsonProperty("content")
    public String content;
    @JsonProperty("notes")
    public List<Note> notes = null;
    @JsonProperty("dates")
    public Dates dates;
    @JsonProperty("linkages")
    public List<Linkage> linkages = null;
    @JsonProperty("orgs")
    public List<Org> orgs = null;
    @JsonProperty("topics")
    public List<String> topics = null;
    @JsonProperty("geographies")
    public List<Geography> geographies = null;
    @JsonProperty("files")
    public List<File> files = null;
    @JsonProperty("parent")
    public List<Parent> parent = null;
    @JsonProperty("child")
    public List<Child> child = null;
    @JsonProperty("checksum")
    public String checksum;
    @JsonProperty("priority1content")
    public String priority1content;
    @JsonProperty("priority2Content")
    public String priority2Content;
    @JsonProperty("priority3Content")
    public String priority3Content;
    @JsonProperty("originalText")
    public String originalText;
    @JsonProperty("changed")
    public Boolean changed;
    @JsonProperty("changedFields")
    public List<String> changedFields = null;
    @JsonProperty("enrichments")
    public List<Enrichment> enrichments = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = -502308521785840486L;

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
