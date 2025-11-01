package com.avandy.news.search;

import com.avandy.news.database.JdbcQueries;
import com.avandy.news.database.SQLite;
import com.avandy.news.gui.Gui;
import com.avandy.news.gui.TextLang;
import com.avandy.news.model.Excluded;
import com.avandy.news.model.Headline;
import com.avandy.news.model.Keyword;
import com.avandy.news.model.SearchType;
import com.avandy.news.model.Source;
import com.avandy.news.model.TopTenRow;
import com.avandy.news.parser.ParserRome;
import com.avandy.news.utils.Common;
import com.rometools.rome.feed.synd.SyndEntry;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


public class Search {
    private static final String MANDATORY_NEWS_SYMBOLS = "[^\\p{L}\\p{N}\\s\\p{P}®$₽€°×+№&]";
    private final SimpleDateFormat sqlDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    private int wordFreqMatches = 3;
    private final SQLite sqLite = new SQLite();
    private final JdbcQueries jdbcQueries = new JdbcQueries();
    private int newsCount = 0;
    public static AtomicBoolean isStop = new AtomicBoolean(false);
    public static AtomicBoolean isSearchNow = new AtomicBoolean(false);
    public static AtomicBoolean isSearchFinished = new AtomicBoolean(false);
    private static List<Headline> headlinesList = new ArrayList<>();
    public final Map<String, Integer> topTenWords = new HashMap<>();
    public static List<Excluded> excludedWordsTopTen;
    public HashMap<String, String> titlesFeelings;
    public HashMap<String, Integer> titlesWeight;

