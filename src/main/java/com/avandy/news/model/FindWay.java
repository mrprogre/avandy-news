package com.avandy.news.model;

import lombok.Getter;

@Getter
public enum FindWay {
    WEB("rss"),
    WEB_RUS("сети"),
    ARCHIVE("arc"),
    ARCHIVE_RUS("архиве");

    private final String type;

    FindWay(String type) {
        this.type = type;
    }

}
