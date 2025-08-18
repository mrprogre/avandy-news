package com.avandy.news.utils;

import com.avandy.news.database.JdbcQueries;
import com.avandy.news.gui.Dialogs;
import com.avandy.news.gui.FrameDragListener;
import com.avandy.news.gui.Gui;
import com.avandy.news.gui.Icons;
import com.avandy.news.gui.TextLang;
import com.avandy.news.model.Excluded;
import com.avandy.news.model.GuiSize;
import com.avandy.news.model.Headline;
import com.avandy.news.parser.ParserJsoup;
import com.avandy.news.parser.ParserRome;
import com.avandy.news.search.Search;
import com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import lombok.experimental.UtilityClass;
import org.apache.commons.text.similarity.JaroWinklerDistance;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.avandy.news.gui.TextLang.*;

@UtilityClass
public class Common {
    public static Gui gui;
    private static final String USER_HOME_DIR = System.getProperty("user.home") + File.separator;
    public static final String DIRECTORY_PATH = USER_HOME_DIR + "News" + File.separator;
    public static final String DATABASE_PATH = USER_HOME_DIR + "News" + File.separator + "news.db";
    public final AtomicBoolean IS_SENDING = new AtomicBoolean(true);
    public final HashMap<String, Integer> guiSettings = new HashMap<>();
    public final HashMap<String, Color> guiColors = new HashMap<>();
    public static JComboBox<String> assistantInterval;
    public final JComboBox<String> countriesCombobox = new JComboBox<>(
            new String[]{"all", "Russia", "USA", "France", "Germany", "UK", "Africa"});
    public static final int READ_TIMEOUT = 3000;
    public static final int CONNECT_TIMEOUT = 3000;

    // создание файлов и директорий
    public static void createFiles() {
        // main directory create
        File mainDirectory = new File(DIRECTORY_PATH);
        if (!mainDirectory.exists()) mainDirectory.mkdirs();

        String pathToDatabase = DIRECTORY_PATH + "news.db";
        File dbIsExists = new File(pathToDatabase);
        if (!dbIsExists.exists()) {
            copyFiles(Common.class.getResource("/news.db"), pathToDatabase);
        }

        File sqliteExeIsExists = new File(DIRECTORY_PATH + "sqlite3.exe");
        if (!sqliteExeIsExists.exists()) {
            copyFiles(Common.class.getResource("/sqlite3.exe"), DIRECTORY_PATH + "sqlite3.exe");
        }
        File manualIsExists = new File(DIRECTORY_PATH + "manual.docx");
        if (!manualIsExists.exists()) {
            copyFiles(Common.class.getResource("/manual.docx"), DIRECTORY_PATH + "manual.docx");
        }
        File gameIsExists = new File(DIRECTORY_PATH + "brain-shake-game.jar");
        if (!gameIsExists.exists()) {
            copyFiles(Common.class.getResource("/brain-shake-game.jar"), DIRECTORY_PATH +
                    "brain-shake-game.jar");
        }
        File sqliteConfigIsExists = new File(USER_HOME_DIR + ".sqliterc");
        if (!sqliteConfigIsExists.exists()) {
            copyFiles(Common.class.getResource("/.sqliterc"), USER_HOME_DIR + ".sqliterc");
        }
    }

    public void showGui() {
        JdbcQueries jdbcQueries = new JdbcQueries();
        setGuiSettings(jdbcQueries, jdbcQueries.getSetting("gui_theme"));
        setUiColors(jdbcQueries);
        setTheme();
        TextLang.setUiText();
        createGui();
        assistantInterval = new JComboBox<>(Gui.INTERVALS);
        getSettingsAfterGui();

        // напоминание о событиях из раздела dates
        new Reminder().remind();
    }

    public static boolean isAssistant() {
        return new JdbcQueries().getSetting("is_assist").equals("on");
    }

    public static boolean isAutoFeel() {
        return new JdbcQueries().getSetting("is_auto_feel").equals("on");
    }

    public static boolean isFeelAndWeight() {
        return new JdbcQueries().getSetting("is_feel_and_weight").equals("on");
    }