    public void mainSearch(SearchType searchType) {
        excludedWordsTopTen = jdbcQueries.getExcludedWords(SearchType.TOP_TEN.getType());

        if (!isSearchNow.get()) {
            boolean isWord = searchType == SearchType.WORD;
            boolean isWords = searchType == SearchType.WORDS;
            boolean isTopTen = searchType == SearchType.TOP_TEN;

            isSearchNow.set(true);
            isStop.set(false);
            LocalTime timeStart = LocalTime.now();

            headlinesList.clear();

            topTenWords.clear();
            boolean isOnlyLastNews = Gui.latestNewsCheckbox.getState();
            Gui.modelMain.setRowCount(0);
            if (!Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.get()) Gui.modelTopTen.setRowCount(0);
            newsCount = 0;
            Gui.amountOfNewsLabel.setText(String.valueOf(newsCount));
            isSearchFinished = new AtomicBoolean(false);

            if (isWord) {
                Gui.searchByKeyword.setVisible(false);
                Gui.stopKeywordSearch.setVisible(true);
            } else if (isWords) {
                Gui.searchByKeywords.setVisible(false);
                Gui.stopKeywordsSearch.setVisible(true);
            }

            try {
                Headline headline;
                List<Source> sourcesList = jdbcQueries.getSources("active");
                sqLite.transaction("BEGIN TRANSACTION");

                // search animation
                new Thread(Common::fillProgressLine).start();

                int q = 1;
                int processPercent;

                // Поиск по ROME и JSOUP
                for (Source source : sourcesList) {
                    if (isStop.get()) return;

                    processPercent = (int) Math.round((double) q++ / (sourcesList.size() + 1) * 100);
                    Gui.searchAnimationLabel.setText("Progress: [" + processPercent + "%] " +
                            source.getSource());

                    try {
                        for (SyndEntry message : new ParserRome().parseFeed(source.getLink()).getEntries()) {
                            String title = message.getTitle()
                                    .replaceAll(MANDATORY_NEWS_SYMBOLS, "")
                                    .replaceAll("#38;", "")
                                    .replaceAll("  ", "")
                                    .replaceAll(" {2,10}", " ")
                                    .replace('\u00A0', ' ');

                            Date pubDate = message.getPublishedDate();
                            String newsDescribe = message.getDescription().getValue()
                                    .trim()
                                    .replaceAll(("<p>|</p>|<br />|&#"), "");
                            if (Common.isHref(newsDescribe)) newsDescribe = title;

                            headline = new Headline(
                                    title,
                                    source.getSource(),
                                    newsDescribe,
                                    message.getLink(),
                                    Headline.DATE_FORMAT.format(pubDate)
                            );

                            if (isWord || isTopTen) {
                                Gui.findWord = Gui.keyword.getText().toLowerCase();
                                String newsTitle = headline.getTitle().toLowerCase();

                                // вставка всех без исключения новостей в архив
                                if (title.length() > Common.MIN_TITLE_LENGTH)
                                    saveToArchive(headline, title, pubDate);

                                if (newsTitle.contains(Gui.findWord) && newsTitle.length() > Common.MIN_TITLE_LENGTH) {
                                    int dateDiff = Common.compareDatesOnly(new Date(), pubDate);

                                    if (dateDiff != 0) {
                                        // Данные за период для таблицы топ-10 без отсева заголовков
                                        getTopTenData(headline.getTitle());
                                    }

                                    if (isTopTen) {
                                        if (dateDiff != 0) {
                                            searchProcess(headline, searchType.getType(), isOnlyLastNews);
                                        }
                                    } else {
                                        //отсеиваем новости, которые уже были найдены ранее при включенном чекбоксе
                                        if (isOnlyLastNews && jdbcQueries.isTitleExists(title, searchType.getType())) {
                                            continue;
                                        }

                                        if (Gui.findWord.isEmpty()) {
                                            if (dateDiff != 0) {
                                                searchProcess(headline, searchType.getType(), isOnlyLastNews);
                                            }

                                        } else {
                                            if (dateDiff != 0) {
                                                searchProcess(headline, searchType.getType(), isOnlyLastNews);
                                            }
                                        }
                                    }
                                }
                            } else if (isWords) {
                                List<Keyword> keywords = jdbcQueries.getKeywords(1);

                                if (!keywords.isEmpty()) {
                                    searchByKeywords(searchType.getType(), keywords, headline, isOnlyLastNews, title, pubDate);
                                } else {
                                    Common.showAlert("No keywords to search!");
                                    Gui.stopKeywordsSearch.doClick();
                                    Gui.modelMain.setRowCount(0);
                                    return;
                                }
                            }
                            if (isStop.get()) return;
                        }
                        if (!Gui.isOnlyLastNews) {
                            jdbcQueries.removeFromTitles();
                        }
                    } catch (Exception e) {
                        String smi = source.getLink()
                                .replaceAll(("https://|http://|www."), "");
                        smi = smi.substring(0, smi.indexOf("/"));
                        Common.console(smi + " is not available");
                    }
                }

                //Время поиска
                Common.console("search completed in " + Duration.between(timeStart, LocalTime.now()).getSeconds() + " s.");
                isSearchNow.set(false);

                // Удаляем дубликаты заголовков и сортируем по дате desc
                removeDuplicatesAndSort(jdbcQueries.getExcludedWords(), searchType);

                // Итоги поиска
                if (isWord && Gui.findWord.isEmpty()) {
                    int excludedCount = newsCount - headlinesList.size();
                    int excludedPercent = (int) Math.round((excludedCount / ((double) newsCount)) * 100);

                    String label = String.format(TextLang.totalText,
                            newsCount, // total
                            headlinesList.size(), // shown
                            excludedCount, // excluded
                            excludedPercent + "%");

                    Gui.amountOfNewsLabel.setText(label);
                } else {
                    Gui.amountOfNewsLabel.setText(TextLang.amountOfNewsLabelText + headlinesList.size());
                }

                /* Начало анализа заголовков */
                // Авто установка позитив/негатив
                if (Common.isAutoFeel()) new JdbcQueries().autoUpdateFeelings(jdbcQueries.getFeelings());

                // заполнение коллекций актуальными данными по ощущениям и весу
                titlesFeelings = jdbcQueries.getFeelingsFromAllNews(Common.getStringIntervalForQuery());
                titlesWeight = jdbcQueries.getWeights();

                // clear animation label
                Gui.searchAnimationLabel.setText("");

                int i = 1;
                // При смене порядка заполнения поправить и в процедуре JdbcQueries.getNewsFromArchive
                for (Headline row : headlinesList) {
                    Gui.modelMain.addRow(new Object[]{
                            i++,
                            row.getTitle(),
                            titlesFeelings.getOrDefault(row.getTitle(), null),
                            titlesWeight.getOrDefault(row.getTitle(), null),
                            row.getNewsDate(),
                            row.getSource(),
                            row.getDescribe(),
                            row.getLink()
                    });
                }

                isSearchFinished.set(true);
                Gui.progressBar.setValue(100);

                if (isWord) {
                    Gui.searchByKeyword.setVisible(true);
                    Gui.stopKeywordSearch.setVisible(false);
                } else if (isWords) {
                    Gui.searchByKeywords.setVisible(true);
                    Gui.stopKeywordsSearch.setVisible(false);
                }

                // коммит транзакции
                sqLite.transaction("COMMIT");

                // Удаление исключённых слов из мап для анализа
                for (Excluded word : excludedWordsTopTen) {
                    topTenWords.remove(word.getWord().toLowerCase());
                }

                // Заполнение таблицы Top-10 в UI с проверкой схожести слов методом Джарро-Винклера
                wordFreqMatches = (searchType == SearchType.WORD) ? 5 : 3;
                // отсев редких слов
                topTenWords.entrySet().removeIf(x -> x.getValue() < wordFreqMatches);
                // сопоставление слов и заполнение
                Common.fillTopTenWithoutDuplicates(topTenWords);

                Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.set(false);

                if (isWord) {
                    Gui.newsInArchiveLabel.setText(TextLang.newsInArchiveLabelText + jdbcQueries.archiveNewsCount());
                }

                Common.calcBalanceFeelingsByPeriod();
            } catch (Exception e) {
                Common.showAlert("Search error: " + e.getMessage() + "\nRestart the app please!");
                try {
                    sqLite.transaction("ROLLBACK");
                } catch (SQLException ignored) {
                }
                isStop.set(true);
            }
        }
    }

