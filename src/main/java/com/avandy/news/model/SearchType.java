package com.avandy.news.model;

import lombok.Getter;

@Getter
public enum SearchType {
    WORD("word"),
    WORDS("words"),
    TOP_TEN("top-ten");

    private final String type;

    SearchType(String type) {
        this.type = type;
    }
}