    // подсчёт ощущений от прочтения новостей за период с выводом значения в консоль
    public void calcBalanceFeelingsByPeriod() {
        int balanceRSS;
        int positiveCount = 0;
        int negativeCount = 0;
        int balanceArchive;
        int positiveCountArchive = 0;
        int negativeCountArchive = 0;
        String interval = getStringIntervalForQuery();
        HashMap<String, String> feelingsFromAllNews = new JdbcQueries().getFeelingsFromAllNews(interval);

        // RSS
        List<Headline> dataFromMainTable = Search.getDataFromMainTable();

        for (Headline feel : dataFromMainTable) {
            if (feel.getFeel() != null) {
                if (feel.getFeel().equals("-")) {
                    negativeCount++;
                } else if (feel.getFeel().equals("+")) {
                    positiveCount++;
                }
            }
        }
        balanceRSS = positiveCount - negativeCount;

        // ARCHIVE
        for (String feel : feelingsFromAllNews.values()) {
            if (feel.equals("-")) {
                negativeCountArchive++;
            } else if (feel.equals("+")) {
                positiveCountArchive++;
            }
        }
        balanceArchive = positiveCountArchive - negativeCountArchive;

        String searchSource = Objects.requireNonNull(Gui.resourceCombobox.getSelectedItem()).toString();

        if (Common.isFeelAndWeight()) {
            if (searchSource.equals("arc") || searchSource.equals("архиве")) {
                console("balance of feelings: " + balanceArchive +
                        " (+" + positiveCountArchive + " -" + negativeCountArchive + ")");
            } else {
                console("balance of feelings:\n" +
                        "actual: " + balanceRSS + " (+" + positiveCount + " -" + negativeCount + ")\n" +
                        "archive: " + balanceArchive + " (+" + positiveCountArchive + " -" + negativeCountArchive + ")"
                );
            }
        }
    }

    // Интервал поиска новостей в виде строки для выборки данных из диапазона
    public String getStringIntervalForQuery() {
        String interval = Objects.requireNonNull(Gui.searchInterval.getSelectedItem())
                .toString()
                .replace("all", "87600 hours"); // 87600 hours = 10 years

        if (interval.contains("min")) {
            interval = interval.replace("min", "minutes");
        }
        return interval;
    }

