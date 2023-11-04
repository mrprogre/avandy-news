package com.avandy.news.gui;

import com.avandy.news.Main;
import com.avandy.news.database.JdbcQueries;
import com.avandy.news.database.SQLite;
import com.avandy.news.export.ExportToCsv;
import com.avandy.news.export.ExportToExcel;
import com.avandy.news.model.GuiSize;
import com.avandy.news.search.Search;
import com.avandy.news.utils.*;
import com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.avandy.news.gui.TextLang.*;
import static java.awt.GraphicsDevice.WindowTranslucency.TRANSLUCENT;

public class Gui extends JFrame {
    final SQLite sqLite = new SQLite();
    final JdbcQueries jdbcQueries = new JdbcQueries();
    final Search search = new Search();
    private static final String GUI_FONT_NAME = "Tahoma";
    private static Font GUI_FONT = new Font(GUI_FONT_NAME, Font.PLAIN, 11);
    public static final Object[] MAIN_TABLE_HEADERS = {"Num", "Title", "Feel", "Wt", "Date", "Source", "Description",
            "Link"};
    private static final String[] TABLE_FOR_ANALYZE_HEADERS = {"top 10", "freq.", " "};
    public static final Color guiFontColor = new Color(255, 255, 153);
    public static final String[] INTERVALS = {"1 min", "5 min", "15 min", "30 min", "45 min", "1 hour", "2 hours",
            "4 hours", "8 hours", "12 hours", "24 hours", "48 hours", "72 hours", "all"};
    public static final String [] interfaceLanguages = new String[]{"en", "ru"};
    public static final String [] onOff = new String[]{"on", "off"};
    public static final JComboBox<Integer> WEIGHT_COMBOBOX =
            new JComboBox<>(new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
    public static final AtomicBoolean WAS_CLICK_IN_TABLE_FOR_ANALYSIS = new AtomicBoolean(false);
    public static int newsCount = 1;
    public static boolean isOnlyLastNews = false;
    public static String findWord;
    public static JScrollPane mainTableScrollPane;
    public static JTable mainTable, topTenTable;
    public static DefaultTableModel modelMain, modelTopTen;
    public static JTextField keyword;
    public static JTextArea consoleTextArea;
    public static JComboBox<String> searchInterval, resourceCombobox;
    public static JLabel amountOfNewsLabel, newsInArchiveLabel, loginLabel, searchAnimationLabel;
    public static JButton searchByKeyword, searchByKeywords, stopKeywordSearch, stopKeywordsSearch;
    public static Checkbox latestNewsCheckbox;
    public static JProgressBar progressBar;
    private SystemTray systemTray;
    private static int mainTableRowNum;
    private static int titleColumnNum;
    private static int sourceColumnNum;
    private static int feelColumnNum;
    private static int weightColumnNum;

    public Gui(HashMap<String, Integer> guiSettings, HashMap<String, Color> guiColors) {
        String tableFontName = jdbcQueries.getSetting("font_name");

        List<String> EXCLUDED_FROM_ITEMS = jdbcQueries.getExcludedItems();

        if (OsChecker.isUnix()) {
            GUI_FONT = new Font(GUI_FONT_NAME, Font.PLAIN, 10);

            if (tableFontName.equals("Arial")) {
                jdbcQueries.updateSettings("font_name", "C059");
                tableFontName = jdbcQueries.getSetting("font_name");
            }
        }

        this.setName("Avandy news");
        this.getContentPane().setBackground(guiColors.get("guiColor"));
        this.setIconImage(Icons.LOGO_ICON.getImage());
        this.setFont(GUI_FONT);
        this.setBounds(guiSettings.get("guiX"), guiSettings.get("guiY"), guiSettings.get("width"),
                guiSettings.get("height") + 22);
        this.getContentPane().setLayout(null);

        // Прозрачность и оформление окна
        this.setUndecorated(true);
        // Проверка поддерживает ли операционная система прозрачность окон
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        GraphicsDevice gd = ge.getDefaultScreenDevice();
        boolean isUniformTranslucencySupported = gd.isWindowTranslucencySupported(TRANSLUCENT);
        if (isUniformTranslucencySupported) {
            String transparencyValue = new JdbcQueries().getSetting("transparency");
            if (transparencyValue != null && transparencyValue.length() > 0) {
                int val = Integer.parseInt(transparencyValue);
                if (val > 100) transparencyValue = "100";
                else if (val < 0) transparencyValue = "40";
                this.setOpacity(Float.parseFloat(transparencyValue) / 100);
            } else {
                this.setOpacity(0.9f);
            }
        }

        //Table
        mainTableScrollPane = new JScrollPane();
        mainTableScrollPane.setBounds(10, 40, guiSettings.get("mainTableWidth"), guiSettings.get("mainTableHeight"));
        getContentPane().add(mainTableScrollPane);
        modelMain = new DefaultTableModel(new Object[][]{}, MAIN_TABLE_HEADERS) {
            final boolean[] columnEditable = new boolean[]{false, false, true, true, false, false, false, false};

            public boolean isCellEditable(int row, int column) {
                return columnEditable[column];
            }

            // Сортировка
            final Class[] types_unique = {Integer.class, String.class,
                    String.class, Integer.class, String.class, String.class, String.class, String.class};

            @Override
            public Class getColumnClass(int columnIndex) {
                return this.types_unique[columnIndex];
            }
        };
        mainTable = new JTable(modelMain) {
            // tooltips
            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = getColumnIndex("Title");
                try {
                    tip = (String) getValueAt(rowIndex, colIndex);
                } catch (RuntimeException ignored) {
                }
                if (tip != null && tip.length() > guiSettings.get("toolTipShowLength")) {
                    return tip;
                } else {
                    return null;
                }
            }

            // Альтернативный цвет для строки таблицы
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);

                Color mainTableColor = guiColors.get("tablesColor");
                Color alternateColor = guiColors.get("tablesAltColor");

                if (!component.getBackground().equals(getSelectionBackground())) {
                    Color color = (row % 2 == 0 ? mainTableColor : alternateColor);
                    component.setBackground(color);
                }
                return component;
            }
        };
        //headers
        JTableHeader headerMain = mainTable.getTableHeader();
        // запрет перемещения столбцов таблицы
        mainTable.getTableHeader().setReorderingAllowed(true);
        mainTable.setAutoCreateRowSorter(false);
        int mainHeaderFontSize = Integer.parseInt(jdbcQueries.getSetting("font_size"));
        int mainHeaderRowHeight = Integer.parseInt(jdbcQueries.getSetting("row_height"));
        headerMain.setFont(new Font(GUI_FONT_NAME, Font.BOLD, 13));
        headerMain.setForeground(Dialogs.FONT_COLOR);
        //Cell alignment
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(JLabel.CENTER);
        mainTable.getColumnModel().getColumn(getColumnIndex("Num")).setCellRenderer(renderer);
        mainTable.setRowHeight(mainHeaderRowHeight);
        mainTable.setColumnSelectionAllowed(true);
        mainTable.setCellSelectionEnabled(true);
        mainTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        mainTable.setFont(new Font(tableFontName, Font.PLAIN, mainHeaderFontSize));
        // Object[] MAIN_TABLE_HEADERS = {"Num", "Title", "Source", "Link", "Description", "Feel", "Wt", "Date"};
        mainTable.getColumnModel().getColumn(getColumnIndex("Num")).setMaxWidth(40);
        mainTable.getColumnModel().getColumn(getColumnIndex("Title")).setPreferredWidth(490);
        mainTable.getColumnModel().getColumn(getColumnIndex("Source")).setPreferredWidth(100);
        mainTable.getColumnModel().getColumn(getColumnIndex("Source")).setMaxWidth(180);
        mainTable.getColumnModel().getColumn(getColumnIndex("Date")).setPreferredWidth(68);
        mainTable.getColumnModel().getColumn(getColumnIndex("Date")).setMaxWidth(143);
        mainTable.removeColumn(mainTable.getColumnModel().getColumn(getColumnIndex("Description"))); // Скрыть описание
        mainTable.removeColumn(mainTable.getColumnModel().getColumn(getColumnIndex("Link"))); // Скрыть ссылку

        // отношение к новости: позитив/негатив или - не важно
        JComboBox<String> feeling = new JComboBox<>(new String[]{"", "+", "-"});
        feeling.addActionListener(e -> {
            titleColumnNum = getColumnIndex("Title");
            feelColumnNum = getColumnIndex("Feel");

            try {
                int row = mainTable.getSelectedRow();
                int col = mainTable.getSelectedColumn();
                if (col == feelColumnNum && row != -1) {
                    if (mainTable.getValueAt(row, col) != null) {
                        String title = mainTable.getValueAt(row, titleColumnNum).toString();
                        String feel = mainTable.getValueAt(row, feelColumnNum).toString();
                        jdbcQueries.updateFeeling(title, feel);
                    }
                }
            } catch (Exception exception) {
                Common.showAlert(exception.getMessage());
            }
        });
        int columnFeelIndex = getColumnIndex("Feel");
        mainTable.getColumnModel().getColumn(columnFeelIndex).setCellEditor(new DefaultCellEditor(feeling));
        mainTable.getColumnModel().getColumn(columnFeelIndex).setCellRenderer(renderer);
        mainTable.getColumnModel().getColumn(columnFeelIndex).setPreferredWidth(40);
        mainTable.getColumnModel().getColumn(columnFeelIndex).setMaxWidth(40);

        // Вес новости по значимости от 0 до 10
        WEIGHT_COMBOBOX.addActionListener(e -> {
            titleColumnNum = getColumnIndex("Title");
            weightColumnNum = getColumnIndex("Wt");
            try {
                int row = mainTable.getSelectedRow();
                int col = mainTable.getSelectedColumn();
                if (col == weightColumnNum && row != -1) {
                    if (mainTable.getValueAt(row, col) != null) {
                        String title = mainTable.getValueAt(row, titleColumnNum).toString();
                        int weight = Integer.parseInt(mainTable.getValueAt(row, weightColumnNum).toString());
                        jdbcQueries.updateWeight(title, weight);
                    }
                }
            } catch (Exception exc) {
                Common.showAlert(exc.getMessage());
            }
        });
        int columnWeightIndex = getColumnIndex("Wt");
        mainTable.getColumnModel().getColumn(columnWeightIndex).setCellEditor(new DefaultCellEditor(WEIGHT_COMBOBOX));
        mainTable.getColumnModel().getColumn(columnWeightIndex).setCellRenderer(renderer);
        mainTable.getColumnModel().getColumn(columnWeightIndex).setPreferredWidth(40);
        mainTable.getColumnModel().getColumn(columnWeightIndex).setMaxWidth(40);

        // Mouse LEFT click listener
        mainTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Инициализация координат ячейки
                mainTableRowNum = mainTable.convertRowIndexToModel(mainTable.rowAtPoint(e.getPoint()));
                //mainTableColumnNum = mainTable.convertColumnIndexToModel(mainTable.columnAtPoint(e.getPoint()));

                if (e.getClickCount() == 2) {
                    int row = mainTable.convertRowIndexToModel(mainTable.rowAtPoint(new Point(e.getX(), e.getY())));
                    int col = mainTable.convertColumnIndexToModel(mainTable.columnAtPoint(new Point(e.getX(), e.getY())));
                    //int col = tableMain.columnAtPoint(new Point(e.getX(), e.getY()));
                    if (col == 1) {
                        String url = (String) mainTable.getModel().getValueAt(row, 7);
                        openPage(url);
                    }
                }
            }
        });

        // Mouse RIGHT click listener
        mainTable.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    final JPopupMenu popup = new JPopupMenu();
                    // Инициализация координат ячейки
                    mainTableRowNum = mainTable.convertRowIndexToModel(mainTable.rowAtPoint(e.getPoint()));
                    //mainTableColumnNum = mainTable.convertColumnIndexToModel(mainTable.columnAtPoint(e.getPoint()));
                    titleColumnNum = getColumnIndex("Title");
                    sourceColumnNum = getColumnIndex("Source");

                    mainTable.setRowSelectionInterval(mainTableRowNum, mainTableRowNum);
                    int column = mainTable.columnAtPoint(e.getPoint());
                    if (mainTable.isRowSelected(mainTableRowNum)) {
                        mainTable.changeSelection(mainTableRowNum, column, false, false);
                    }

                    String title = (String) mainTable.getValueAt(mainTableRowNum, titleColumnNum);
                    String source = (String) mainTable.getValueAt(mainTableRowNum, sourceColumnNum);

                    // Show describe (menu)
                    JMenuItem menuDescribe = new JMenuItem(rightClickMenuDescribeText, Icons.SETTINGS_DESCRIBE_ICON);
                    menuDescribe.addActionListener(x -> Common.showInfo(jdbcQueries.getLinkOrDescribeByHash(source, title, "describe")));

                    // Add to favorites (menu)
                    JMenuItem menuFavorite = new JMenuItem(rightClickMenuToFavoritesText, Icons.WHEN_OK);
                    menuFavorite.addActionListener(x -> jdbcQueries.addFavoriteTitle(title, jdbcQueries.getLinkOrDescribeByHash(source, title, "link")));

                    // Copy (menu)
                    JMenuItem menuCopy = new JMenuItem(rightClickMenuCopyText, Icons.SETTINGS_COPY_ICON);
                    menuCopy.addActionListener(x -> {
                        StringBuilder sbf = new StringBuilder();
                        int cols = mainTable.getSelectedColumnCount();
                        int rows = mainTable.getSelectedRowCount();
                        int[] selectedRows = mainTable.getSelectedRows();
                        int[] selectedColumns = mainTable.getSelectedColumns();
                        for (int i = 0; i < rows; ++i) {
                            for (int j = 0; j < cols; ++j) {
                                sbf.append(mainTable.getValueAt(selectedRows[i], selectedColumns[j]));
                                if (j < cols - 1) {
                                    sbf.append("\t");
                                }
                            }
                            sbf.append("\n");
                        }
                        StringSelection stsel = new StringSelection(sbf.toString());
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(stsel, stsel);
                    });

                    // Add to archive (menu)
                    JMenuItem menuAddHeadline = new JMenuItem(rightClickMenuAddHeadlineText, Icons.CHECK_FOR_UPDATES_ICON);
                    menuAddHeadline.addActionListener(x -> {
                        try {
                            String titleToArch = JOptionPane.showInputDialog(mainTableScrollPane, "title");
                            // Check title
                            while (titleToArch.length() == 0) {
                                titleToArch = JOptionPane.showInputDialog(mainTableScrollPane, "input title");
                            }

                            String desc = JOptionPane.showInputDialog(mainTableScrollPane, "description");
                            String sourceToArch = JOptionPane.showInputDialog(mainTableScrollPane, "source");
                            String link = JOptionPane.showInputDialog(mainTableScrollPane, "link");
                            String pubDate = JOptionPane.showInputDialog(mainTableScrollPane, "pub date (YYYY-MM-DD HH24:MI:SS)");

                            // Check date format
                            Pattern pattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}$");
                            Matcher matcher = pattern.matcher(pubDate);
                            while (!matcher.find()) {
                                pubDate = JOptionPane.showInputDialog(mainTableScrollPane, "pub date (YYYY-MM-DD HH24:MI:SS)");
                                matcher = pattern.matcher(pubDate);
                            }

                            boolean isAdded = jdbcQueries.addTitleToArchiveManual(titleToArch, pubDate, link, sourceToArch, desc);
                            if (isAdded) {
                                Common.console("title added to archive");
                            }

                        } catch (Exception ignored) {
                        }
                    });

                    // Delete row (menu)
                    JMenuItem menuDeleteRow = new JMenuItem(rightClickMenuRemoveText, Icons.EXIT_BUTTON_ICON);
                    menuDeleteRow.addActionListener(x -> modelMain.removeRow(mainTableRowNum));

                    // Clear news list
                    JMenuItem menuRemoveAll = new JMenuItem(rightClickMenuClearText, Icons.EXIT_BUTTON_ICON);
                    menuRemoveAll.addActionListener(x -> {
                        try {
                            if (modelMain.getRowCount() == 0) {
                                Common.showAlert("No data to clear");
                                return;
                            }
                            modelMain.setRowCount(0);
                            modelTopTen.setRowCount(0);
                            newsCount = 0;
                            amountOfNewsLabel.setText(String.valueOf(newsCount));
                        } catch (Exception t) {
                            Common.showAlert(t.getMessage());
                            t.printStackTrace();
                        }
                    });

                    // Добавить слово в ключевые слова для поиска
                    Insets itemInsets = new Insets(3, -10, 3, 10);

                    JMenu menuAddToKeywords = new JMenu(rightClickMenuKeywordsText);
                    menuAddToKeywords.setIcon(Icons.MINUS_GREEN_ICON);

                    for (String word : title.split(" ")) {
                        String cleanWord = Common.keepOnlyLetters(word);
                        int wordLength = cleanWord.length();

                        if (wordLength > 3 && !EXCLUDED_FROM_ITEMS.contains(cleanWord.toLowerCase())) {
                            JMenu parts = new JMenu(cleanWord);
                            parts.setMargin(itemInsets);

                            parts.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                    addKeyword(cleanWord.toLowerCase());
                                }
                            });

                            for (int i = 0; i <= wordLength - 3; i++) {
                                String partOfWord = cleanWord.substring(0, wordLength - i).toLowerCase();
                                JMenuItem item = new JMenuItem(partOfWord);
                                item.setMargin(itemInsets);
                                item.addActionListener(y -> addKeyword(partOfWord));
                                parts.add(item);
                            }
                            menuAddToKeywords.add(parts);
                        } else if (wordLength == 3 && !EXCLUDED_FROM_ITEMS.contains(cleanWord.toLowerCase())) {
                            JMenuItem item = new JMenuItem(cleanWord);
                            item.setMargin(itemInsets);
                            item.addActionListener(y -> addKeyword(cleanWord.toLowerCase()));
                            menuAddToKeywords.add(item);
                        }
                    }
                    // панель добавления ключевого слова
                    JPanel addWordPanel = new JPanel();
                    JTextField addWordTextField = new JTextField(10);
                    JButton addWordButton = new JButton();
                    addWordButton.setToolTipText(rightClickMenuKeywordsToolpitText);
                    addWordButton.setIcon(Icons.ADD_ICON);
                    addWordButton.setBorderPainted(false);
                    addWordButton.setFocusable(false);
                    addWordButton.setContentAreaFilled(false);
                    addWordButton.addActionListener((x) -> addKeyword(addWordTextField.getText()));
                    animation(addWordButton, Icons.ADD_ICON, Icons.ADD_ICON2);

                    // включение русской раскладки клавиатуры для добавления слова
                    //getInputContext().selectInputMethod(new Locale("ru", "RU"));
                    addWordPanel.add(addWordTextField);
                    addWordPanel.add(addWordButton);
                    menuAddToKeywords.add(addWordPanel);

                    // Добавить слово в список на исключение заголовков
                    JMenu menuAddToExcludedHeadlines = new JMenu(rightClickMenuExcludedText);
                    menuAddToExcludedHeadlines.setIcon(Icons.MINUS_RED_ICON);
                    for (String word : title.split(" ")) {
                        String cleanWord = Common.keepOnlyLetters(word);
                        int wordLength = cleanWord.length();

                        if (wordLength > 3 && !EXCLUDED_FROM_ITEMS.contains(cleanWord.toLowerCase())) {
                            JMenu parts = new JMenu(cleanWord);
                            parts.setMargin(itemInsets);
                            parts.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                    jdbcQueries.addWordToExcludeTitles(cleanWord.toLowerCase());
                                }
                            });

                            for (int i = 0; i <= wordLength - 3; i++) {
                                String partOfWord = cleanWord.substring(0, wordLength - i).toLowerCase();
                                JMenuItem item = new JMenuItem(partOfWord);
                                item.setMargin(itemInsets);
                                item.addActionListener(y -> jdbcQueries.addWordToExcludeTitles(partOfWord));
                                parts.add(item);
                            }
                            menuAddToExcludedHeadlines.add(parts);
                        } else if (wordLength == 3 && !EXCLUDED_FROM_ITEMS.contains(cleanWord.toLowerCase())) {
                            JMenuItem item = new JMenuItem(cleanWord);
                            item.setMargin(itemInsets);
                            item.addActionListener(y -> jdbcQueries.addWordToExcludeTitles(cleanWord.toLowerCase()));
                            menuAddToExcludedHeadlines.add(item);
                        }
                    }
                    // панель добавления слова исключения
                    JPanel addExcludedWordPanel = new JPanel();
                    JTextField addExcludedWordTextField = new JTextField(10);
                    JButton addExcludedWordButton = new JButton();
                    addExcludedWordButton.setToolTipText(rightClickMenuExcludedTooltipText);
                    addExcludedWordButton.setIcon(Icons.ADD_ICON);
                    addExcludedWordButton.setBorderPainted(false);
                    addExcludedWordButton.setFocusable(false);
                    addExcludedWordButton.setContentAreaFilled(false);
                    addExcludedWordButton.addActionListener((x) -> jdbcQueries.addWordToExcludeTitles(
                            addExcludedWordTextField.getText().toLowerCase()));
                    animation(addExcludedWordButton, Icons.ADD_ICON, Icons.ADD_ICON2);
                    // включение русской раскладки клавиатуры для добавления слова
                    //getInputContext().selectInputMethod(new Locale("ru", "RU"));
                    addExcludedWordPanel.add(addExcludedWordTextField);
                    addExcludedWordPanel.add(addExcludedWordButton);
                    menuAddToExcludedHeadlines.add(addExcludedWordPanel);

                    popup.add(menuDescribe);
                    popup.add(menuFavorite);
                    popup.add(menuAddHeadline);
                    popup.add(menuCopy);
                    popup.add(menuDeleteRow);
                    popup.add(menuRemoveAll);
                    popup.addSeparator();
                    popup.add(new JLabel("   " + rightClickMenuAddWordToText));
                    popup.add(menuAddToKeywords);
                    popup.add(menuAddToExcludedHeadlines);

                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        mainTableScrollPane.setViewportView(mainTable);

        /* TOP-LEFT ACTION PANEL */
        int topLeftX = guiSettings.get("topLeftX");
        int topLeftY = guiSettings.get("topLeftY");
        int searchAnimationLabelWidth = guiSettings.get("searchAnimationLabelWidth");

        //Input keyword
        JLabel findInLabel = new JLabel(findInLabelText);
        if (OsChecker.isWindows() && isRussian()) {
            findInLabel.setBounds(topLeftX, topLeftY, 57, 19);
            topLeftX += 13;
        } else findInLabel.setBounds(topLeftX, topLeftY, 50, 19);

        findInLabel.setForeground(new Color(255, 179, 131));
        findInLabel.setFont(new Font(GUI_FONT_NAME, Font.BOLD, 14));
        if (OsChecker.isUnix()) {
            findInLabel.setFont(new Font(GUI_FONT_NAME, Font.BOLD, 13));
            findInLabel.setBounds(topLeftX, topLeftY + 1, 50, 19);

            if (isRussian()) {
                findInLabel.setBounds(topLeftX, topLeftY + 1, 59, 19);
                topLeftX += 19;
            }
        }
        findInLabel.setHorizontalAlignment(SwingConstants.LEFT);
        getContentPane().add(findInLabel);

        if (isRussian()) {
            resourceCombobox = new JComboBox<>(new String[]{"сети", "архиве"});
            resourceCombobox.setBounds(topLeftX + 50, topLeftY, 70, 22);
            topLeftX += 23;
        } else {
            resourceCombobox = new JComboBox<>(new String[]{"rss", "arc"});
            resourceCombobox.setBounds(topLeftX + 50, topLeftY, 47, 22);
        }
        resourceCombobox.setFont(GUI_FONT);
        getContentPane().add(resourceCombobox);

        //Keyword field
        keyword = new JTextField(findWord);
        keyword.setBounds(topLeftX + 104, topLeftY, 100, 22);
        keyword.setFont(new Font(GUI_FONT_NAME, Font.BOLD, 13));
        getContentPane().add(keyword);

        // Clear keyword field
        JButton clearKeyword = new JButton();
        clearKeyword.setBorderPainted(false);
        clearKeyword.setFocusable(false);
        clearKeyword.setContentAreaFilled(false);
        clearKeyword.setIcon(Icons.EXIT_BUTTON_ICON);
        clearKeyword.setBackground(new Color(154, 237, 196));
        clearKeyword.setFont(new Font(GUI_FONT_NAME, Font.BOLD, 10));
        clearKeyword.setBounds(topLeftX + 199, topLeftY, 30, 22);
        getContentPane().add(clearKeyword);
        clearKeyword.addActionListener(e -> keyword.setText(""));
        animation(clearKeyword, Icons.EXIT_BUTTON_ICON, Icons.WHEN_MOUSE_ON_EXIT_BUTTON_ICON);

        //Search
        searchByKeyword = new JButton();
        searchByKeyword.setIcon(Icons.SEARCH_KEYWORDS_ICON);
        searchByKeyword.setBackground(new Color(154, 237, 196));
        searchByKeyword.setFont(new Font(GUI_FONT_NAME, Font.BOLD, 10));
        searchByKeyword.setBounds(topLeftX + 229, topLeftY, 30, 22);
        getContentPane().add(searchByKeyword);
        // Search by Enter
        getRootPane().setDefaultButton(searchByKeyword);
        searchByKeyword.requestFocus();
        searchByKeyword.doClick();
        searchByKeyword.addActionListener(e -> {
            String findWay = String.valueOf(resourceCombobox.getSelectedItem());
            if (findWay.equals("rss") || findWay.equals("сети")) {
                new Thread(() -> search.mainSearch("word")).start();
            } else if (findWay.equals("arc") || findWay.equals("архиве")) {
                new Thread(() -> search.searchInArchive("word")).start();
            }
        });

        //Stop addNewSource
        stopKeywordSearch = new JButton();
        stopKeywordSearch.setIcon(Icons.STOP_SEARCH_ICON);
        stopKeywordSearch.setBackground(new Color(255, 208, 202));
        stopKeywordSearch.setBounds(topLeftX + 229, topLeftY, 30, 22);
        stopKeywordSearch.addActionListener(e -> {
            try {
                Search.isSearchFinished.set(true);
                Search.isStop.set(true);
                Common.console("search stopped");
                searchByKeyword.setVisible(true);
                stopKeywordSearch.setVisible(false);
                Search.isSearchNow.set(false);
                try {
                    sqLite.transaction("ROLLBACK");
                } catch (SQLException ignored) {
                }
            } catch (Exception t) {
                Common.showAlert("No threads to stop");
            }
        });
        getContentPane().add(stopKeywordSearch);

        // Интервалы для поиска новостей
        searchInterval = new JComboBox<>(INTERVALS);
        searchInterval.setFont(GUI_FONT);
        searchInterval.setBounds(topLeftX + 267, topLeftY, 78, 22); //516
        getContentPane().add(searchInterval);

        // latest news
        latestNewsCheckbox = new Checkbox(latestNewsCheckboxText);
        latestNewsCheckbox.setFocusable(false);
        if (OsChecker.isUnix()) {
            latestNewsCheckbox.setBounds(topLeftX + 352, topLeftY + 1, 82, 20);
        } else {
            latestNewsCheckbox.setBounds(topLeftX + 352, topLeftY + 1, 80, 20);
        }
        latestNewsCheckbox.setFont(GUI_FONT);
        getContentPane().add(latestNewsCheckbox);
        latestNewsCheckbox.addItemListener(e -> {
            isOnlyLastNews = latestNewsCheckbox.getState();
            if (!isOnlyLastNews) {
                jdbcQueries.removeFromTitles();
            }
        });

        searchAnimationLabel = new JLabel();
        searchAnimationLabel.setEnabled(false);
        if (isRussian()) {
            searchAnimationLabel.setBounds(topLeftX + 437, topLeftY + 1,
                    searchAnimationLabelWidth - 76, 20);
        } else
            searchAnimationLabel.setBounds(topLeftX + 434, topLeftY + 1, searchAnimationLabelWidth, 20);
        searchAnimationLabel.setFont(GUI_FONT);
        searchAnimationLabel.setForeground(guiFontColor);
        getContentPane().add(searchAnimationLabel);
        animation(searchAnimationLabel);

        /* TOP-RIGHT */
        /* Сворачивание в трей */
        JButton toTrayBtn = new JButton(Icons.HIDE_BUTTON_ICON);
        toTrayBtn.setFocusable(false);
        toTrayBtn.setBorderPainted(false);
        if (SystemTray.isSupported()) {
            getContentPane().add(toTrayBtn);
        }
        toTrayBtn.addActionListener(e -> setVisible(false));
        animation(toTrayBtn, Icons.HIDE_BUTTON_ICON, Icons.WHEN_MOUSE_ON_HIDE_BUTTON_ICON);
        // Сворачивание приложения в трей
        appInTray();

        // Exit button
        JButton exitBtn = new JButton(Icons.EXIT_BUTTON_ICON);
        exitBtn.setFocusable(false);
        exitBtn.setBorderPainted(false);
        exitBtn.addActionListener((e) -> {
            Search.isSearchFinished.set(true);
            Common.saveState();
            saveCurrentGuiXY();
            sqLite.closeConnection();
            System.exit(0);
        });
        animation(exitBtn, Icons.EXIT_BUTTON_ICON, Icons.WHEN_MOUSE_ON_EXIT_BUTTON_ICON);

        /* KEYWORDS SEARCH */
        int bottomLeftX = guiSettings.get("bottomLeftX");
        int bottomLeftY = guiSettings.get("bottomLeftY");

        // label
        JLabel lblKeywordsSearch = new JLabel();
        if (isRussian()) {
            if (OsChecker.isUnix()) {
                lblKeywordsSearch.setBounds(bottomLeftX - 66, bottomLeftY + 4, 150, 14);
            } else {
                lblKeywordsSearch.setBounds(bottomLeftX - 60, bottomLeftY + 4, 150, 14);
            }
        } else lblKeywordsSearch.setBounds(bottomLeftX - 18, bottomLeftY + 4, 120, 14);

        lblKeywordsSearch.setText(lblKeywordsSearchText);
        lblKeywordsSearch.setForeground(guiFontColor);
        lblKeywordsSearch.setFont(GUI_FONT);
        getContentPane().add(lblKeywordsSearch);

        // Список ключевых слов для поиска
        JLabel showKeywordsList = new JLabel(Icons.LIST_BUTTON_ICON);
        showKeywordsList.setBounds(bottomLeftX + 79, bottomLeftY, 30, 22);
        showKeywordsList.setForeground(new Color(154, 237, 196));
        showKeywordsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new Dialogs("dialog_keywords");
            }
        });
        animation(showKeywordsList, Icons.LIST_BUTTON_ICON, Icons.WHEN_MOUSE_ON_LIST_BUTTON_ICON);
        getContentPane().add(showKeywordsList);

        //Bottom search by keywords
        searchByKeywords = new JButton(Icons.SEARCH_KEYWORDS_ICON);
        searchByKeywords.setBounds(bottomLeftX + 112, bottomLeftY, 30, 22);
        searchByKeywords.setBackground(new Color(154, 237, 196));
        getContentPane().add(searchByKeywords);
        searchByKeywords.addActionListener(e -> {
            String findWay = String.valueOf(resourceCombobox.getSelectedItem());
            if (findWay.equals("rss") || findWay.equals("сети")) {
                new Thread(() -> search.mainSearch("words")).start();
            } else if (findWay.equals("arc") || findWay.equals("архиве")) {
                new Thread(() -> search.searchInArchive("words")).start();
            }
        });

        //Stop (bottom)
        stopKeywordsSearch = new JButton(Icons.STOP_SEARCH_ICON);
        stopKeywordsSearch.setBackground(new Color(255, 208, 202));
        stopKeywordsSearch.setBounds(bottomLeftX + 112, bottomLeftY, 30, 22);
        stopKeywordsSearch.addActionListener(e -> {
            try {
                Search.isSearchFinished.set(true);
                Search.isStop.set(true);
                Common.console("search stopped");
                searchByKeywords.setVisible(true);
                stopKeywordsSearch.setVisible(false);
                Search.isSearchNow.set(false);
                try {
                    sqLite.transaction("ROLLBACK");
                } catch (SQLException ignored) {
                }
            } catch (Exception t) {
                Common.showAlert("No threads to stop");
            }
        });
        getContentPane().add(stopKeywordsSearch);

        /* CONSOLE */
        //Console - textarea
        consoleTextArea = new JTextArea();
        consoleTextArea.setForeground(guiColors.get("fontColor"));
        // авто скроллинг
        DefaultCaret caret = (DefaultCaret) consoleTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
        consoleTextArea.setAutoscrolls(true);
        consoleTextArea.setLineWrap(true);
        consoleTextArea.setWrapStyleWord(true);
        consoleTextArea.setEditable(false);
        consoleTextArea.setBounds(20, 11, 145, 51);
        consoleTextArea.setFont(new Font(GUI_FONT_NAME, Font.BOLD, 13));
        consoleTextArea.setBackground(new Color(222, 222, 222)); // 83, 82, 82
        getContentPane().add(consoleTextArea);
        consoleTextArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    final JPopupMenu menu = new JPopupMenu();
                    JMenuItem clear = new JMenuItem("Clear", Icons.DELETE_UNIT);
                    clear.addActionListener(x -> consoleTextArea.setText(""));
                    menu.add(clear);
                    menu.show(consoleTextArea, e.getX(), e.getY());
                }
            }
        });

        //Console - scroll
        JScrollPane consoleTextAreaScroll = new JScrollPane(consoleTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        consoleTextAreaScroll.setBounds(guiSettings.get("consoleX"), guiSettings.get("consoleY"),
                guiSettings.get("consoleWidth"), guiSettings.get("consoleHeight"));
        consoleTextAreaScroll.setBorder(null);
        getContentPane().add(consoleTextAreaScroll);

        // Шкала прогресса
        progressBar = new JProgressBar();
        progressBar.setFocusable(false);
        progressBar.setMaximum(100);
        progressBar.setBorderPainted(false);
        progressBar.setBounds(10, 37, guiSettings.get("mainTableWidth"), 1);
        getContentPane().add(progressBar);

        /* RIGHT AREA */
        //Table for analysis
        JScrollPane topTenScroll = new JScrollPane();
        topTenScroll.setBounds(guiSettings.get("topTenX"), guiSettings.get("topTenY"), 290,
                guiSettings.get("topTenHeight"));
        getContentPane().add(topTenScroll);

        modelTopTen = new DefaultTableModel(new Object[][]{}, TABLE_FOR_ANALYZE_HEADERS) {
            final boolean[] columnTopTen = new boolean[]{false, false, true};

            public boolean isCellEditable(int row, int column) {
                return columnTopTen[column];
            }

            final Class[] types_unique = {String.class, Integer.class, Button.class};

            @Override
            public Class getColumnClass(int columnIndex) {
                return this.types_unique[columnIndex];
            }
        };
        topTenTable = new JTable(modelTopTen) {
            // Альтернативный цвет для строки таблицы
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);

                Color mainTableColor = guiColors.get("tablesColor");
                Color alternateColor = guiColors.get("tablesAltColor");

                if (!component.getBackground().equals(getSelectionBackground())) {
                    Color color = (row % 2 == 0 ? mainTableColor : alternateColor);
                    component.setBackground(color);
                }
                return component;
            }
        };
        JTableHeader topTenHeaders = topTenTable.getTableHeader();
        // запрет перемещения столбцов таблицы
        topTenTable.getTableHeader().setReorderingAllowed(false);
        topTenHeaders.setFont(new Font(GUI_FONT_NAME, Font.BOLD, 13));
        //Cell alignment
        DefaultTableCellRenderer topTenRenderer = new DefaultTableCellRenderer();
        topTenRenderer.setHorizontalAlignment(JLabel.CENTER);
        topTenTable.getColumnModel().getColumn(1).setCellRenderer(topTenRenderer);
        topTenTable.getColumn(" ").setCellRenderer(new ButtonColumn(topTenTable, 2));
        topTenTable.setRowHeight(21);
        topTenTable.setColumnSelectionAllowed(true);
        topTenTable.setCellSelectionEnabled(true);
        topTenTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        topTenTable.setFont(new Font(GUI_FONT_NAME, Font.PLAIN, 14));
        topTenTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        topTenTable.getColumnModel().getColumn(1).setPreferredWidth(40);
        topTenTable.getColumnModel().getColumn(1).setMaxWidth(40);
        topTenTable.getColumnModel().getColumn(2).setMaxWidth(30);
        //tableForAnalysis.setAutoCreateRowSorter(true);
        topTenScroll.setViewportView(topTenTable);

        // запуск поиска по слову из таблицы топ 10
        topTenTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = topTenTable.convertRowIndexToModel(topTenTable.rowAtPoint(new Point(e.getX(), e.getY())));
                    int col = topTenTable.columnAtPoint(new Point(e.getX(), e.getY()));
                    if (col == 0) {
                        if (modelMain.getRowCount() > 0) modelMain.setRowCount(0);
                        String valueAt = (String) topTenTable.getModel().getValueAt(row, 0);
                        Gui.keyword.setText(valueAt);
                        // выбор все новостей из архива по слову из топ 10
                        new Thread(() -> jdbcQueries.getNewsFromArchive(valueAt.toLowerCase())).start();
                        WAS_CLICK_IN_TABLE_FOR_ANALYSIS.set(true);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    int row = topTenTable.convertRowIndexToModel(topTenTable.rowAtPoint(e.getPoint()));
                    topTenTable.setRowSelectionInterval(row, row);
                    int column = topTenTable.columnAtPoint(e.getPoint());
                    if (topTenTable.isRowSelected(row)) {
                        topTenTable.changeSelection(row, column, false, false);
                    }

                    final JPopupMenu popupTopTen = new JPopupMenu();

                    // Диалоговое окно со списком исключенных слов из анализа
                    JMenuItem excludedFromTopTen = new JMenuItem("List", Icons.LIST_BUTTON_ICON);
                    excludedFromTopTen.addActionListener(x -> new Dialogs("dialog_excluded_analysis"));
                    popupTopTen.add(excludedFromTopTen);

                    // Export top ten to excel file (menu)
                    JMenuItem exportToXls = new JMenuItem("Export", Icons.EXPORT_XLS_ICON);
                    exportToXls.addActionListener((x) -> {
                        if (modelTopTen.getRowCount() != 0) {
                            new Thread(new ExportToExcel()::exportTopTenTableToExcel).start();
                        } else {
                            Common.showAlert("No data to export");
                        }
                    });
                    popupTopTen.add(exportToXls);

                    // Delete row (menu)
                    JMenuItem menuDeleteRow = new JMenuItem("Remove", Icons.EXIT_BUTTON_ICON);
                    menuDeleteRow.addActionListener(x -> {
                        int rowTopTen = topTenTable.convertRowIndexToModel(topTenTable.getSelectedRow());
                        if (rowTopTen != -1) modelTopTen.removeRow(rowTopTen);
                    });
                    popupTopTen.add(menuDeleteRow);

                    // Clear news list
                    JMenuItem menuRemoveAll = new JMenuItem("Clear all", Icons.EXIT_BUTTON_ICON);
                    menuRemoveAll.addActionListener(x -> {
                        try {
                            if (modelTopTen.getRowCount() == 0) {
                                Common.showAlert("No data to clear");
                                return;
                            }
                            modelTopTen.setRowCount(0);
                        } catch (Exception t) {
                            Common.showAlert(t.getMessage());
                        }
                    });
                    popupTopTen.add(menuRemoveAll);

                    popupTopTen.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        //Amount of news
        amountOfNewsLabel = new JLabel(amountOfNewsLabelInitText);
        amountOfNewsLabel.setBounds(guiSettings.get("topTenX"), guiSettings.get("topTenY") + 242,
                280, 13);
        amountOfNewsLabel.setFont(GUI_FONT);
        amountOfNewsLabel.setForeground(guiFontColor);
        getContentPane().add(amountOfNewsLabel);

        newsInArchiveLabel = new JLabel();
        newsInArchiveLabel.setEnabled(false);
        newsInArchiveLabel.setText(newsInArchiveLabelText + jdbcQueries.archiveNewsCount());
        newsInArchiveLabel.setForeground(guiFontColor);
        newsInArchiveLabel.setFont(GUI_FONT);
        newsInArchiveLabel.setBounds(guiSettings.get("menuArchiveX"), guiSettings.get("menuArchiveY"),
                140, 14);
        getContentPane().add(newsInArchiveLabel);
        animation(newsInArchiveLabel);

        // Username label
        if (Login.username != null) {
            loginLabel = new JLabel(loginLabelText + Login.username);
            Common.console(loginLabelHelloText + Login.username + "!");
        } else {
            loginLabel = new JLabel();
        }
        loginLabel.setEnabled(false);
        loginLabel.setToolTipText(loginLabelPwdText);
        loginLabel.setFont(GUI_FONT);
        loginLabel.setForeground(guiFontColor);
        loginLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    Common.saveState();
                    // удалить старый frame
                    dispose();
                    // удаление иконки в трее
                    TrayIcon[] trayIcons = systemTray.getTrayIcons();
                    for (TrayIcon t : trayIcons) {
                        systemTray.remove(t);
                    }

                    // тема для окна логирования
                    FlatCobalt2IJTheme.setup();
                    UIManager.put("Button.arc", 8);

                    if (Login.username.equals("demo")) {
                        new Login().createUser();
                    } else {
                        new Login().login();
                    }

                    Common.showGui();
                    loginLabel.setText(loginLabelText + Login.username);
                    Gui.consoleTextArea.setText("");
                    Common.console(loginLabelHelloText + Login.username + "!");
                    new Reminder().remind();

                    // user password change dialog
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    JPanel panel = new JPanel();
                    panel.setLayout(new GridLayout(3, 2, 5, 5));
                    JLabel oldPwd = new JLabel("Old password");
                    JPasswordField oldPwdField = new JPasswordField(3);
                    JLabel newPwd = new JLabel("New password");
                    JPasswordField newPwdField = new JPasswordField(3);
                    JLabel repeatPwd = new JLabel("Repeat password");
                    JPasswordField repeatPwdField = new JPasswordField(3);
                    panel.add(oldPwd);
                    panel.add(oldPwdField);
                    panel.add(newPwd);
                    panel.add(newPwdField);
                    panel.add(repeatPwd);
                    panel.add(repeatPwdField);

                    String[] menu = new String[]{"Cancel", "Ok"};
                    int option = JOptionPane.showOptionDialog(mainTableScrollPane, panel, "Change password", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, Icons.LOGO_ICON, menu, menu[1]);

                    if (option == 1) {
                        String oldPwdString = Common.getHash(new String(oldPwdField.getPassword()));
                        String newPwdString = Common.getHash(new String(newPwdField.getPassword()));
                        String repeatPwdString = Common.getHash(new String(repeatPwdField.getPassword()));

                        if (!Objects.equals(oldPwdString, jdbcQueries.getUserHashPassword(Login.username))) {
                            Common.showAlert("Old password is incorrect!");
                            return;
                        }

                        if (Objects.equals(oldPwdString, newPwdString)) {
                            Common.showAlert("The new password is the same as the old one");
                            return;
                        }

                        if (!Objects.equals(newPwdString, repeatPwdString)) {
                            Common.showAlert("Passwords doesn't match");
                            return;
                        }

                        jdbcQueries.updateUserPassword(Login.userId, newPwdString);
                        Common.console("user password changed");
                    }

                }
            }
        });
        animation(loginLabel);

        // Создание главного меню
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createViewMenu());
        menuBar.add(createShowUrl());
        menuBar.add(createHelp());

        // Форматирование меню
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(loginLabel);
        menuBar.add(new JLabel("  "));
        menuBar.add(toTrayBtn);
        menuBar.add(new JLabel(" "));
        menuBar.add(exitBtn);
        menuBar.add(new JLabel("  "));

        // Отображение меню
        setJMenuBar(menuBar);
        // Отображение приложения
        this.setVisible(true);

        if (Login.username.equals("demo")) {
            new Thread(this::createUserAnimation).start();
        }
    }

    // сохранение текущей позиции приложения при закрытии
    private void saveCurrentGuiXY() {
        jdbcQueries.updateSettings("gui_x", String.valueOf(getX()));
        jdbcQueries.updateSettings("gui_y", String.valueOf(getY()));
    }

    private static boolean isRussian() {
        return Common.getUiLang().equals("ru");
    }

    private static void addKeyword(String keyword) {
        String[] menu = new String[]{"No", "Yes"};
        int option = JOptionPane.showOptionDialog(mainTableScrollPane,
                "Do you really want to add the keyword?", "Keywords",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                Icons.EDIT_ICON, menu, menu[1]);

        if (option == 1) {
            new JdbcQueries().addKeyword(keyword.toLowerCase());
        }
    }

    private int getColumnIndex(String name) {
        return mainTable.getColumnModel().getColumnIndex(name);
    }

    private void appInTray() {
        try {
            BufferedImage Icon = ImageIO.read(Objects.requireNonNull(Icons.APP_IN_TRAY_BUTTON_ICON));
            final TrayIcon trayIcon = new TrayIcon(Icon, "Avandy News");
            systemTray = SystemTray.getSystemTray();
            systemTray.add(trayIcon);

            final PopupMenu trayMenu = new PopupMenu();
            MenuItem itemShow = new MenuItem("Show");
            itemShow.addActionListener(e -> {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
            });
            trayMenu.add(itemShow);

            MenuItem itemClose = new MenuItem("Close");
            itemClose.addActionListener(e -> System.exit(0));
            trayMenu.add(itemClose);

            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        setVisible(true);
                        setExtendedState(JFrame.NORMAL);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        trayIcon.setPopupMenu(trayMenu);
                    }
                }
            });
        } catch (IOException | AWTException e) {
            e.printStackTrace();
        }
    }

    private static void showSQLiteIde() {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(new File(Common.DIRECTORY_PATH + "sqlite3.exe"));
            } catch (IOException io) {
                io.printStackTrace();
            }
        }

        // копируем адрес базы в JdbcQueries в системный буфер для быстрого доступа
        String pathToBase = (".open " + Common.DATABASE_PATH).replace("\\", "/");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(pathToBase), null);
    }

    private void getSettingsDialog() {
        String transparencyValue = jdbcQueries.getSetting("transparency");
        String fontNameValue = jdbcQueries.getSetting("font_name");
        Integer fontSizeValue = Integer.valueOf(jdbcQueries.getSetting("font_size"));
        Integer rowHeightValue = Integer.valueOf(jdbcQueries.getSetting("row_height"));
        String isAssistValue = jdbcQueries.getSetting("is_assist");
        String isAutoFeel = jdbcQueries.getSetting("is_auto_feel");
        String langValue = jdbcQueries.getSetting("lang");
        int jaroWinklerValue = Integer.parseInt(jdbcQueries.getSetting("jaro-winkler-level"));

        // X-Y
        int xGui = Integer.parseInt(jdbcQueries.getSetting("gui_x"));
        int yGui = Integer.parseInt(jdbcQueries.getSetting("gui_y"));
        JTextField xTextField = new JTextField();
        xTextField.setText(String.valueOf(xGui));
        JTextField yTextField = new JTextField();
        yTextField.setText(String.valueOf(yGui));

        JSlider transparencySlider = new JSlider(new DefaultBoundedRangeModel(
                Integer.parseInt(transparencyValue), 0, 40, 100));
        JLabel transparencyLabel = new JLabel(transparencyText + transparencySlider.getValue() + " %");
        // присоединяем слушателя
        transparencySlider.addChangeListener(e -> {
            int value = ((JSlider) e.getSource()).getValue();
            transparencyLabel.setText("Transparency " + value + " %");
        });

        JTextField pathToDatabase = new JTextField();
        pathToDatabase.setBackground(new Color(255, 255, 255));
        pathToDatabase.setText(Common.DATABASE_PATH);
        // правой кнопкой открыть директорию с базой данных
        pathToDatabase.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        try {
                            Desktop.getDesktop().open(new File(Common.DATABASE_PATH.replace("news.db", "")));
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        }
                    }
                }
            }
        });

        JComboBox<Integer> fontSizeCombobox = new JComboBox<>(new Integer[]{13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23});
        fontSizeCombobox.setSelectedItem(fontSizeValue);

        JComboBox<String> fontNameCombobox = new JComboBox<>(Common.getFonts());
        fontNameCombobox.setSelectedItem(fontNameValue);

        JComboBox<Integer> jaroWinklerCombobox = new JComboBox<>(new Integer[]{70, 75, 80, 85, 90, 95, 100});
        jaroWinklerCombobox.setSelectedItem(jaroWinklerValue);

        Integer[] rowSizes = null;
        String sizeGui = jdbcQueries.getSetting("gui_theme");
        if (sizeGui.equals(GuiSize.SMALL.getSize())) {
            rowSizes = new Integer[]{19, 28, 34};
        } else if (sizeGui.equals(GuiSize.MIDDLE.getSize())) {
            rowSizes = new Integer[]{19, 30, 38};
        } else if (sizeGui.equals(GuiSize.LARGE.getSize())) {
            rowSizes = new Integer[]{29, 36, 48};
        }
        JComboBox<Integer> rowHeightCombobox = new JComboBox<>(rowSizes);
        rowHeightCombobox.setSelectedItem(rowHeightValue);

        JButton guiColorButton = new JButton(selectText);
        // Выбор цвета фона UI
        guiColorButton.addActionListener(x -> {
            Color backgroundColorValue = JColorChooser.showDialog(null, "Gui background", Color.black);
            if (backgroundColorValue != null) {
                this.setBackground(backgroundColorValue);
                Common.saveColor("gui_color", backgroundColorValue);
            }
        });

        JComboBox<String> onOffAssistant = new JComboBox<>(onOff);
        onOffAssistant.setSelectedItem(isAssistValue);

        JComboBox<String> onOffAutoFeel = new JComboBox<>(onOff);
        onOffAutoFeel.setSelectedItem(isAutoFeel);

        JComboBox<String> langCombobox = new JComboBox<>(interfaceLanguages);
        langCombobox.setSelectedItem(langValue);

        // Выбор цвета фона таблиц
        JButton tablesColorButton = new JButton(selectText);
        tablesColorButton.addActionListener(et -> {
            Color tablesColor = JColorChooser.showDialog(null, "Tables background", Color.black);
            if (tablesColor != null) {
                mainTable.setBackground(tablesColor);
                topTenTable.setBackground(tablesColor);
                Common.saveColor("tables_color", tablesColor);
            }
        });

        // Выбор цвета фона чётной строки таблицы
        JButton tablesAlternateColorButton = new JButton(selectText);
        tablesAlternateColorButton.addActionListener(et -> {
            Color tablesColor = JColorChooser.showDialog(null, "Tables alternate background", Color.black);
            if (tablesColor != null) {
                mainTable.setBackground(tablesColor);
                topTenTable.setBackground(tablesColor);
                Common.saveColor("tables_alt_color", tablesColor);
            }
        });

        // Выбор цвета шрифта в таблице
        JButton fontColorButton = new JButton(selectText);
        fontColorButton.addActionListener(ef -> {
            Color fontColor = JColorChooser.showDialog(null, "Font color", Color.black);
            if (fontColor != null) {
                mainTable.setForeground(fontColor);
                topTenTable.setForeground(fontColor);
                Common.saveColor("font_color", fontColor);
            }
        });

        // Перезагрузка базы данных
        JButton reloadDatabaseButton = new JButton(reloadText);
        reloadDatabaseButton.addActionListener(ef -> {
            jdbcQueries.vacuum();
            Common.showInfo(databaseReloadText + Common.getDatabaseSize());
        });

        // Сравнение двух строк методом Джаро-Винклера
        JButton jwButton = new JButton(jaroWinklerText3);
        jwButton.addActionListener(ef -> Common.compareTwoStrings());

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new GridLayout(16, 1, 0, 5));
        settingsPanel.add(new JLabel(languageText));
        settingsPanel.add(langCombobox);
        settingsPanel.add(new JLabel(interfaceXText));
        settingsPanel.add(xTextField);
        settingsPanel.add(new JLabel(interfaceYText));
        settingsPanel.add(yTextField);
        settingsPanel.add(new JLabel(assistantText));
        settingsPanel.add(onOffAssistant);
        settingsPanel.add(new JLabel(autoFeelText));
        settingsPanel.add(onOffAutoFeel);
        settingsPanel.add(new JLabel(tableFontNameText));
        settingsPanel.add(fontNameCombobox);
        settingsPanel.add(new JLabel(fontSizeText));
        settingsPanel.add(fontSizeCombobox);
        settingsPanel.add(new JLabel(rowHeightText));
        settingsPanel.add(rowHeightCombobox);
        settingsPanel.add(new JLabel(jaroWinklerText));
        settingsPanel.add(jaroWinklerCombobox);
        settingsPanel.add(new JLabel(jaroWinklerText2));
        settingsPanel.add(jwButton);
        settingsPanel.add(transparencyLabel);
        settingsPanel.add(transparencySlider);
        settingsPanel.add(new JLabel(interfaceColorText));
        settingsPanel.add(guiColorButton);
        settingsPanel.add(new JLabel(tablesColorText));
        settingsPanel.add(tablesColorButton);
        settingsPanel.add(new JLabel(tablesAlternateText));
        settingsPanel.add(tablesAlternateColorButton);
        settingsPanel.add(new JLabel(fontColorText));
        settingsPanel.add(fontColorButton);
        settingsPanel.add(new JLabel(reloadDbText + Common.getDatabaseSize()));
        settingsPanel.add(reloadDatabaseButton);
        Object[] newSource = {settingsPanel, pathToDatabaseText, pathToDatabase};

        int result = JOptionPane.showConfirmDialog(mainTableScrollPane, newSource, settingsDialogText,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            jdbcQueries.updateSettings("transparency", String.valueOf(transparencySlider.getValue()));

            if (sqLite.isValidPathToDatabase(pathToDatabase.getText())) {
                jdbcQueries.updateSettings("db_path", pathToDatabase.getText());
            } else {
                Common.showAlert("Incorrect path to database!");
                return;
            }

            jdbcQueries.updateSettings("font_name", Objects.requireNonNull(fontNameCombobox.getSelectedItem()).toString());
            jdbcQueries.updateSettings("font_size", Objects.requireNonNull(fontSizeCombobox.getSelectedItem()).toString());
            jdbcQueries.updateSettings("row_height", Objects.requireNonNull(rowHeightCombobox.getSelectedItem()).toString());
            jdbcQueries.updateSettings("is_assist", Objects.requireNonNull(onOffAssistant.getSelectedItem()).toString());
            jdbcQueries.updateSettings("is_auto_feel", Objects.requireNonNull(onOffAutoFeel.getSelectedItem()).toString());
            jdbcQueries.updateSettings("jaro-winkler-level", Objects.requireNonNull(jaroWinklerCombobox.getSelectedItem()).toString());
            jdbcQueries.updateSettings("lang", Objects.requireNonNull(langCombobox.getSelectedItem()).toString());
            jdbcQueries.updateSettings("gui_x", xTextField.getText());
            jdbcQueries.updateSettings("gui_y", yTextField.getText());

            saveCurrentGuiXY();
            refreshGui();
        }
    }

    public void refreshGui() {
        Common.saveState();
        // удалить старый frame
        this.dispose();
        // удаление иконки в трее
        TrayIcon[] trayIcons = systemTray.getTrayIcons();
        for (TrayIcon t : trayIcons) {
            systemTray.remove(t);
        }
        Common.showGui();
    }

    public static void animation(JButton exitBtn, ImageIcon off, ImageIcon on) {
        exitBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                exitBtn.setIcon(on);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                exitBtn.setIcon(off);
            }
        });
    }

    public static void animation(JLabel label, ImageIcon off, ImageIcon on) {
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                label.setIcon(on);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setIcon(off);
            }
        });
    }

    private void animation(JLabel label) {
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!label.isEnabled()) {
                    label.setEnabled(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (label.isEnabled()) {
                    label.setEnabled(false);
                }
            }
        });
    }

    public static void openPage(String url) {
        if (url != null && !url.equals("no data found")) {
            url = url.replaceAll(("https://|http://"), "");

            URI uri = null;
            try {
                uri = new URI("https://" + url);
            } catch (URISyntaxException ex) {
                ex.printStackTrace();
            }
            Desktop desktop = Desktop.getDesktop();
            assert uri != null;
            try {
                desktop.browse(uri);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private JMenu createFileMenu() {
        JMenu file = new JMenu("File");
        ImageIcon iconItem = Icons.MINUS_RED_ICON;
        ImageIcon iconItem2 = Icons.MINUS_BLUE_ICON;
        ImageIcon iconItem3 = Icons.MINUS_GREEN_ICON;

        JMenuItem sources = new JMenuItem("Sources", iconItem);
        KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        sources.setAccelerator(ctrlS);
        sources.addActionListener(x -> new Dialogs("dialog_sources"));

        JMenuItem keywords = new JMenuItem("Keywords", iconItem);
        KeyStroke ctrlK = KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        keywords.setAccelerator(ctrlK);
        keywords.addActionListener(x -> new Dialogs("dialog_keywords"));

        JMenuItem excludedHeadlines = new JMenuItem("Excluded", iconItem);
        KeyStroke ctrlE = KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        excludedHeadlines.setAccelerator(ctrlE);
        excludedHeadlines.addActionListener(x -> new Dialogs("dialog_excluded_headlines"));

        JMenuItem favorites = new JMenuItem("Favorites", iconItem);
        KeyStroke ctrlW = KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        favorites.setAccelerator(ctrlW);
        favorites.addActionListener(x -> new Dialogs("dialog_favorites"));

        JMenuItem feelings = new JMenuItem("Feelings", iconItem);
        KeyStroke ctrlF = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        feelings.setAccelerator(ctrlF);
        feelings.addActionListener(x -> new Dialogs("dialog_feelings"));

        JMenuItem dates = new JMenuItem("Dates", iconItem);
        KeyStroke ctrlD = KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        dates.setAccelerator(ctrlD);
        dates.addActionListener(x -> new Dialogs("dialog_dates"));

        JMenuItem settings = new JMenuItem("Settings", iconItem2);
        KeyStroke ctrlP = KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        settings.setAccelerator(ctrlP);
        settings.addActionListener(x -> getSettingsDialog());

        JMenuItem sqlite = new JMenuItem("Sqlite", iconItem2);
        sqlite.setToolTipText("Press CTRL+v in SQLIte IDE to insert database path");
        KeyStroke ctrlL = KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        sqlite.setAccelerator(ctrlL);
        sqlite.addActionListener(x -> showSQLiteIde());

        JMenuItem exit = new JMenuItem("Exit", iconItem3);
        KeyStroke ctrlQ = KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        exit.setAccelerator(ctrlQ);
        exit.addActionListener(x -> System.exit(0));

        // Добавить слово в ключевые слова для поиска
        JMenu exportMenu = new JMenu("Export");
        exportMenu.setIcon(iconItem2);

        JMenuItem itemXls = new JMenuItem("XLS", Icons.EXPORT_XLS_ICON);
        itemXls.addActionListener(x -> {
            if (modelMain.getRowCount() != 0) {
                new Thread(new ExportToExcel()::exportMainTableToExcel).start();
            } else {
                Common.showAlert("No data to export");
            }
        });

        JMenuItem itemCsv = new JMenuItem("CSV", Icons.EXPORT_CSV_ICON);
        itemCsv.addActionListener(x -> {
            if (modelMain.getRowCount() != 0) {
                new Thread(new ExportToCsv()::export).start();
            } else {
                Common.showAlert("No data to export");
            }
        });

        exportMenu.add(itemXls);
        exportMenu.add(itemCsv);

        file.add(sources);
        file.add(keywords);
        file.add(excludedHeadlines);
        file.add(favorites);
        file.add(feelings);
        file.add(dates);
        file.addSeparator();
        file.add(exportMenu);
        file.add(settings);
        file.add(sqlite);
        file.addSeparator();
        file.add(exit);

        return file;
    }

    private JMenu createViewMenu() {
        String sizeGui = jdbcQueries.getSetting("gui_theme");
        String minSize = GuiSize.SMALL.getSize();
        String midSize = GuiSize.MIDDLE.getSize();
        String maxSize = GuiSize.LARGE.getSize();
        JMenu viewMenu = new JMenu("View");

        JCheckBoxMenuItem small = new JCheckBoxMenuItem(minSize);
        small.addActionListener(e -> {
            jdbcQueries.updateSettings("gui_theme", minSize);
            jdbcQueries.updateSettings("font_size", "17");
            jdbcQueries.updateSettings("row_height", "28");
            setDefaultDialogsXY();
            refreshGui();
        });
        JCheckBoxMenuItem middle = new JCheckBoxMenuItem(midSize);
        middle.addActionListener(e -> {
            jdbcQueries.updateSettings("gui_theme", midSize);
            jdbcQueries.updateSettings("font_size", "19");
            jdbcQueries.updateSettings("row_height", "30");
            setDefaultDialogsXY();
            refreshGui();
        });
        JCheckBoxMenuItem large = new JCheckBoxMenuItem(maxSize);
        large.addActionListener(e -> {
            jdbcQueries.updateSettings("gui_theme", maxSize);
            jdbcQueries.updateSettings("font_size", "19");
            jdbcQueries.updateSettings("row_height", "29");
            setDefaultDialogsXY();
            refreshGui();
        });

        JMenuItem x0y0 = new JMenuItem("0");
        KeyStroke ctrl0 = KeyStroke.getKeyStroke(KeyEvent.VK_0, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        x0y0.setAccelerator(ctrl0);
        x0y0.addActionListener(e -> {
            jdbcQueries.updateSettings("gui_x", "0");
            jdbcQueries.updateSettings("gui_y", "0");
            setDefaultDialogsXY();
            refreshGui();
        });

        JMenuItem x40y40 = new JMenuItem("40 : 40");
        x40y40.addActionListener(e -> {
            jdbcQueries.updateSettings("gui_x", "40");
            jdbcQueries.updateSettings("gui_y", "40");
            refreshGui();
            setDefaultDialogsXY();
        });

        JMenuItem x135y40 = new JMenuItem("135 : 40");
        x135y40.addActionListener(e -> {
            jdbcQueries.updateSettings("gui_x", "135");
            jdbcQueries.updateSettings("gui_y", "40");
            setDefaultDialogsXY();
            refreshGui();
        });

        JMenuItem x280y150 = new JMenuItem("280 : 150");
        x280y150.addActionListener(e -> {
            jdbcQueries.updateSettings("gui_x", "280");
            jdbcQueries.updateSettings("gui_y", "150");
            setDefaultDialogsXY();
            refreshGui();
        });

        JMenuItem defaultDialogsXY = new JMenuItem("Default X Y (dialogs)");
        defaultDialogsXY.addActionListener(e -> setDefaultDialogsXY());

        // организуем переключатели в логическую группу
        ButtonGroup bg = new ButtonGroup();
        bg.add(small);
        bg.add(middle);
        bg.add(large);

        if (sizeGui.equals(minSize)) {
            bg.setSelected(small.getModel(), true);
        } else if (sizeGui.equals(midSize)) {
            bg.setSelected(middle.getModel(), true);
        } else if (sizeGui.equals(maxSize)) {
            bg.setSelected(large.getModel(), true);
        }

        JMenuItem readMode = new JMenuItem("Read mode", Icons.READ_MODE_ICON);
        KeyStroke ctrlR = KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        readMode.setAccelerator(ctrlR);
        readMode.addActionListener(e -> {
            if (modelMain.getRowCount() > 0) {
                new Dialogs("dialog_read_only");
            } else {
                Common.showAlert("No news headlines to show!");
            }
        });

        // Случайные цвета
        JMenuItem randomGuiColors = new JMenuItem("Random");
        KeyStroke ctrlRight = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        randomGuiColors.setAccelerator(ctrlRight);
        randomGuiColors.addActionListener(x -> {
            Common.setRandomColors();
            refreshGui();
        });

        // Установить цвета приложения по-умолчанию
        JMenuItem defaultGuiColors = new JMenuItem("Default");
        KeyStroke ctrlLeft = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        defaultGuiColors.setAccelerator(ctrlLeft);
        defaultGuiColors.addActionListener(x -> {
            Common.setDefaultColors();
            refreshGui();
        });

        viewMenu.add(readMode);
        viewMenu.addSeparator();
        viewMenu.add(new JLabel("  Size"));
        viewMenu.add(small);
        viewMenu.add(middle);
        viewMenu.add(large);
        viewMenu.addSeparator();
        viewMenu.add(new JLabel("  Interface colors"));
        viewMenu.add(randomGuiColors);
        viewMenu.add(defaultGuiColors);
        viewMenu.addSeparator();
        viewMenu.add(new JLabel("  Position XY"));
        viewMenu.add(x0y0);
        viewMenu.add(x40y40);
        viewMenu.add(x135y40);
        viewMenu.add(x280y150);
        viewMenu.add(defaultDialogsXY);

        return viewMenu;
    }

    private void setDefaultDialogsXY() {
        JdbcQueries queries = new JdbcQueries();
        queries.updateSettings("dialog_sources_xy", "-1,-1");
        queries.updateSettings("dialog_excluded_analysis_xy", "-1,-1");
        queries.updateSettings("dialog_excluded_headlines_xy", "-1,-1");
        queries.updateSettings("dialog_keywords_xy", "-1,-1");
        queries.updateSettings("dialog_favorites_xy", "-1,-1");
        queries.updateSettings("dialog_dates_xy", "-1,-1");
        queries.updateSettings("dialog_feelings_xy", "-1,-1");
        queries.updateSettings("dialog_read_only_xy", "-1,-1");
    }

    private JMenu createShowUrl() {
        JMenu urlMenu = new JMenu("Links");

        String link1 = jdbcQueries.getSetting("link1");
        JMenuItem link1item = new JMenuItem(Common.getNameFromUrl(link1));
        KeyStroke ctrl1 = KeyStroke.getKeyStroke(KeyEvent.VK_1, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        link1item.setAccelerator(ctrl1);
        link1item.setIcon(Icons.MINUS_RED_ICON);
        link1item.addActionListener(e -> openPage(link1));

        String link2 = jdbcQueries.getSetting("link2");
        JMenuItem link2item = new JMenuItem(Common.getNameFromUrl(link2));
        KeyStroke ctrl2 = KeyStroke.getKeyStroke(KeyEvent.VK_2, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        link2item.setAccelerator(ctrl2);
        link2item.setIcon(Icons.MINUS_BLUE_ICON);
        link2item.addActionListener(e -> openPage(link2));

        String link3 = jdbcQueries.getSetting("link3");
        JMenuItem link3item = new JMenuItem(Common.getNameFromUrl(link3));
        KeyStroke ctrl3 = KeyStroke.getKeyStroke(KeyEvent.VK_3, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        link3item.setAccelerator(ctrl3);
        link3item.setIcon(Icons.MINUS_GREEN_ICON);
        link3item.addActionListener(e -> openPage(link3));

        String link4 = jdbcQueries.getSetting("link4");
        JMenuItem link4item = new JMenuItem(Common.getNameFromUrl(link4));
        KeyStroke ctrl4 = KeyStroke.getKeyStroke(KeyEvent.VK_4, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        link4item.setAccelerator(ctrl4);
        link4item.setIcon(Icons.MINUS_RED_ICON);
        link4item.addActionListener(e -> openPage(link4));

        String link5 = jdbcQueries.getSetting("link5");
        JMenuItem link5item = new JMenuItem(Common.getNameFromUrl(link5));
        KeyStroke ctrl5 = KeyStroke.getKeyStroke(KeyEvent.VK_5, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        link5item.setAccelerator(ctrl5);
        link5item.setIcon(Icons.MINUS_BLUE_ICON);
        link5item.addActionListener(e -> openPage(link5));

        String link6 = jdbcQueries.getSetting("link6");
        JMenuItem link6item = new JMenuItem(Common.getNameFromUrl(link6));
        KeyStroke ctrl6 = KeyStroke.getKeyStroke(KeyEvent.VK_6, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        link6item.setAccelerator(ctrl6);
        link6item.setIcon(Icons.MINUS_GREEN_ICON);
        link6item.addActionListener(e -> openPage(link6));

        JMenuItem openAllLInks = new JMenuItem("Open all", Icons.WHEN_MOUSE_ON_LIST_BUTTON_ICON);
        KeyStroke ctrl7 = KeyStroke.getKeyStroke(KeyEvent.VK_7, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        openAllLInks.setAccelerator(ctrl7);
        openAllLInks.addActionListener(e -> {
            openPage(link1);
            openPage(link2);
            openPage(link3);
            openPage(link4);
            openPage(link5);
            openPage(link6);
        });

        JMenuItem changeUrl = new JMenuItem("Edit", Icons.EDIT_ICON);
        changeUrl.addActionListener(e -> {
            JPanel panel = new JPanel();
            JTextField link1text = new JTextField(jdbcQueries.getSetting("link1"), 18);
            JTextField link2text = new JTextField(jdbcQueries.getSetting("link2"), 18);
            JTextField link3text = new JTextField(jdbcQueries.getSetting("link3"), 18);
            JTextField link4text = new JTextField(jdbcQueries.getSetting("link4"), 18);
            JTextField link5text = new JTextField(jdbcQueries.getSetting("link5"), 18);
            JTextField link6text = new JTextField(jdbcQueries.getSetting("link6"), 18);

            panel.setLayout(new GridLayout(6, 0, 1, 5));
            panel.add(new JLabel("Link 1"));
            panel.add(link1text);
            panel.add(new JLabel("Link 2"));
            panel.add(link2text);
            panel.add(new JLabel("Link 3"));
            panel.add(link3text);
            panel.add(new JLabel("Link 4"));
            panel.add(link4text);
            panel.add(new JLabel("Link 5"));
            panel.add(link5text);
            panel.add(new JLabel("Link 6"));
            panel.add(link6text);

            String[] menu = new String[]{"Cancel", "Update"};
            int option = JOptionPane.showOptionDialog(mainTableScrollPane, panel, "Change links",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, Icons.EDIT_ICON, menu, menu[1]);

            if (option == 1) {
                if (link1text.getText().length() != 0 &&
                        link2text.getText().length() != 0 &&
                        link3text.getText().length() != 0 &&
                        link4text.getText().length() != 0 &&
                        link5text.getText().length() != 0 &&
                        link6text.getText().length() != 0) {
                    jdbcQueries.updateSettings("link1", link1text.getText());
                    jdbcQueries.updateSettings("link2", link2text.getText());
                    jdbcQueries.updateSettings("link3", link3text.getText());
                    jdbcQueries.updateSettings("link4", link4text.getText());
                    jdbcQueries.updateSettings("link5", link5text.getText());
                    jdbcQueries.updateSettings("link6", link6text.getText());
                    Common.console("links changed");

                    refreshGui();
                } else {
                    Common.showAlert("Link can't be empty!");
                }
            }
        });

        urlMenu.add(link1item);
        urlMenu.add(link2item);
        urlMenu.add(link3item);
        urlMenu.add(link4item);
        urlMenu.add(link5item);
        urlMenu.add(link6item);
        urlMenu.addSeparator();
        urlMenu.add(changeUrl);
        urlMenu.addSeparator();
        urlMenu.add(openAllLInks);

        return urlMenu;
    }

    private JMenu createHelp() {
        JMenu helpMenu = new JMenu("Help");

        JMenuItem info = new JMenuItem("Info", Icons.LOGO_ICON);
        KeyStroke ctrlI = KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        info.setAccelerator(ctrlI);

        info.addActionListener(e -> {
            String text = "<html>" +
                    "<b><font color=\"#C4A431\">" + name + "</font></b><br/>" +
                    ver + " <b><font color=\"#31b547\">" + Main.APP_VERSION + "</font></b>" +
                    dat + "<b><font color=\"#31b547\">" + Main.APP_VERSION_DATE + "</b></font><br/>" +
                    owner + "<br/>" +
                    register + "<br/>" +
                    registerRusPro + "<br/>" +
                    "<font color=\"#FF7373\">avandy-news.ru</font><br/" +
                    "<font color=\"#fa8e47\">rps_project@mail.ru</font><br/> " +
                    "<font color=\"#59C9FF\">github.com/mrprogre</font><br/><br/>" +
                    "Permission is hereby granted, free of charge, to any person obtaining a copy of this software<br/>" +
                    "and associated documentation files (the \"Software\"), to deal in the Software without restriction,<br/>" +
                    "including without limitation the rights to use, copy, modify, merge, publish, distribute,<br/>" +
                    "sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is<br/>" +
                    "furnished to do so, subject to the following conditions: The above copyright notice and this<br/>" +
                    "permission notice shall be included in all copies or substantial portions of the Software.<br/<br/" +

                    "The software is provided \"as is\", without warranty of any kind, express or implied, including<br/>" +
                    "but not limited to the warranties of merchantability, fitness for a particular purpose and<br/>" +
                    "noninfringement. In no event shall the authors or copyright holders be liable for any claim,<br/>" +
                    "damages or other liability, whether in an action of contract, tort or otherwise,<br/>" +
                    "arising from, out of or in connection with the software or the use <br/>" +
                    "or other dealings in the software." +
                    "</<html>";

            JOptionPane.showMessageDialog(mainTableScrollPane, text, donate,
                    JOptionPane.INFORMATION_MESSAGE, Icons.qrSbp);
        });

        JMenuItem manual = new JMenuItem("Manual", Icons.MANUAL_ICON);
        KeyStroke ctrlF1 = KeyStroke.getKeyStroke(KeyEvent.VK_F1, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        manual.setAccelerator(ctrlF1);
        manual.addActionListener(e -> {
            if (Desktop.isDesktopSupported()) {
                File myFile = new File(Common.DIRECTORY_PATH + "manual.docx");
                try {
                    Desktop.getDesktop().open(myFile);
                } catch (IOException ex) {
                    Common.showAlert("File manual.docx not found");
                }
            }
        });

        JMenuItem support = new JMenuItem("Support", Icons.MESSAGE_ICON);
        KeyStroke ctrlF2 = KeyStroke.getKeyStroke(KeyEvent.VK_F2, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        support.setAccelerator(ctrlF2);
        support.addActionListener(e -> openPage("https://avandy-news.ru/"));

        JMenuItem showAssistant = new JMenuItem("Show assistant", Icons.ASSISTANT_ICON);
        KeyStroke ctrlF3 = KeyStroke.getKeyStroke(KeyEvent.VK_F3, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        showAssistant.setAccelerator(ctrlF3);
        showAssistant.addActionListener(e -> Common.showAssistant());

        JMenuItem checkForUpdates = new JMenuItem("Check for Updates", Icons.CHECK_FOR_UPDATES_ICON);
        KeyStroke ctrlF4 = KeyStroke.getKeyStroke(KeyEvent.VK_F4, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        checkForUpdates.setAccelerator(ctrlF4);
        checkForUpdates.addActionListener(e -> new Thread(() -> {
            try {
                String url = null;
                URL oracle = new URL("https://avandy-news.ru/version.txt");
                BufferedReader in = new BufferedReader(new InputStreamReader(oracle.openStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {

                    if (OsChecker.isUnix() && inputLine.contains("lin")) {
                        url = inputLine.substring(4, inputLine.indexOf(";"));
                    } else if (OsChecker.isWindows() && inputLine.contains("win")) {
                        url = inputLine.substring(4, inputLine.indexOf(";"));
                    }

                    if (inputLine.contains("ver")) {
                        String appVer = inputLine.substring(4, inputLine.indexOf(";"));
                        if (Main.APP_VERSION.equals(appVer))
                            Common.showInfo("You already have the latest version of Avandy News Analysis");
                        else {
                            String text = "A new version of the program is available. Download update?";

                            String[] menu = new String[]{"No", "Get"};
                            int option = JOptionPane.showOptionDialog(mainTableScrollPane,
                                    text, "New version of App", JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.PLAIN_MESSAGE, Icons.CHECK_FOR_UPDATES_ICON, menu, menu[1]);
                            if (option == 1 && url.length() > 0) {
                                openPage(url);
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                Common.showAlert(exception.getMessage());
            }
        }).start());

        JMenuItem game = new JMenuItem("Play game", Icons.GAME_ICON);
        KeyStroke ctrlF5 = KeyStroke.getKeyStroke(KeyEvent.VK_F5, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        game.setAccelerator(ctrlF5);
        game.addActionListener(e -> {
            try {
                String jar = Common.DIRECTORY_PATH + "brain-shake-game.jar";

                Runtime.getRuntime().exec("java -jar " + jar);
            } catch (IOException ioe) {
                Common.showAlert(ioe.getMessage());
            }
        });

        helpMenu.add(info);
        helpMenu.add(manual);
        helpMenu.add(support);
        helpMenu.add(showAssistant);
        helpMenu.add(checkForUpdates);
        helpMenu.add(game);
        return helpMenu;
    }

    private void createUserAnimation() {
        loginLabel.setForeground(Color.RED);
        loginLabel.setText("create user");
        try {
            for (int i = 0; i < 10; i++) {
                Thread.sleep(500);
                loginLabel.setEnabled(true);
                Thread.sleep(500);
                loginLabel.setEnabled(false);
            }
        } catch (InterruptedException e) {
            Common.showAlert(e.getMessage());
        }
        loginLabel.setForeground(guiFontColor);
    }

}