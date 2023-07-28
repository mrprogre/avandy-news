package com.avandy.news.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Excluded {
    private Integer id;
    private String word;
    private Integer userId;
}