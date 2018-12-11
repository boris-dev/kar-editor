
package ru.adhocapp.midiparser.domain;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "photoBase64",
    "songs"
})
public class Artist {

    @JsonProperty("name")
    private String name;
    @JsonProperty("photoBase64")
    private String photoBase64;
    @JsonProperty("songs")
    private List<Song> songs = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("photoBase64")
    public String getPhotoBase64() {
        return photoBase64;
    }

    @JsonProperty("photoBase64")
    public void setPhotoBase64(String photoBase64) {
        this.photoBase64 = photoBase64;
    }

    @JsonProperty("songs")
    public List<Song> getSongs() {
        return songs;
    }

    @JsonProperty("songs")
    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