    // создание интерфейса
    private void createGui() {
        gui = new Gui(guiSettings, guiColors);
        Runnable runnable = () -> {
            FrameDragListener frameDragListener = new FrameDragListener(gui);
            gui.addMouseListener(frameDragListener);
            gui.addMouseMotionListener(frameDragListener);
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void setGuiSettings(JdbcQueries jdbcQueries, String guiTheme) {
        guiSettings.put("guiX", Integer.parseInt(jdbcQueries.getSetting("gui_x")));
        guiSettings.put("guiY", Integer.parseInt(jdbcQueries.getSetting("gui_y")));

        if (GuiSize.LARGE.getSize().equals(guiTheme)) {
            guiSettings.put("width", 1681);
            guiSettings.put("height", 948);
            guiSettings.put("mainTableWidth", 1360);
            guiSettings.put("mainTableHeight", 897);
            guiSettings.put("topLeftX", 10);
            guiSettings.put("topLeftY", 9);
            guiSettings.put("bottomLeftX", 1228);
            guiSettings.put("bottomLeftY", 9);
            guiSettings.put("topTenX", 1380);
            guiSettings.put("topTenY", 40);
            guiSettings.put("topTenHeight", 237);
            guiSettings.put("consoleX", 1380);
            guiSettings.put("consoleY", 303);
            guiSettings.put("consoleHeight", 618);
            guiSettings.put("consoleWidth", 290);
            guiSettings.put("menuArchiveX", 1380);
            guiSettings.put("menuArchiveY", 924);
            guiSettings.put("toolTipShowLength", 113);
            guiSettings.put("searchAnimationLabelWidth", 762);
        } else if (GuiSize.MIDDLE.getSize().equals(guiTheme)) {
            guiSettings.put("width", 1281);
            guiSettings.put("height", 648);
            guiSettings.put("mainTableWidth", 960);
            guiSettings.put("mainTableHeight", 597);
            guiSettings.put("topLeftX", 10);
            guiSettings.put("topLeftY", 9);
            guiSettings.put("bottomLeftX", 828);
            guiSettings.put("bottomLeftY", 9);
            guiSettings.put("topTenX", 980);
            guiSettings.put("topTenY", 40);
            guiSettings.put("topTenHeight", 237);
            guiSettings.put("consoleX", 980);
            guiSettings.put("consoleY", 303);
            guiSettings.put("consoleHeight", 320);
            guiSettings.put("consoleWidth", 290);
            guiSettings.put("menuArchiveX", 980);
            guiSettings.put("menuArchiveY", 625);
            guiSettings.put("toolTipShowLength", 75);
            guiSettings.put("searchAnimationLabelWidth", 362);
        } else if (GuiSize.SMALL.getSize().equals(guiTheme)) {
            guiSettings.put("width", 1181);
            guiSettings.put("height", 554);
            guiSettings.put("mainTableWidth", 860);
            guiSettings.put("mainTableHeight", 504);
            guiSettings.put("topLeftX", 10);
            guiSettings.put("topLeftY", 9);
            guiSettings.put("bottomLeftX", 728);
            guiSettings.put("bottomLeftY", 9);
            guiSettings.put("topTenX", 880);
            guiSettings.put("topTenY", 40);
            guiSettings.put("topTenHeight", 237);
            guiSettings.put("consoleX", 880);
            guiSettings.put("consoleY", 303);
            guiSettings.put("consoleWidth", 290);
            guiSettings.put("consoleHeight", 224);
            guiSettings.put("menuArchiveX", 880);
            guiSettings.put("menuArchiveY", 530);
            guiSettings.put("toolTipShowLength", 58);
            guiSettings.put("searchAnimationLabelWidth", 262);
        }
    }

    private void setUiColors(JdbcQueries jdbcQueries) {
        String[] guiColor = jdbcQueries.getSetting("gui_color").split(",");
        String[] fontColors = jdbcQueries.getSetting("font_color").split(",");
        String[] tablesColors = jdbcQueries.getSetting("tables_color").split(",");
        String[] tablesAltColors = jdbcQueries.getSetting("tables_alt_color").split(",");

        guiColors.put("guiColor", new Color(Integer.parseInt(guiColor[0]), Integer.parseInt(guiColor[1]),
                Integer.parseInt(guiColor[2])));
        guiColors.put("fontColor", new Color(Integer.parseInt(fontColors[0]), Integer.parseInt(fontColors[1]),
                Integer.parseInt(fontColors[2])));
        guiColors.put("tablesColor", new Color(Integer.parseInt(tablesColors[0]), Integer.parseInt(tablesColors[1]),
                Integer.parseInt(tablesColors[2])));
        guiColors.put("tablesAltColor", new Color(Integer.parseInt(tablesAltColors[0]), Integer.parseInt(tablesAltColors[1]),
                Integer.parseInt(tablesAltColors[2])));
    }

    private void setTheme() {
        UIManager.put("Component.arc", 10);
        UIManager.put("ProgressBar.arc", 6);
        UIManager.put("Button.arc", 8);
        UIManager.put("Table.background", guiColors.get("tablesColor"));
        UIManager.put("Table.foreground", guiColors.get("fontColor"));
        UIManager.put("TextField.background", Color.GRAY);
        UIManager.put("TextField.foreground", Color.BLACK);
        FlatHiberbeeDarkIJTheme.setup();
    }

    // Считывание конфигураций после запуска интерфейса
    public void getSettingsAfterGui() {
        JdbcQueries jdbcQueries = new JdbcQueries();
        intervalMapper(jdbcQueries.getSetting("interval"));
        Gui.latestNewsCheckbox.setState(Boolean.parseBoolean(jdbcQueries.getSetting("onlyNewNews")));
        Gui.resourceCombobox.setSelectedItem(jdbcQueries.getSetting("resource"));
        Gui.isOnlyLastNews = Gui.latestNewsCheckbox.getState();
        Gui.consoleTextArea.setBackground(guiColors.get("tablesColor"));
    }

    // сохранение состояния окна в database.tmp
    public void saveState() {
        JdbcQueries jdbcQueries = new JdbcQueries();
        String interval = Objects.requireNonNull(Gui.searchInterval.getSelectedItem()).toString()
                .replace(" hour", "h")
                .replace("s", "")
                .replace(" min", "m");
        String resource = Objects.requireNonNull(Gui.resourceCombobox.getSelectedItem()).toString();

        jdbcQueries.updateSettings("interval", interval);
        jdbcQueries.updateSettings("resource", resource);
        jdbcQueries.updateSettings("onlyNewNews", String.valueOf(Gui.latestNewsCheckbox.getState()));
    }

    //Console
    public void console(String text) {
        String line = "- - - - - - - - - - - - - - - - - - - - - - - - - - ";

        if (OsChecker.isUnix()) {
            line = "- - - - - - - - - - - - - - - - - - - - - - - - - - - ";
        }

        Gui.consoleTextArea.append(line + text + "\n");
    }

    // Шкала прогресса
    public void fillProgressLine() {
        int counter = 0;
        while (!Search.isSearchFinished.get() || !IS_SENDING.get()) {
            if (!IS_SENDING.get()) Gui.progressBar.setForeground(new Color(255, 115, 0));
            else Gui.progressBar.setForeground(new Color(106, 255, 235));
            counter = getCounter(counter);
        }
    }

    private static int getCounter(int counter) {
        if (counter == 99) {
            counter = 0;
        }
        Gui.progressBar.setValue(counter);
        try {
            Thread.sleep(7);
        } catch (InterruptedException ignored) {
        }
        counter++;
        return counter;
    }

    // Интервал поиска/таймера в секундах
    int getInterval() {
        int minutes;
        if (Objects.requireNonNull(Gui.searchInterval.getSelectedItem()).toString().contains(" min")) {
            minutes = Integer.parseInt(Objects.requireNonNull(Gui.searchInterval
                            .getSelectedItem())
                    .toString()
                    .replace(" min", ""));
        } else if (Objects.requireNonNull(Gui.searchInterval.getSelectedItem()).toString().contains("all")) {
            minutes = 240000;
        } else {
            minutes = Integer.parseInt(Objects.requireNonNull(Gui.searchInterval
                            .getSelectedItem())
                    .toString()
                    .replace(" hour", "")
                    .replace("s", "")) * 60;
        }
        return minutes;
    }

    // Сравнение дат для отображения новостей по интервалу (Gui.newsInterval)
    public int compareDatesOnly(Date now, Date in) {
        int minutes = Common.getInterval();

        Calendar minus = Calendar.getInstance();
        minus.setTime(new Date());
        minus.add(Calendar.MINUTE, -minutes);
        Calendar now_cal = Calendar.getInstance();
        now_cal.setTime(now);

        if (in.after(minus.getTime()) && in.before(now_cal.getTime())) {
            return 1;
        } else
            return 0;
    }

    // Копирование файлов из jar
    public void copyFiles(URL p_file, String copy_to) {
        File copied = new File(copy_to);
        try (InputStream in = p_file.openStream();
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(copied.toPath()))) {
            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
        } catch (Exception ignored) {
        }
    }

    public void saveColor(String type, Color color) {
        new JdbcQueries().updateSettings(type, color.getRed() + "," + color.getGreen() + "," + color.getBlue());
    }

    public void setDefaultColors() {
        JdbcQueries jdbcQueries = new JdbcQueries();
        jdbcQueries.updateSettings("font_color", "0,0,0");
        jdbcQueries.updateSettings("gui_color", "0,51,102");
        jdbcQueries.updateSettings("tables_color", "255,255,255");
        jdbcQueries.updateSettings("tables_alt_color", "237,237,237");
    }

    public void setRandomColors() {
        int maxColorValue = 180;
        JdbcQueries jdbcQueries = new JdbcQueries();
        Random random = new Random();

        int guiRed = random.nextInt(maxColorValue);
        int guiGreen = random.nextInt(maxColorValue);
        int guiBlue = random.nextInt(maxColorValue);
        String gui = String.join(",", String.valueOf(guiRed), String.valueOf(guiGreen),
                String.valueOf(guiBlue));

        int tablesRed = random.nextInt(maxColorValue);
        int tablesGreen = random.nextInt(maxColorValue);
        int tablesBlue = random.nextInt(maxColorValue);

        if (tablesRed < 25) tablesRed = 25;
        if (tablesGreen < 25) tablesGreen = 25;
        if (tablesBlue < 25) tablesBlue = 25;

        String tables = String.join(",", String.valueOf(tablesRed), String.valueOf(tablesGreen),
                String.valueOf(tablesBlue));
        String tablesAlt = String.join(",", String.valueOf(tablesRed - 25),
                String.valueOf(tablesGreen - 25), String.valueOf(tablesBlue - 25));

        jdbcQueries.updateSettings("gui_color", gui);
        jdbcQueries.updateSettings("tables_color", tables);
        jdbcQueries.updateSettings("tables_alt_color", tablesAlt);

        // Если фон светлый, то шрифт чёрный
        if (1 - (0.299 * tablesRed + 0.587 * tablesGreen + 0.114 * tablesBlue) / 255 < 0.5) {
            jdbcQueries.updateSettings("font_color", "0,0,0");
        } else {
            jdbcQueries.updateSettings("font_color", "240,240,240");
        }
    }

    // преобразование строки в строку с хэш-кодом
    public String getHash(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5"); // MD2, MD5, SHA-1, SHA-256, SHA-384, SHA-512
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // преобразование интервала
    private static void intervalMapper(String interval) {
        switch (interval) {
            case "1h":
                Gui.searchInterval.setSelectedItem(interval.replace("h", "") + " hour");
                assistantInterval.setSelectedItem(interval.replace("h", "") + " hour");
                break;
            case "1m":
            case "5m":
            case "15m":
            case "30m":
            case "45m":
                Gui.searchInterval.setSelectedItem(interval.replace("m", "") + " min");
                assistantInterval.setSelectedItem(interval.replace("m", "") + " min");
                break;
            case "all":
                Gui.searchInterval.setSelectedItem("all");
                assistantInterval.setSelectedItem("all");
                break;
            default:
                Gui.searchInterval.setSelectedItem(interval.replace("h", "") + " hours");
                assistantInterval.setSelectedItem(interval.replace("h", "") + " hours");
                break;
        }
    }

    // отсеивание описаний содержащих недопустимые символы
    public boolean isHref(String newsDescribe) {
        return newsDescribe.contains("<img")
                || newsDescribe.contains("href")
                || newsDescribe.contains("<div")
                || newsDescribe.contains("&#34")
                || newsDescribe.contains("<p lang")
                || newsDescribe.contains("&quot")
                || newsDescribe.contains("<span")
                || newsDescribe.contains("<ol")
                || newsDescribe.isEmpty();
    }

    public static String getNameFromUrl(String url) {
        String urlToConsole = "empty";
        if (url != null) {
            urlToConsole = url.replaceAll(("https://|http://|www."), "").concat("/");
            urlToConsole = urlToConsole.substring(0, urlToConsole.indexOf("/"));
        }
        return urlToConsole;
    }

    public static void showAlert(String message) {
        JOptionPane.showMessageDialog(Gui.mainTableScrollPane, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static void showAlertHtml(String message) {
        JLabel label = new JLabel("<html>" + message + "</<html>");
        JOptionPane.showMessageDialog(Gui.mainTableScrollPane, label, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static void showInfo(String message) {
        JOptionPane.showMessageDialog(Gui.mainTableScrollPane, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showInfoHtml(String message) {
        JLabel label = new JLabel("<html>" + message + "</<html>");
        JOptionPane.showMessageDialog(Gui.mainTableScrollPane, label, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    // Оставляет только буквы
    public static String keepOnlyLetters(String word) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            if (Character.isLetter(word.charAt(i)) || word.contains("-"))
                sb.append(word.charAt(i));
        }
        return sb.toString();
    }

    public void showAssistant() {
        Font font = new Font("Tahoma", Font.BOLD, 12);
        Color fontColor = new Color(212, 235, 255);
        if (OsChecker.isUnix()) font = new Font("Tahoma", Font.BOLD, 11);

        JFrame frame = new JFrame(assistWelcome);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setBounds(100, 100, 475, 265);
        frame.setResizable(false);
        frame.setIconImage(Icons.LOGO_ICON.getImage());
        frame.setAlwaysOnTop(false);
        frame.getContentPane().setLayout(null);
        frame.setLocationRelativeTo(Gui.mainTableScrollPane);
        frame.setFont(font);

        JLabel whatDoYouWantLabel = new JLabel(assistQuestion);
        whatDoYouWantLabel.setForeground(fontColor);
        whatDoYouWantLabel.setFont(font);
        whatDoYouWantLabel.setBounds(10, 5, 400, 30);

        JLabel allNewsLabel = new JLabel(assistAllNews);
        allNewsLabel.setFont(font);
        JButton allNews = new JButton(assistFind);
        allNews.setForeground(fontColor);
        allNews.addActionListener(e -> {
            Gui.keyword.setText("");
            Gui.searchByKeyword.doClick();
            Gui.searchInterval.setSelectedItem(assistantInterval.getSelectedItem());
        });

        JButton showExcluded = new JButton("Excluded");
        showExcluded.setForeground(fontColor);
        showExcluded.addActionListener(x -> new Dialogs("dialog_excluded_headlines"));

        JLabel keywordsLabel = new JLabel(assistKeywords);
        keywordsLabel.setFont(font);
        JButton keywords = new JButton(assistFind);
        keywords.setForeground(fontColor);
        keywords.addActionListener(e -> {
            Gui.keyword.setText("");
            Gui.searchInterval.setSelectedItem(assistantInterval.getSelectedItem());
            Gui.searchByKeywords.doClick();
        });
        JButton showKeywords = new JButton("Keywords");
        showKeywords.setForeground(fontColor);
        showKeywords.addActionListener(x -> new Dialogs("dialog_keywords"));

        JLabel oneWordLabel = new JLabel(assistOneWord);
        oneWordLabel.setFont(font);
        JTextField word = new JTextField(6);
        word.setFont(font);
        //gui.getInputContext().selectInputMethod(new Locale("ru", "RU"));
        JButton oneWord = new JButton(assistFind);
        oneWord.setForeground(fontColor);
        oneWord.addActionListener(e -> {
            if (!word.getText().isEmpty()) {
                Gui.searchInterval.setSelectedItem(assistantInterval.getSelectedItem());
                Gui.keyword.setText(word.getText());
                Gui.searchByKeyword.doClick();
            }
        });

        JLabel assistantLabel = new JLabel(assistPeriod);
        assistantLabel.setFont(font);

        JCheckBox isAssist = new JCheckBox(assistStartUp);
        isAssist.setForeground(fontColor);
        isAssist.setContentAreaFilled(false);
        isAssist.setSelected(isAssistant());
        isAssist.addItemListener(e -> {
            String isOn = isAssist.isSelected() ? "on" : "off";
            new JdbcQueries().updateSettings("is_assist", isOn);
        });

        frame.getContentPane().add(whatDoYouWantLabel);
        frame.getContentPane().add(allNewsLabel);
        frame.getContentPane().add(showExcluded);
        frame.getContentPane().add(oneWordLabel);
        frame.getContentPane().add(word);
        frame.getContentPane().add(oneWord);
        frame.getContentPane().add(keywordsLabel);
        frame.getContentPane().add(showKeywords);
        frame.getContentPane().add(keywords);
        frame.getContentPane().add(assistantInterval);
        frame.getContentPane().add(allNews);
        frame.getContentPane().add(assistantLabel);
        frame.getContentPane().add(isAssist);

        int x1 = 10;
        int x2 = 250;
        int x3 = 365;
        int y0 = 40;
        int y1 = 80;
        int y2 = 120;
        int y3 = 160;
        int height = 28;
        int width1 = 225;
        int width2 = 100;
        int width3 = 80;

        if (OsChecker.isUnix()) {
            width1 = 235;
            x2 = 255;
        }

        allNewsLabel.setBounds(x1, y0, width1, height);
        showExcluded.setBounds(x2, y0, width2, height);
        allNews.setBounds(x3, y0, width3, height);

        keywordsLabel.setBounds(x1, y1, width1, height);
        showKeywords.setBounds(x2, y1, width2, height);
        keywords.setBounds(x3, y1, width3, height);

        oneWordLabel.setBounds(x1, y2, width1, height);
        word.setBounds(x2, y2, width2, height);
        oneWord.setBounds(x3, y2, width3, height);

        assistantLabel.setBounds(x1, y3, width1, height);
        assistantInterval.setBounds(x2, y3, width2, height);
        isAssist.setBounds(x1 - 4, y3 + 35, width1, height);

        frame.setVisible(true);
    }

    public String getDatabaseSize() {
        long size = new File(DATABASE_PATH).length();

        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "Kb", "Mb", "Gb", "Tb"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups))
                + " " + units[digitGroups];
    }

    public String getUiLang() {
        return new JdbcQueries().getSetting("lang");
    }

    public String[] getFonts() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    }

    // Возвращает коллекцию повторяющихся слов с разными окончаниями
    public void fillTopTenWithoutDuplicates(Map<String, Integer> wordsCount) {
        int jaroWinklerLevel = Integer.parseInt(new JdbcQueries().getSetting("jaro-winkler-level"));
        Map<String, Integer> comparedTop10 = new TreeMap<>();
        List<String> excluded = new ArrayList<>();

        for (Map.Entry<String, Integer> topTenWord : wordsCount.entrySet()) {
            for (Map.Entry<String, Integer> topTenWord2 : wordsCount.entrySet()) {
                double compare = jaroWinklerCompare(topTenWord.getKey(), topTenWord2.getKey());

                if (compare != 100 && compare >= jaroWinklerLevel && !excluded.contains(topTenWord.getKey())) {
                    String commonString = longestCommonSubstring(topTenWord.getKey(), topTenWord2.getKey());
                    if (commonString.length() > 3)
                        comparedTop10.put(commonString, comparedTop10.getOrDefault(commonString, 0)
                                + topTenWord.getValue());
                    excluded.add(topTenWord.getKey());
                }
            }
        }

        // сначала отображаются схожие слова
        if (!comparedTop10.isEmpty()) {
            // Удаление исключённых слов из мап слов имеющих общие корни
            for (Excluded word : Search.excludedWordsTopTen) {
                comparedTop10.remove(word.getWord());
            }
            if (!comparedTop10.isEmpty())
                Gui.modelTopTen.addRow(new Object[]{"- - - - - - - " + topTenSimilarText});
        }

        comparedTop10.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .forEach(x -> Gui.modelTopTen.addRow(new Object[]{x.getKey(), x.getValue()}));

        // потом отображаются уникальные слова
        if (!comparedTop10.isEmpty() && !wordsCount.isEmpty()) Gui.modelTopTen.addRow(
                new Object[]{"- - - - - - - " + topTenUniqueText});
        wordsCount.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .forEach(x -> Gui.modelTopTen.addRow(new Object[]{x.getKey(), x.getValue()}));
    }

    // поиск общей подстроки (полно всяких: ный, ять, акой, ать)
    private String longestCommonSubstring(String s, String t) {
        int[][] table = new int[s.length()][t.length()];
        int longest = 0;
        String result = "";
        for (int i = 0; i < s.length(); i++) {
            for (int j = 0; j < t.length(); j++) {
                if (s.charAt(i) != t.charAt(j)) {
                    continue;
                }
                table[i][j] = (i == 0 || j == 0) ? 1
                        : 1 + table[i - 1][j - 1];
                if (table[i][j] > longest) {
                    longest = table[i][j];
                }
                if (table[i][j] == longest) {
                    result = s.substring(i - longest + 1, i + 1);
                }
            }
        }
        return result;
    }

    // Сравнение двух строк методом Джаро-Винклера
    public void compareTwoStrings() {
        JTextField textField1 = new JTextField();
        JTextField textField2 = new JTextField();
        String[] opt = new String[]{jaroWinklerText5, jaroWinklerText4};

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel(jaroWinklerText6));
        panel.add(textField1);
        panel.add(new JLabel(jaroWinklerText7));
        panel.add(textField2);

        int result = JOptionPane.showOptionDialog(Gui.mainTableScrollPane, panel, "Jaro-Winkler similarity",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                Icons.CHECK_FOR_UPDATES_ICON, opt, opt[1]);

        String String1 = textField1.getText();
        String String2 = textField2.getText();

        if (!String1.isEmpty() && !String2.isEmpty()) {
            if (result == 1) {
                Common.showInfo("String similarity [" + String1 + " : " + String2 + "] is " +
                        jaroWinklerCompare(String1, String2) + "%");
                compareTwoStrings();
            }
        }
    }

    public boolean isDateValid(int month, int day) {
        try {
            LocalDate localDate = LocalDate.of(1987, month, day);
        } catch (DateTimeException e) {
            Common.showAlert("Incorrect date format specified");
            return false;
        }
        return true;
    }
    public static int jaroWinklerCompare(String text1, String text2) {
        return (int) Math.round((1 - new JaroWinklerDistance().apply(text1, text2)) * 100);
    }


    // Проверка источника при добавлении
    public boolean checkRomeRss(String link) {
        SyndFeed syndFeed = new ParserRome().parseFeed(link);

        if (syndFeed != null) {
            for (SyndEntry message : syndFeed.getEntries()) {
                String title = message.getTitle();
                return (title != null && !title.isEmpty());
            }
        }
        return false;
    }


    // Проверка источника при добавлении
    public boolean checkJsoupRss(String link) {
        for (Headline item : new ParserJsoup().parse(link)) {
            String title = item.getTitle();
            return (title != null && !title.isEmpty());
        }
        return false;
    }

}