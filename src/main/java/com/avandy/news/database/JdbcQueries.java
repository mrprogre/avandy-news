package com.avandy.news.database;

import com.avandy.news.gui.Gui;
import com.avandy.news.model.*;
import com.avandy.news.utils.Common;
import com.avandy.news.utils.Login;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JdbcQueries {
    private final Connection connection = SQLite.connection;

    /* INSERT */

    // сохранение всех заголовков в архив
    public void addAllTitlesToArchive(String title, String date, String link, String source, String describe) {
        try {
            String query = "INSERT INTO all_news(title, news_date, link, source, describe, title_lower, pub_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, strftime('%Y-%m-%d', ?))";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.setString(2, date);
            statement.setString(3, link);
            statement.setString(4, source);
            statement.setString(5, describe);
            statement.setString(6, title.toLowerCase());
            statement.setString(7, date);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            if (!e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                Common.showAlert("addAllTitlesToArchive error: " + e.getMessage());
            }
        }
    }

    // ручное добавление заголовка в архив
    public boolean addTitleToArchiveManual(String title, String date, String link, String source, String describe) {
        boolean isAdded = false;

        try {
            String query = "INSERT INTO all_news(title, news_date, link, source, describe, title_lower, pub_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, strftime('%Y-%m-%d', ?))";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.setString(2, date);
            statement.setString(3, link);
            statement.setString(4, source);
            statement.setString(5, describe);
            statement.setString(6, title.toLowerCase());
            statement.setString(7, date);
            statement.executeUpdate();
            statement.close();
            isAdded = true;
        } catch (SQLException e) {
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                Common.showAlert("The title is already in the list");
            }
        }
        return isAdded;
    }

    // Вставка ключевого слова
    public void addKeyword(String word) {
        if (word.length() > 2) {
            try {
                String query = "INSERT INTO keywords(word, user_id) VALUES (?, ?)";
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setString(1, word);
                statement.setInt(2, Login.userId);
                statement.executeUpdate();
                statement.close();

                Common.console("+ Keyword: \"" + word + "\"");
            } catch (Exception e) {
                if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                    Common.showAlert("Keyword: " + word + " is already in the list");
                }
            }
        } else {
            Common.showAlert("Keyword must be more than 2 characters!");
        }
    }

    // Вставка нового источника
    public void addNewSource(String source, String link, String country, ParserType parserType) {
        try {
            String query = "INSERT INTO rss_list(source, link, is_active, user_id, country, parser_type) " +
                    "VALUES (?, ?, 1, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, source);
            statement.setString(2, link);
            statement.setInt(3, Login.userId);
            statement.setString(4, country);
            statement.setString(5, parserType.name());
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                Common.showAlert("Link: " + link + " is already in the list");
            }
            else {
                Common.showAlert("При добавлении источника возникла ошибка: " + e.getMessage());
            }
        }
        Common.console("source " + source + " added");
        Common.showInfoHtml("Source added<br/>" + source);
    }

    // Вставка слова для исключения из анализа частоты употребления слов
    public void addExcludedWord(String word) {
        try {
            String query = "INSERT INTO exclude(word, user_id) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, word);
            statement.setInt(2, Login.userId);
            statement.executeUpdate();
            statement.close();

            Common.console("\"" + word + "\" excluded from top 10");
        } catch (Exception e) {
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                Common.showAlert("Word: " + word + " is already in the list");
            } else {
                Common.showAlert("addExcludedWord error: " + e.getMessage());
            }
        }
    }

    // Вставка избранных заголовков
    public void addFavoriteTitle(String title, String link) {
        try {
            String query = "INSERT INTO favorites(title, link, user_id) VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.setString(2, link);
            statement.setInt(3, Login.userId);
            statement.executeUpdate();
            statement.close();

            Common.console("title added to favorites");
        } catch (Exception e) {
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE"))
                Common.showAlert("Title: " + title + " is already in the list");
            else
                Common.showAlert("При добавлении заголовка в избранное возникла ошибка: " + e.getMessage());
        }
    }

    // вставка кода по заголовку для отсеивания ранее обнаруженных новостей
    public void addTitles(String title, String type) {
        try {
            String query = "INSERT INTO titles(title, type, user_id) VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.setString(2, type);
            statement.setInt(3, Login.userId);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            Common.showAlert("addTitles error: " + e.getMessage());
        }
    }

    // вставка слова для исключения содержащих его заголовков
    public void addWordToExcludeTitles(String word) {
        if (word != null && word.length() > 2) {
            try {
                String query = "INSERT INTO excluded_headlines(word, user_id) VALUES (?, ?)";
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setString(1, word);
                statement.setInt(2, Login.userId);
                statement.executeUpdate();
                statement.close();
                Common.console("+ Excluded: \"" + word + "\"");
            } catch (Exception e) {
                if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                    Common.showAlert("Word: " + word + " is already in the list");
                } else {
                    Common.showAlert("Ошибка добавления слова исключения: " + e.getMessage());
                }
            }
        } else {
            Common.showAlert("Exclusion word must be more than 2 characters!");
        }
    }

    // вставка нового события
    public void addDate(String type, String description, int day, int month, int year) {
        try {
            String query = "INSERT INTO dates(type, description, day, month, year, user_id) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, type);
            statement.setString(2, description);
            statement.setInt(3, day);
            statement.setInt(4, month);
            statement.setInt(5, year);
            statement.setInt(6, Login.userId);
            statement.executeUpdate();
            statement.close();

            Common.console("event added: " + type + " " + description);
        } catch (Exception e) {
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE"))
                Common.showAlert("The event: " + type + " " + description + " is already in the list");
            else
                Common.showAlert("При добавлении даты возникла ошибка: " + e.getMessage());
        }
    }

    public boolean addUser(String username, String password) {
        try {
            String query = "INSERT INTO users(username, password) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, password);
            statement.executeUpdate();
            statement.close();

            return true;
        } catch (Exception e) {
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                Common.showAlert("User exists!");
                new Login().login();
            }
        }
        return false;
    }

    // вставка нового правила для установки ощущений
    public void addAutoFeelingRule(String like, String feeling, Integer weight) {
        try {
            String query = "INSERT INTO feelings(like, feeling, weight, user_id) VALUES (?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, like);
            statement.setString(2, feeling);
            statement.setInt(3, weight);
            statement.setInt(4, Login.userId);
            statement.executeUpdate();
            statement.close();

            Common.console("rule added: " + like + " " + feeling);
        } catch (Exception e) {
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE"))
                Common.showAlert("Rule: " + like + " is already in the list");
            else
                Common.showAlert("При добавлении правила возникла ошибка: " + e.getMessage());
        }
    }

    /* SELECT */
    // Источники новостей
    public List<Source> getSources(String type) {
        List<Source> sources = new ArrayList<>();
        try {
            String query = "SELECT id, source, link, is_active, position, country FROM rss_list " +
                    "WHERE is_active = 1 AND user_id = ? ORDER BY position";

            getAllRssById(type, sources, query);
        } catch (Exception e) {
            Common.showAlert("getSources error: " + e.getMessage());
        }
        return sources;
    }

    private void getAllRssById(String type, List<Source> sources, String query) throws SQLException {
        if (type.equals("all")) {
            query = "SELECT id, source, link, is_active, position, country FROM rss_list WHERE user_id = ? " +
                    "ORDER BY is_active DESC, position";
        }

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, Login.userId);

        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            sources.add(Source.builder()
                    .id(rs.getInt("id"))
                    .source(rs.getString("source"))
                    .link(rs.getString("link"))
                    .isActive(rs.getBoolean("is_active"))
                    .position(rs.getInt("position"))
                    .country(rs.getString("country"))
                    .build());
        }
        rs.close();
        statement.close();
    }

    // Настройки по ключу
    public String getSetting(String key) {
        String setting = null;
        String query = "SELECT value FROM settings WHERE key = ? AND (user_id IS NULL OR user_id = ?)";

        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, key);
            statement.setInt(2, Login.userId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                setting = rs.getString("value");
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("getSettings error: " + e.getMessage());
        }
        return setting;
    }

    // Список исключённые из анализа слова
    public List<Excluded> getExcludedWords(String type) {
        String query = null;
        List<Excluded> excludedWords = new ArrayList<>();

        try {
            if (type.equals("top-ten")) {
                query = "SELECT id, word, user_id FROM exclude WHERE user_id = ? ORDER BY id DESC";
            } else if (type.equals("headline")) {
                query = "SELECT id, word, user_id FROM excluded_headlines WHERE user_id = ? ORDER BY id DESC";
            }

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, Login.userId);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                excludedWords.add(new Excluded(
                        rs.getInt("id"),
                        rs.getString("word"),
                        rs.getInt("user_id"))
                );
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("getExcludedWords error: " + e.getMessage());
        }
        return excludedWords;
    }

    // Список значимых дат
    public List<Dates> getDates(int isActive) {
        List<Dates> dates = new ArrayList<>();
        try {
            String query = "SELECT type, description, day, month, year, is_active, user_id FROM dates " +
                    "WHERE user_id = ? " +
                    "ORDER BY month, day";

            if (isActive == 0) {
                query = "SELECT type, description, day, month, year, is_active, user_id FROM dates " +
                        "WHERE is_active = 1 AND user_id = ? ORDER BY month, day";
            }

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, Login.userId);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                dates.add(new Dates(
                        rs.getString("type"),
                        rs.getString("description"),
                        rs.getInt("day"),
                        rs.getInt("month"),
                        rs.getInt("year"),
                        rs.getBoolean("is_active"),
                        rs.getInt("user_id")
                ));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("getDates error: " + e.getMessage());
        }
        return dates;
    }

    public List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        try {
            String query = "SELECT username FROM users WHERE id != 0 ORDER BY id";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("getAllUsers error: " + e.getMessage());
        }
        return users;
    }

    // Список ключевых слов для поиска (1 - активные, 2 - все)
    public List<Keyword> getKeywords(int isActive) {
        List<Keyword> keywords = new ArrayList<>();
        try {
            String query;
            PreparedStatement statement;

            if (isActive == 1) {
                query = "SELECT word, is_active, user_id FROM keywords WHERE user_id = ? and is_active = ? " +
                        "ORDER BY word";
                statement = connection.prepareStatement(query);
                statement.setInt(2, isActive);
            } else {
                query = "SELECT word, is_active, user_id FROM keywords WHERE user_id = ? ORDER BY is_active DESC, word";
                statement = connection.prepareStatement(query);
            }
            statement.setInt(1, Login.userId);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Keyword keyword = new Keyword(
                        rs.getString("word"),
                        rs.getBoolean("is_active"),
                        rs.getInt("user_id")
                );
                keywords.add(keyword);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("getKeywords error: " + e.getMessage());
        }
        return keywords;
    }

    // Список избранных новостей
    public List<Favorite> getFavorites() {
        List<Favorite> favorites = new ArrayList<>();
        try {
            String query = "SELECT title, link, add_date, user_id FROM favorites WHERE user_id = ? ORDER BY add_date";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, Login.userId);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                favorites.add(new Favorite(
                                rs.getString("title"),
                                rs.getString("link"),
                                rs.getString("add_date"),
                                rs.getInt("user_id")
                        )
                );
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("getExcludedTitlesWords error: " + e.getMessage());
        }
        return favorites;
    }

    // Новости по слову из архива
    public List<String> getNewsFromArchive(String word) {
        List<String> headlines = new ArrayList<>();
        String interval = Common.getStringIntervalForQuery();

        try {
            String query = "SELECT " +
                    "           title, " +
                    "           feel, " +
                    "           weight, " +
                    "           strftime('%H:%M %d.%m.%Y', news_date) as news_date, " +
                    "           source, " +
                    "           describe, " +
                    "           link " +
                    "FROM all_news " +
                    "WHERE news_date BETWEEN datetime('now', '-'||?, 'localtime') AND datetime('now', 'localtime') " +
                    "AND title_lower like '%'|| ? ||'%' " +
                    "ORDER BY pub_date DESC";

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, interval);
            statement.setString(2, word.toLowerCase());

            ResultSet rs = statement.executeQuery();
            int i = 1;
            while (rs.next()) {
                Integer weight = rs.getInt("weight");
                if (rs.getInt("weight") == 0) weight = null;

                // TODO не работает сортировка по дате при поиске по архиву
                Gui.modelMain.addRow(new Object[]{
                        i++,
                        rs.getString("title"),
                        rs.getString("feel"),
                        weight,
                        rs.getString("news_date"),
                        rs.getString("source"),
                        rs.getString("describe"),
                        rs.getString("link")
                });

                headlines.add(rs.getString("title"));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("getNewsForTopTen error: " + e.getMessage());
        }
        return headlines;
    }

    // Link, Describe by hash code
    public String getLinkOrDescribeByHash(String source, String title, String type) {
        String response = "no data found";
        String query = null;
        try {
            if (type.equals("link")) {
                query = "SELECT link FROM all_news WHERE source = ? AND title = ?";
            } else if (type.equals("describe")) {
                query = "SELECT describe FROM all_news WHERE source = ? AND title = ?";
            }

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, source);
            statement.setString(2, title);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                response = rs.getString(type);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("getLinkByHash error: " + e.getMessage());
        }
        return response;
    }

    public int getUserIdByUsername(String username) {
        int id = 0;
        try {
            String query = "SELECT id FROM users WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            id = resultSet.getInt("id");

            statement.close();
        } catch (Exception e) {
            Common.showAlert("getUserHashPassword error: " + e.getMessage());
        }
        return id;
    }

    public String getUsernameById(int userId) {
        String username = "";
        try {
            String query = "SELECT username FROM users WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);

            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            username = resultSet.getString("username");

            statement.close();
        } catch (Exception e) {
            Common.showAlert("getUsernameById error: " + e.getMessage());
        }
        return username;
    }

    public List<Feelings> getFeelings() {
        List<Feelings> feelings = new ArrayList<>();
        try {
            String query = "SELECT like, feeling, weight, user_id, is_active, call_order FROM feelings " +
                    "ORDER BY like, call_order";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                feelings.add(
                        new Feelings(rs.getString("like"),
                                rs.getString("feeling"),
                                rs.getInt("weight"),
                                new JdbcQueries().getUsernameById(rs.getInt("user_id")),
                                rs.getBoolean("is_active"),
                                rs.getInt("call_order")
                        )
                );
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("getFeelings error: " + e.getMessage());
        }
        return feelings;
    }

    public HashMap<String, Integer> getWeights() {
        HashMap<String, Integer> weights = new HashMap<>();
        try {
            String query = "SELECT title, weight FROM all_news WHERE weight IS NOT NULL";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            Integer weight;
            while (rs.next()) {
                weight = rs.getInt("weight");
                if (weight == 0) weight = null;

                weights.put(rs.getString("title"), weight);
            }

            statement.close();
        } catch (Exception e) {
            Common.showAlert("getTitlesWeight error: " + e.getMessage());
        }
        return weights;
    }

    public HashMap<String, String> getFeelingsFromAllNews(String interval) {
        HashMap<String, String> feelings = new HashMap<>();
        try {
            String query = "SELECT distinct title, feel FROM all_news WHERE feel IS NOT NULL " +
                    " AND news_date BETWEEN datetime('now', '-'||?, 'localtime') " +
                    "AND datetime('now', 'localtime')";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, interval);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                feelings.put(rs.getString("title"), rs.getString("feel"));
            }

            statement.close();
        } catch (Exception e) {
            Common.showAlert("getTitlesWeight error: " + e.getMessage());
        }
        return feelings;
    }

    public String getUserHashPassword(String username) {
        String password = "";
        try {
            String query = "SELECT password FROM users WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            password = resultSet.getString("password");

            statement.close();
        } catch (Exception e) {
            Common.showAlert("getUserHashPassword error: " + e.getMessage());
        }
        return password;
    }

    public List<String> getInitQueries() {
        List<String> queries = new ArrayList<>();
        try {
            String query = "SELECT query FROM init_data";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                queries.add(rs.getString("query"));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("getInitQueries error: " + e.getMessage());
        }
        return queries;
    }

    public List<String> getRssQueries(String country) {
        List<String> queries = new ArrayList<>();

        try {
            PreparedStatement statement;
            String query;
            if (country.equals("all")) {
                query = "SELECT query FROM init_rss";
                statement = connection.prepareStatement(query);
            } else {
                query = "SELECT query FROM init_rss WHERE country = ?";
                statement = connection.prepareStatement(query);
                statement.setString(1, country);
            }

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                queries.add(rs.getString("query"));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("getRssQueries error: " + e.getMessage());
        }
        return queries;
    }

    public List<String> getExcludedItems() {
        List<String> items = new ArrayList<>();
        try {
            String query = "SELECT word FROM excluded_items";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                items.add(rs.getString("word"));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("getExcludedItems error: " + e.getMessage());
        }
        return items;
    }

    /* REMOVE */
    // удаление слов из разных таблиц
    public void removeItem(String item, int activeWindow) {
        try {
            String query = null;
            if (activeWindow == 2) {
                query = "DELETE FROM rss_list WHERE source = ? and user_id = ?";
            } else if (activeWindow == 3) {
                query = "DELETE FROM exclude WHERE word = ? and user_id = ?";
            } else if (activeWindow == 4) {
                query = "DELETE FROM excluded_headlines WHERE word = ? and user_id = ?";
            } else if (activeWindow == 5) {
                query = "DELETE FROM keywords WHERE word = ? AND user_id = ?";
            } else if (activeWindow == 6) {
                query = "DELETE FROM favorites WHERE title = ? AND user_id = ?";
            } else if (activeWindow == 7) {
                query = "DELETE FROM dates WHERE type||' '||description = ? AND user_id = ?";
            } else if (activeWindow == 8) {
                query = "DELETE FROM feelings WHERE like = ? AND (user_id IS NOT NULL OR user_id = ?)";
            }

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, item);
            statement.setInt(2, Login.userId);

            statement.executeUpdate();
            statement.close();

            Common.console("Removed: " + item);
        } catch (Exception e) {
            Common.showAlert("removeItem error: " + e.getMessage());
        }

    }

    // Очистка данных любой передаваемой таблицы
    public void removeFromTitles() {
        try {
            String query = "DELETE FROM titles where user_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, Login.userId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("deleteFromTable error: " + e.getMessage());
        }
    }

    // Очистка данных любой передаваемой таблицы
    public void removeFromRssList(String country, String source) {
        try {
            String query = "DELETE FROM rss_list where user_id = ? and country = ? and source = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, Login.userId);
            statement.setString(2, country);
            statement.setString(3, source);
            statement.executeUpdate();
            statement.close();

            Common.console("Removed: " + country + " - " + source);
        } catch (Exception e) {
            Common.showAlert("deleteFromTable error: " + e.getMessage());
        }
    }

    // Очистка данных любой передаваемой таблицы
    public void removeFromUsers(String username) {
        int userId = getUserIdByUsername(username);
        try {
            // "on delete cascade" doesn't work
            String[] removeCascade = {
                    "DELETE FROM dates WHERE user_id = ?",
                    "DELETE FROM exclude WHERE user_id = ?",
                    "DELETE FROM excluded_headlines WHERE user_id = ?",
                    "DELETE FROM favorites WHERE user_id = ?",
                    "DELETE FROM keywords WHERE user_id = ?",
                    "DELETE FROM rss_list WHERE user_id = ?",
                    "DELETE FROM settings WHERE user_id = ?",
                    "DELETE FROM users where id = ?"
            };
            connection.setAutoCommit(false);
            for (String query : removeCascade) {
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setInt(1, userId);
                statement.executeUpdate();
                statement.close();
            }
            connection.setAutoCommit(true);
        } catch (Exception e) {
            Common.showAlert("deleteFromTable error: " + e.getMessage());
        }
    }

    /* DIFFERENT */
    // отсеивание ранее найденных заголовков при включённом чекбоксе
    public boolean isTitleExists(String title, String type) {
        int isExists = 0;
        try {
            String query = "SELECT EXISTS (SELECT 1 FROM titles WHERE title = ? and type = ? and user_id = ?)";

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.setString(2, type);
            statement.setInt(3, Login.userId);

            ResultSet rs = statement.executeQuery();
            rs.next();
            isExists = rs.getInt(1);
            rs.close();
            statement.close();

        } catch (Exception e) {
            Common.showAlert("isTitleExists error: " + e.getMessage());
        }
        return isExists == 1;
    }

    // новостей в архиве всего
    public int archiveNewsCount() {
        int countNews = 0;
        try {
            String query = "SELECT COUNT(*) FROM ALL_NEWS";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                countNews = rs.getInt(1);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("archiveNewsCount error: " + e.getMessage());
        }
        return countNews;
    }

    // количество пользователей
    public int usersCount() {
        int count = 0;
        try {
            String query = "SELECT COUNT(*) FROM users";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            rs.next();
            count = rs.getInt(1);

            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("usersCount error: " + e.getMessage());
        }
        return count;
    }

    // Обновление настроек
    public void updateSettings(String key, String value) {
        try {
            String query = "UPDATE settings SET value = ? WHERE key = ? AND user_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, value);
            statement.setString(2, key);
            statement.setInt(3, Login.userId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("updateSettings error: " + e.getMessage());
        }
    }

    // Добавление веса новости
    public void updateWeight(String title, int value) {
        try {
            String query = "UPDATE all_news SET weight = ?, add_weight_user_id = ?, " +
                    "add_weight_date = datetime('now', 'localtime') WHERE title = ?";

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, value);
            statement.setInt(2, Login.userId);
            statement.setString(3, title);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException sql) {
            Common.showAlert("updateWeight error: " + sql.getMessage());
        }
    }

    // Добавление чувств после прочтения новости
    public void updateFeeling(String title, String value) {
        try {
            String query = "UPDATE all_news SET feel = ?, add_feel_user_id = ?, " +
                    "add_feel_date = datetime('now', 'localtime') WHERE title = ?";

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, value);
            statement.setInt(2, Login.userId);
            statement.setString(3, title);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException sql) {
            Common.showAlert("updateFeeling error: " + sql.getMessage());
        }
    }

    // Изменение чувств и веса правил
    public void updateRules(String like, String value, String type) {
        try {
            String query;

            if (type.equals("feel")) {
                query = "UPDATE feelings SET feeling = ?, update_user_id = ?, " +
                        "update_date = datetime('now', 'localtime') WHERE like = ?";
            } else if (type.equals("call-order")) {
                query = "UPDATE feelings SET call_order = ?, update_user_id = ?, " +
                        "update_date = datetime('now', 'localtime') WHERE like = ?";
            } else {
                query = "UPDATE feelings SET weight = ?, update_user_id = ?, " +
                        "update_date = datetime('now', 'localtime') WHERE like = ?";
            }

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, value);
            statement.setInt(2, Login.userId);
            statement.setString(3, like);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException sql) {
            Common.showAlert("updateRuleFeeling error: " + sql.getMessage());
        }
    }

    // обновление статуса чекбокса
    public void updateIsActiveCheckboxes(boolean check, String name, String type) {
        String query = null;
        try {
            switch (type) {
                case "rss":
                    query = "UPDATE rss_list SET is_active = ? WHERE source = ? AND user_id = ?";
                    break;
                case "keywords":
                    query = "UPDATE keywords SET is_active = ? WHERE word = ? and user_id = ?";
                    break;
                case "feel":
                    query = "UPDATE feelings SET is_active = ? WHERE like = ? and user_id = ?";
                    break;
            }

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setBoolean(1, check);
            statement.setString(2, name);
            statement.setInt(3, Login.userId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("updateIsActiveStatus error: " + e.getMessage());
        }
    }

    public void updateIsActiveDates(boolean check, String type, String description) {
        try {
            String query = "UPDATE dates SET is_active = ? WHERE type = ? and description = ? and main.dates.user_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setBoolean(1, check);
            statement.setString(2, type);
            statement.setString(3, description);
            statement.setInt(4, Login.userId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("updateIsActiveStatus error: " + e.getMessage());
        }
    }

    public void updateIsActiveCountry(boolean check, String country, String source) {
        try {
            String query = "UPDATE rss_list SET is_active = ? WHERE country = ? and source = ? and user_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setBoolean(1, check);
            statement.setString(2, country);
            statement.setString(3, source);
            statement.setInt(4, Login.userId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("updateIsActiveCountry error: " + e.getMessage());
        }
    }

    public void updateRssPosition(int position, String country, String source) {
        try {
            String query = "UPDATE rss_list SET position = ? WHERE country = ? and source = ? and user_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, position);
            statement.setString(2, country);
            statement.setString(3, source);
            statement.setInt(4, Login.userId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("updateRssPosition error: " + e.getMessage());
        }
    }

    public void initUser(String userCountry, String lang) {
        List<String> data = getInitQueries();
        List<String> rssList = getRssQueries(userCountry);
        data.addAll(rssList);

        try {
            connection.setAutoCommit(false);
            PreparedStatement statement;
            for (String query : data) {
                statement = connection.prepareStatement(query);
                statement.setInt(1, Login.userId);
                statement.executeUpdate();
            }
            updateSettings("lang", lang);
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            Common.showAlert("initUser error: " + e.getMessage());
        }
    }

    public void updateUserPassword(int id, String newPassword) {
        try {
            String query = "UPDATE users SET password = ? WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, newPassword);
            statement.setInt(2, id);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("updateUserPassword error: " + e.getMessage());
        }
    }

    public void autoUpdateFeelings(List<Feelings> fromFeelings) {
        String query = "update all_news set feel = " +
                "(select feeling from feelings where is_active = 1 and like like '%' || ? || '%' order by call_order)," +
                "       weight = " +
                "(select weight from feelings where is_active = 1 and like like '%' || ? || '%' order by call_order)," +
                "       add_feel_user_id = ?," +
                "       add_feel_date = datetime('now', 'localtime')" +
                " where news_date BETWEEN datetime('now', '-7 days', 'localtime') AND datetime('now', 'localtime') " +
                "   and title_lower like '%' || ? || '%'";
        try {
            PreparedStatement statement = connection.prepareStatement(query);

            for (Feelings feel : fromFeelings) {
                Gui.searchAnimationLabel.setText("Analysis: " + feel.getLike());

                statement.setString(1, feel.getLike());
                statement.setString(2, feel.getLike());
                statement.setInt(3, Login.userId);
                statement.setString(4, feel.getLike());
                statement.executeUpdate();
            }
            statement.close();
        } catch (Exception e) {
            Common.showAlert("autoUpdateFeelings error: " + e.getMessage());
        }
    }

    public void vacuum() {
        try {
            PreparedStatement statement = connection.prepareStatement("VACUUM");
            statement.execute();
            statement.close();
        } catch (Exception e) {
            Common.showAlert("vacuum error: " + e.getMessage());
        }
    }

}
