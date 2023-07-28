package com.avandy.news.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Source {
    private Integer id;
    private String source;
    private String link;
    private Boolean isActive;
    private Integer position;
    private Integer userId;
}
