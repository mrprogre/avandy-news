package com.avandy.news.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TopTenRow {
    private String word;
    private int frequency;
}
