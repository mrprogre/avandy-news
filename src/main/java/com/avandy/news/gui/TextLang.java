package com.avandy.news.gui;

import com.avandy.news.utils.Common;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TextLang {
    public String name, owner, ver, dat, donate, register, registerRusPro, amountOfNewsLabelInitText, findInLabelText, latestNewsCheckboxText, lblKeywordsSearchText,
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
            assistPeriod, assistStartUp, reminderToday, reminderTomorrow, reminderWillBe, reminderIs,
            checkAddedRss, checkAddedRssFailed;

    public void setUiText() {
        String uiLang = Common.getUiLang();

        if (uiLang.equals("en")) {
            name = "Avandy News Analysis";
            owner = "Developer: Chernyavskiy Dmitry Andreevich";
            ver = "Version: ";
            dat = " dated ";
            donate = "Donate";
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
            reminderToday = "Today";
            reminderTomorrow = "Tomorrow";
            reminderWillBe = "will be";
            reminderIs = "is";
            checkAddedRss = "Source checking.. press OK and wait please!";
            checkAddedRssFailed = "It's impossible to obtain data from<br/>";
            register = "Computer program registry: num 2023615114 dated 10.03.2023";
            registerRusPro = "Register of Russian software: num 17539 dated 17.05.2023";
        } else if (uiLang.equals("ru")) {
            name = "Аванди Ньюс Анализ";
            owner = "Разработчик: Чернявский Дмитрий Андреевич";
            ver = "Версия: ";
            dat = " дата ";
            donate = "Поддержать проект (Официальный перевод ИП. С данной суммы мы уплатим налог)";
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
            reminderToday = "Сегодня";
            reminderTomorrow = "Завтра";
            reminderWillBe = "исполняется";
            reminderIs = "исполнилось";
            checkAddedRss = "Проверка источника.. нажмите ОК и немного подождите!";
            checkAddedRssFailed = "Невозможно прочитать данные из источника<br/>";
            register = "Реестр программ для ЭВМ: запись 2023615114 от 10.03.2023";
            registerRusPro = "Реестр российского ПО: запись 17539 от 17.05.2023";
        }
    }

}