    private static void removeDuplicatesAndSort(List<String> excludedTitles, SearchType searchType) {
        Map<String, Headline> uniqueByTitle = new LinkedHashMap<>();

        if (searchType == SearchType.WORD && Gui.findWord.isEmpty()) {
            for (String word : excludedTitles) {
                headlinesList.removeIf(x -> x.getTitle().toLowerCase().contains(word));
            }
        }

        for (Headline item : headlinesList) {
            uniqueByTitle.putIfAbsent(item.getTitle(), item);
        }

        // Сортируем по дате в обратном порядке (новые первыми)
        headlinesList = new ArrayList<>(uniqueByTitle.values());
        headlinesList.sort(Collections.reverseOrder());
    }

    private void searchByKeywords(String searchType, List<Keyword> keywords, Headline headline,
                                  boolean isOnlyLastNews, String title, Date pubDate) {
        for (Keyword keyword : keywords) {
            if (headline.getTitle().toLowerCase().contains(keyword.getWord().toLowerCase())
                    && headline.getTitle().length() > Common.MIN_TITLE_LENGTH) {

                // отсеиваем новости которые были обнаружены ранее
                if (isOnlyLastNews && jdbcQueries.isTitleExists(title, searchType)) {
                    continue;
                }

                //Data for a table
                int dateDiff = Common.compareDatesOnly(new Date(), pubDate);

                if (dateDiff != 0) {
                    saveToArchive(headline, title, pubDate);
                    searchProcess(headline, searchType, isOnlyLastNews);
                    getTopTenData(headline.getTitle());
                }
            }
        }
    }

    private void searchProcess(Headline headline, String searchType, boolean isOnlyLastNews) {
        newsCount++;
        Gui.amountOfNewsLabel.setText(String.valueOf(newsCount));

        headlinesList.add(headline);

        if (isOnlyLastNews) {
            jdbcQueries.addTitles(headline.getTitle(), searchType);
        }
    }

