package com.avandy.news.model;

public enum GuiSize {
    LARGE("large"),
    MIDDLE("middle"),
    SMALL("small");
    private final String size;

    GuiSize(String size) {
        this.size = size;
    }

    public String getSize() {
        return size;
    }
}