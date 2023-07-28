package com.avandy.news.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

@Getter
@Setter
@AllArgsConstructor
public class Headline implements Comparable<Headline> {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.ENGLISH);
    private String title;
    private String newsDate;
    private String source;
    private String describe;
    private String link;
    private String feel;
    private Integer weight;

    public Headline(String title, String source, String describe, String link, String newsDate) {
        this.title = title;
        this.source = source;
        this.describe = describe;
        this.newsDate = newsDate;
        this.link = link;
    }

    public Headline(Object title, Object feel, Object weight, Object newsDate, Object source, Object link) {
        this.title = (String) title;
        this.feel = (String) feel;
        this.weight = (Integer) weight;
        this.newsDate = (String) newsDate;
        this.source = (String) source;
        this.link = (String) link;
    }

    @Override
    public String toString() {
        return this.getTitle() + "\n" + this.getLink() + "\n" + this.getDescribe() + "\n" +
                this.getSource() + " - " + this.getNewsDate();
    }

    @Override
    public int compareTo(Headline o) {
        try {
            return DATE_FORMAT.parse(this.getNewsDate()).compareTo(DATE_FORMAT.parse(o.getNewsDate()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}

