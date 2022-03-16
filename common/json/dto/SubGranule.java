
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
    "id",
    "commonId",
    "version",
    "seq",
    "subcitation",
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
    "parentId",
    "childId",
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
public class SubGranule implements Serializable
{

    @JsonProperty("id")
    public String id;
    @JsonProperty("commonId")
    //@JsonIgnore
    public String commonId;
    @JsonProperty("version")
    public String version;
    @JsonProperty("seq")
    public String seq;
    @JsonProperty("subcitation")
    public Subcitation subcitation;
    @JsonProperty("heading")
    public String heading;
    @JsonProperty("teaser")
    public String teaser;
    @JsonProperty("abstract")
    public String _abstract;
    @JsonProperty("content")
    public String content;
    @JsonProperty("notes")
    public List<Note__1> notes = null;
    @JsonProperty("dates")
    public Dates__1 dates;
    @JsonProperty("linkages")
    public List<Linkage__1> linkages = null;
    @JsonProperty("orgs")
    public List<Org__1> orgs = null;
    @JsonProperty("topics")
    public List<String> topics = null;
    @JsonProperty("geographies")
    public List<Geography__1> geographies = null;
    @JsonProperty("files")
    public List<File__1> files = null;
    @JsonProperty("parentId")
    public String parentId;
    @JsonProperty("childId")
    public List<String> childId = null;
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
    public List<Enrichment__1> enrichments = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private final static long serialVersionUID = -3476798737126443997L;

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
