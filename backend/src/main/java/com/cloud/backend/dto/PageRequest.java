package com.cloud.backend.dto;

public class PageRequest {

    private int page = 1;
    private int size = 20;

    public PageRequest() {}

    public PageRequest(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public int getPage() { return page; }
    public int getSize() { return size; }

    public int getOffset() {
        return (page - 1) * size;
    }
}
