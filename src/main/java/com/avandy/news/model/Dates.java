package com.avandy.news.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Dates {
    private String type;
    private String description;
    private int day;
    private int month;
    private int year;
    private Boolean isActive;
    private Integer userId;
}
