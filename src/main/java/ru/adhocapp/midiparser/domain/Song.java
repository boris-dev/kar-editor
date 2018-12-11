
package ru.adhocapp.midiparser.domain;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "GUID",
    "chords",
    "lang",
    "lyrics",
    "midiBase64",
    "name",
    "type",
    "youtubeLink"
})
public class Song {

    @JsonProperty("GUID")
    private String gUID;
    @JsonProperty("chords")
    private String chords;
    @JsonProperty("lang")
    private String lang;
    @JsonProperty("lyrics")
    private String lyrics;
    @JsonProperty("midiBase64")
    private String midiBase64;
    @JsonProperty("name")
    private String name;
    @JsonProperty("type")
    private String type;
    @JsonProperty("youtubeLink")
    private String youtubeLink;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("GUID")
    public String getGUID() {
        return gUID;
    }

    @JsonProperty("GUID")
    public void setGUID(String gUID) {
        this.gUID = gUID;
    }

    @JsonProperty("chords")
    public String getChords() {
        return chords;
    }

    @JsonProperty("chords")
    public void setChords(String chords) {
        this.chords = chords;
    }

    @JsonProperty("lang")
    public String getLang() {
        return lang;
    }

    @JsonProperty("lang")
    public void setLang(String lang) {
        this.lang = lang;
    }

    @JsonProperty("lyrics")
    public String getLyrics() {
        return lyrics;
    }

    @JsonProperty("lyrics")
    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

    @JsonProperty("midiBase64")
    public String getMidiBase64() {
        return midiBase64;
    }

    @JsonProperty("midiBase64")
    public void setMidiBase64(String midiBase64) {
        this.midiBase64 = midiBase64;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("youtubeLink")
    public String getYoutubeLink() {
        return youtubeLink;
    }

    @JsonProperty("youtubeLink")
    public void setYoutubeLink(String youtubeLink) {
        this.youtubeLink = youtubeLink;
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
