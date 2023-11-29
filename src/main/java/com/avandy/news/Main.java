package com.avandy.news;

import com.avandy.news.database.JdbcQueries;
import com.avandy.news.database.SQLite;
import com.avandy.news.utils.Common;
import com.avandy.news.utils.Login;
import com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme;

import javax.swing.*;

public class Main {
    // При обновлении менять версию в поле "ver" version.txt
    public static final String APP_VERSION = "1.0.0.8";
    public static final String APP_VERSION_DATE = "29.11.2023";

    public static void main(String[] args) {
        // установка темы окна авторизации
        FlatCobalt2IJTheme.setup();
        UIManager.put("Button.arc", 8);

        // создание файлов программы при первом запуске
        Common.createFiles();

        // подключение к базе данных
        new SQLite().openConnection();

        // определение пользователя
        if (new JdbcQueries().usersCount() == 1) {
            Login.userId = 0; Login.username = "demo";
        } else {
            new Login().login();
        }

        // отображение интерфейса
        Common.showGui();

        // показать ассистента
        if (Common.isAssistant()) Common.showAssistant();
    }

}