package com.avandy.news.gui;

import com.avandy.news.utils.Common;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TextLang {
    public String amountOfNewsLabelInitText, findInLabelText, latestNewsCheckboxText, lblKeywordsSearchText,
            newsInArchiveLabelText, loginLabelText, loginLabelHelloText, loginLabelPwdText, selectText, reloadText,
            reloadDbText, pathToDatabaseText, amountOfNewsLabelText, totalText, transparencyText, languageText,
            interfaceXText, interfaceYText, assistantText, autoFeelText, tableFontNameText, fontSizeText, rowHeightText,
            interfaceColorText, tablesColorText, topTenUniqueText, topTenSimilarText,
            tablesAlternateText, fontColorText, databaseReloadText, jaroWinklerText, jaroWinklerText2,
            jaroWinklerText3, jaroWinklerText4, jaroWinklerText5, jaroWinklerText6, jaroWinklerText7,
            rightClickMenuDescribeText, rightClickMenuToFavoritesText, rightClickMenuCopyText,
            rightClickMenuAddHeadlineText, rightClickMenuRemoveText, rightClickMenuClearText,
            rightClickMenuAddWordToText, rightClickMenuKeywordsText, rightClickMenuKeywordsToolpitText,
            rightClickMenuExcludedText, rightClickMenuExcludedTooltipText, settingsDialogText,
            assistWelcome, assistQuestion, assistFind, assistAllNews, assistKeywords, assistOneWord,
            assistPeriod, assistStartUp;

    public void setUiText() {
        String uiLang = Common.getUiLang();

        if (uiLang.equals("en")) {
            amountOfNewsLabelInitText = "Avandy News Analysis";
            amountOfNewsLabelText = "total: ";
            findInLabelText = "find in";
            latestNewsCheckboxText = " latest news";
            lblKeywordsSearchText = "Search by keywords";
            newsInArchiveLabelText = "News in archive: ";
            loginLabelText = "user: ";
            loginLabelHelloText = "Hello, ";
            loginLabelPwdText = "Right click to change password";
            selectText = "Select";
            reloadText = "Reload";
            pathToDatabaseText = "Path to database:";
            reloadDbText = "Reload database: ";
            totalText = "total: %d, shown: %d, excluded: %d (%s)";
            transparencyText = "Transparency ";
            languageText = "Language";
            interfaceXText = "Interface X";
            interfaceYText = "Interface Y";
            assistantText = "Assistant";
            autoFeelText = "Auto feel";
            tableFontNameText = "Table font name";
            fontSizeText = "Font size";
            rowHeightText = "Row height";
            interfaceColorText = "Interface color";
            tablesColorText = "Tables color";
            tablesAlternateText = "Tables alternate";
            fontColorText = "Font color";
            databaseReloadText = "Database reloaded, current size: ";
            jaroWinklerText = "Jaro-Winkler (level %)";
            jaroWinklerText2 = "Jaro-Winkler";
            jaroWinklerText3 = "Compare two strings";
            jaroWinklerText4 = "Compare";
            jaroWinklerText5 = "Exit";
            jaroWinklerText6 = "First string:";
            jaroWinklerText7 = "Second string:";
            rightClickMenuDescribeText = "Describe";
            rightClickMenuToFavoritesText = "to favorites";
            rightClickMenuCopyText = "copy";
            rightClickMenuAddHeadlineText = "add headline";
            rightClickMenuRemoveText = "remove";
            rightClickMenuClearText = "clear";
            rightClickMenuAddWordToText = "Add word to";
            rightClickMenuKeywordsText = "Keywords";
            rightClickMenuKeywordsToolpitText = "add to keywords list";
            rightClickMenuExcludedText = "Excluded";
            rightClickMenuExcludedTooltipText = "add to excluded list";
            settingsDialogText = "Settings";
            topTenSimilarText = "sum of similar words";
            topTenUniqueText = "unique words";
            assistWelcome = "Welcome to the Virtual Assistant!";
            assistQuestion = "What kind of search are you interested in?";
            assistAllNews = "All news with dropout applied";
            assistKeywords = "News with keywords";
            assistOneWord = "News containing the word";
            assistPeriod = "Search period";
            assistStartUp = "Show on startup";
            assistFind = "Find";
        } else if (uiLang.equals("ru")) {
            amountOfNewsLabelInitText = "Аванди Ньюс Анализ";
            amountOfNewsLabelText = "Всего: ";
            findInLabelText = "Поиск в";
            latestNewsCheckboxText = " Актуальное";
            lblKeywordsSearchText = "Поиск по ключевым словам";
            newsInArchiveLabelText = "Новостей в архиве: ";
            loginLabelText = "пользователь: ";
            loginLabelHelloText = "Привет, ";
            loginLabelPwdText = "Нажмите правой кнопкой, чтобы изменить пароль";
            selectText = "Выбрать";
            reloadText = "Запустить";
            pathToDatabaseText = "Путь к базе данных:";
            reloadDbText = "Перезапуск базы: ";
            totalText = "Всего: %d, показано: %d, исключено: %d (%s)";
            transparencyText = "Прозрачность ";
            languageText = "Язык интерфейса";
            interfaceXText = "Позиция X";
            interfaceYText = "Позиция Y";
            assistantText = "Ассистент";
            autoFeelText = "Автоустановка ощущений";
            tableFontNameText = "Название шрифта";
            fontSizeText = "Размер шрифта";
            rowHeightText = "Высота строки";
            interfaceColorText = "Цвет интерфейса";
            tablesColorText = "Цвет таблицы";
            tablesAlternateText = "Второй увет таблицы";
            fontColorText = "Цвет шрифта";
            databaseReloadText = "База перезагружена, текущий объём: ";
            jaroWinklerText = "Джаро-Винклер (порог %)";
            jaroWinklerText2 = "Джаро-Винклер";
            jaroWinklerText3 = "Сравнить две строки";
            jaroWinklerText4 = "Сравнить";
            jaroWinklerText5 = "Выход";
            jaroWinklerText6 = "Первая строка:";
            jaroWinklerText7 = "Вторая строка:";
            rightClickMenuDescribeText = "Подробнее";
            rightClickMenuToFavoritesText = "в избранное";
            rightClickMenuCopyText = "копировать";
            rightClickMenuAddHeadlineText = "добавить новость";
            rightClickMenuRemoveText = "удалить строку";
            rightClickMenuClearText = "очистить всё";
            rightClickMenuAddWordToText = "Добавить слово в";
            rightClickMenuKeywordsText = "Ключевые слова";
            rightClickMenuKeywordsToolpitText = "добавить в список ключевых слов";
            rightClickMenuExcludedText = "Слова-исключения";
            rightClickMenuExcludedTooltipText = "добавить в список слов-исключений";
            settingsDialogText = "Настройки";
            topTenSimilarText = "сумма схожих слов";
            topTenUniqueText = "уникальные слова";
            assistWelcome = "Вас приветствует виртуальный ассистент!";
            assistQuestion = "Какой вид поиска Вас интересует?";
            assistAllNews = "Все новости с применением отсева";
            assistKeywords = "Новости с ключевыми словами";
            assistOneWord = "Новости, содержащие слово";
            assistPeriod = "Период поиска";
            assistStartUp = "Показывать при запуске";
            assistFind = "Найти";
        }
    }

}
