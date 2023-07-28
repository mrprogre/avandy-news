package com.avandy.news.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Feelings {
    private String like;
    private String feeling;
    private int weight;
    private String username;
    private Boolean isActive;
    private int callOrder;
}
