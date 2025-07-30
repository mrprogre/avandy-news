package com.avandy.news.parser;

import com.avandy.news.model.Headline;
import com.avandy.news.utils.Common;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class ParserJsoup {

    public List<Headline> parse(String url) {
        List<Headline> headlines = new ArrayList<>();
        String title;
        String link;
        String date;

        try {
            Document document = Jsoup.connect(url)
                    .timeout(Common.READ_TIMEOUT)
                    .userAgent("Safari/537.3")
                    .get();

            Elements items = document.getElementsByTag("item");
            for (Element element : items) {
                title = element.getElementsByTag("title").text();

                link = element.getElementsByTag("link").text();
                if (link.isEmpty()) link = element.ownText();

                date = element.getElementsByTag("pubDate").text();
                Date pubDate = getPubDate(date);

                headlines.add(Headline.builder()
                        .title(title)
                        .link(link)
                        .newsDate(String.valueOf(pubDate))
                        .build());
            }
        } catch (Exception e) {
            System.out.println("ParserJsoup error: " + e.getMessage());
        }

        return headlines;
    }

    // Преобразование входящей даты в виде строки в Date для записи новостей в БД
    public static Date getPubDate(String date) {
        if (date.isEmpty()) return new Date();
        Date pubDate;

        // Tue, 30 Jul 2024 19:23:33 +0300
        // Tue, 30 Jul 2024 13:32:21 -0400
        ThreadLocal<DateFormat> dateFormatZ = ThreadLocal.withInitial(() ->
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH));

        // Tue, 30 Jul 2024 17:39 GMT
        // Tue, 30 Jul 2024 17:39 EST
        ThreadLocal<DateFormat> dateFormatZMin = ThreadLocal.withInitial(() ->
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH));

        // 2024-07-29T18:05:57Z
        ThreadLocal<DateFormat> dateFormatWithT = ThreadLocal.withInitial(() ->
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH));

        // Tue, 30 Jul 24 17:39 GMT
        // Tue, 30 Jul 24 17:39 EST
        ThreadLocal<DateFormat> dateFormatZMinShortYear = ThreadLocal.withInitial(() ->
                new SimpleDateFormat("EEE, dd MMM yy HH:mm:ss z", Locale.ENGLISH));

        // Tue, 30 Jul 24 19:23:33 +0300
        // Tue, 30 Jul 24 13:32:21 -0400
        ThreadLocal<DateFormat> dateFormatShortYear = ThreadLocal.withInitial(() ->
                new SimpleDateFormat("EEE, dd MMM yy HH:mm:ss Z", Locale.ENGLISH));

        try {
            if (date.charAt(3) == ',' && (date.contains("+") || date.contains("-")) && !(date.charAt(14) == ' ')) {
                pubDate = dateFormatZ.get().parse(date);
            } else if (date.charAt(3) == ',' && (date.contains("GMT") || date.contains("EST")) && !(date.charAt(14) == ' ')) {
                pubDate = dateFormatZMin.get().parse(date);
            } else if (date.charAt(3) == ',' && (date.contains("+") || date.contains("-")) && date.charAt(14) == ' ') {
                pubDate = dateFormatShortYear.get().parse(date);
            } else if (date.charAt(3) == ',' && (date.contains("GMT") || date.contains("EST")) && date.charAt(14) == ' ') {
                pubDate = dateFormatZMinShortYear.get().parse(date);
            } else if (date.charAt(10) == 'T') {
                pubDate = dateFormatWithT.get().parse(date);
            } else {
                pubDate = new Date();
            }
        } catch (Exception e) {
            pubDate = new Date();
        }
        return pubDate;
    }

}
