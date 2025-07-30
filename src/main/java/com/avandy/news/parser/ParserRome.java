package com.avandy.news.parser;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import static com.avandy.news.utils.Common.CONNECT_TIMEOUT;
import static com.avandy.news.utils.Common.READ_TIMEOUT;

@RequiredArgsConstructor
public class ParserRome {
    long start;

    public SyndFeed parseFeed(String url) {
        try {
            start = System.currentTimeMillis();
            URLConnection connection = new URL(url).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            try (InputStream inputStream = connection.getInputStream()) {
                XmlReader reader = new XmlReader(inputStream);
                return new SyndFeedInput().build(reader);
            } catch (FeedException e) {
                System.out.println("ParserRome.FeedException: " + url + ", " + e.getMessage());
            }

            long downloadTime = System.currentTimeMillis() - start;
            if (downloadTime > 10000L) {
                System.out.println("Long search ParserRome: " + url + " - " + (downloadTime / 1000) + "сек.");
            }

        } catch (IOException e) {
            System.out.println("ParserRome source error " + e.getMessage());
        }
        return null;
    }

}