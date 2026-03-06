package com.thecookiezen.archiledger.infrastructure.persistence.ladybugdb.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;

import com.thecookiezen.ladybugdb.spring.annotation.NodeEntity;

@NodeEntity(label = "MemoryNote")
public class LadybugMemoryNote {
    @Id
    private String id;

    private String content;

    private List<String> keywords = new ArrayList<>();

    private String context;

    private List<String> tags = new ArrayList<>();

    private String timestamp;

    private int retrievalCount;

    public LadybugMemoryNote() {
    }

    public LadybugMemoryNote(String id, String content, List<String> keywords, String context,
            List<String> tags, String timestamp, int retrievalCount) {
        this.id = id;
        this.content = content;
        this.keywords = keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
        this.context = context;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.timestamp = timestamp;
        this.retrievalCount = retrievalCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getRetrievalCount() {
        return retrievalCount;
    }

    public void setRetrievalCount(int retrievalCount) {
        this.retrievalCount = retrievalCount;
    }
}
