package com.avandy.news.model;

/**
 * В связи с тем, что не все сайты парсятся с помощью ROME добавлен JSOUP.
 * Основная часть источников приходится на ROME.
 * В Schedulers сначала запускается JSOUP, потом ROME.
 */

public enum ParserType {
    ROME,
    JSOUP
}