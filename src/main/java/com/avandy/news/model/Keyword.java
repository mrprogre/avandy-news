package com.avandy.news.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Keyword {
    private String word;
    private Boolean isActive;
    private Integer userId;
}