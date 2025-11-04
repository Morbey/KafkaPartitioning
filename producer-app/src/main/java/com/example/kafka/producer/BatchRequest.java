package com.example.kafka.producer;

public class BatchRequest {
    private Integer count;
    private String prefix;

    public BatchRequest() {
    }

    public BatchRequest(Integer count, String prefix) {
        this.count = count;
        this.prefix = prefix;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