    private void saveToArchive(Headline headline, String title, Date pubDate) {
        jdbcQueries.addAllTitlesToArchive(title,
                sqlDateFormat.format(pubDate),
                headline.getLink(),
                headline.getSource(),
                headline.getDescribe());
    }

    private void getTopTenData(String title) {
        String[] substr = title.split(" ");

        for (String s : substr) {
            if (s.length() > 3) {
                s = Common.keepOnlyLetters(s);
                topTenWords.put(s, topTenWords.getOrDefault(s, 0) + 1);
            }
        }
    }

    public static List<Headline> getDataFromMainTable() {
        Headline headline;
        List<Headline> newsList = new ArrayList<>();

        for (int i = 0; i < Gui.modelMain.getRowCount(); i++) {
            headline = new Headline(
                    Gui.modelMain.getValueAt(i, 1), // Title
                    Gui.modelMain.getValueAt(i, 2), // Feel
                    Gui.modelMain.getValueAt(i, 3), // Wt
                    Gui.modelMain.getValueAt(i, 4), // Source
                    Gui.modelMain.getValueAt(i, 5), // Date
                    Gui.modelMain.getValueAt(i, 7)  // Link
            );
            newsList.add(headline);
        }
        return newsList;
    }

    public static List<TopTenRow> getDataFromTopTenTable() {
        TopTenRow topTenRow;
        List<TopTenRow> newsList = new ArrayList<>();

        for (int i = 0; i < Gui.modelTopTen.getRowCount(); i++) {
            if (Gui.modelTopTen.getValueAt(i, 0).toString().contains("- -")) continue;

            topTenRow = new TopTenRow(
                    Gui.modelTopTen.getValueAt(i, 0).toString(),
                    (int) Gui.modelTopTen.getValueAt(i, 1)
            );
            newsList.add(topTenRow);
        }
        return newsList;
    }

    public void searchInArchive(SearchType searchType) {
        isSearchFinished.set(false);

        if (!isSearchNow.get()) {
            isSearchNow.set(true);
            JdbcQueries jdbcQueries = new JdbcQueries();
            new Thread(Common::fillProgressLine).start();
            final int WORD_FREQ_MATCHES = 2;
            Gui.modelMain.setRowCount(0);
            Gui.modelTopTen.setRowCount(0);

            Map<String, Integer> wordsCount = new HashMap<>();
            List<String> newsFromArchive = new ArrayList<>();

            if (searchType == SearchType.WORDS) {
                List<Keyword> keywords = jdbcQueries.getKeywords(1);
                List<String> newsFromArchiveByKeywords;

                for (Keyword keyword : keywords) {
                    newsFromArchiveByKeywords = jdbcQueries.getNewsFromArchive(keyword.getWord());
                    newsFromArchive.addAll(newsFromArchiveByKeywords);
                }
            } else if (searchType == SearchType.WORD) {
                newsFromArchive = jdbcQueries.getNewsFromArchive(Gui.keyword.getText());
            }
            Gui.amountOfNewsLabel.setText(TextLang.amountOfNewsLabelText + newsFromArchive.size());

            for (String title : newsFromArchive) {
                String[] substr = title.split(" ");

                for (String s : substr) {
                    if (s.length() > 3) {
                        s = Common.keepOnlyLetters(s);
                        wordsCount.put(s, wordsCount.getOrDefault(s, 0) + 1);
                    }
                }
            }

            // Удаление исключённых слов из мап для анализа
            List<Excluded> excludedWordsFromAnalysis = jdbcQueries.getExcludedWords(SearchType.TOP_TEN.getType());
            for (Excluded word : excludedWordsFromAnalysis) {
                wordsCount.remove(word.getWord());
            }

            // Сортировка DESC и заполнение таблицы анализа
            wordsCount.entrySet().stream()
                    .filter(x -> x.getValue() > WORD_FREQ_MATCHES)
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .forEach(x -> Gui.modelTopTen.addRow(new Object[]{x.getKey(), x.getValue()}));

            Common.calcBalanceFeelingsByPeriod();

            isSearchFinished.set(true);
            isSearchNow.set(false);
            Gui.progressBar.setValue(100);
        }
    }

}
