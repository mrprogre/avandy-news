package com.avandy.news.gui;

import com.avandy.news.database.JdbcQueries;
import com.avandy.news.export.ExportToCsv;
import com.avandy.news.export.ExportToExcel;
import com.avandy.news.model.*;
import com.avandy.news.search.Search;
import com.avandy.news.utils.Common;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;

public class Dialogs extends JDialog implements KeyListener {
    private final JdbcQueries jdbcQueries = new JdbcQueries();
    public static JTable table;
    public static DefaultTableModel model;
    private final String dialogName;
    public static final Color FONT_COLOR = new Color(220, 179, 56);
    JComboBox<Integer> positionCombobox = new JComboBox<>();

    public Dialogs(String name) {
        String nameXY = name + "_xy";
        HashMap<String, Integer> guiSettings = Common.guiSettings;
        JTextArea textAreaForDialogs = new JTextArea();
        textAreaForDialogs.setFont(new Font("Dialog", Font.PLAIN, 13));
        textAreaForDialogs.setTabSize(10);
        textAreaForDialogs.setEditable(false);
        textAreaForDialogs.setLineWrap(true);
        textAreaForDialogs.setWrapStyleWord(true);
        textAreaForDialogs.setBounds(12, 27, 22, 233);
        this.setResizable(true);
        this.setFont(new Font("Tahoma", Font.PLAIN, 14));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setVisible(true);
        JScrollPane scrollPane = new JScrollPane();
        this.getContentPane().setLayout(new BorderLayout(0, 0));
        this.addKeyListener(this);
        Container container = getContentPane();
        this.setAlwaysOnTop(false);

        switch (name) {
            case "dialog_sources": {
                this.setBounds(0, 0, 500, 308);
                this.setTitle("Sources");
                this.setLocationRelativeTo(Gui.mainTableScrollPane);

                for (int i = 1; i <= 100 ; i++) {
                    positionCombobox.addItem(i);
                }

                Object[] columns = {"N", "Country", "Source", "Link", "Pos", "", " "};
                model = new DefaultTableModel(new Object[][]{
                }, columns) {
                    final boolean[] columnEditable = new boolean[]{false, false, false, false, true, true, true};

                    public boolean isCellEditable(int row, int column) {
                        return columnEditable[column];
                    }

                    // Сортировка
                    final Class[] types_unique = {Integer.class, String.class, String.class, String.class,
                            JComboBox.class, Boolean.class, Button.class};

                    @Override
                    public Class getColumnClass(int columnIndex) {
                        return this.types_unique[columnIndex];
                    }
                };
                table = new JTable(model);
                table.getColumn("").setCellEditor(new CheckBoxEditor(new JCheckBox()));
                table.getColumn("Pos").setCellEditor(new DefaultCellEditor(positionCombobox));
                table.getColumn(" ").setCellRenderer(new ButtonColumn(table, 6));
                table.getColumn(" ").setCellRenderer(new ButtonColumn(table, 6));
                table.setAutoCreateRowSorter(true);
                table.getTableHeader().setReorderingAllowed(false);
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(JLabel.CENTER);
                table.setRowHeight(20);
                table.setFont(new Font("SansSerif", Font.PLAIN, 13));
                JTableHeader header = table.getTableHeader();
                header.setFont(new Font("Tahoma", Font.BOLD, 13));
                table.getColumn("N").setCellRenderer(renderer);
                table.getColumn("N").setMaxWidth(40);
                table.getColumn("N").setPreferredWidth(40);
                table.getColumn("Country").setPreferredWidth(100);
                table.getColumn("Source").setPreferredWidth(150);
                table.getColumn("Link").setPreferredWidth(250);
                table.getColumn("Pos").setMaxWidth(50);
                table.getColumn("Pos").setCellRenderer(renderer);
                table.getColumn("").setMaxWidth(30);
                table.getColumn(" ").setMaxWidth(30);
                table.setColumnSelectionAllowed(true);
                table.setCellSelectionEnabled(true);
                table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

                scrollPane.setBounds(10, 27, 324, 233);
                scrollPane.setViewportView(table);
                // открыть новость в браузере по дабл клику на заголовке
                table.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            int row = table.convertRowIndexToModel(table.rowAtPoint(new Point(e.getX(), e.getY())));
                            int col = table.convertColumnIndexToModel(table.columnAtPoint(new Point(e.getX(), e.getY())));
                            if (col == 3) {
                                String url = (String) table.getModel().getValueAt(row, 3);
                                Gui.openPage(url);
                            }
                        }
                    }
                });

                positionCombobox.addActionListener(e -> {
                    int countryColumnNum = getColumnIndex("Country");
                    int sourceColumnNum = getColumnIndex("Source");

                    int row = table.getSelectedRow();
                    int col = table.getSelectedColumn();
                    if (row != -1) {
                        if (table.getValueAt(row, col) != null) {
                            String countryValue = table.getValueAt(row, countryColumnNum).toString();
                            String sourceValue = table.getValueAt(row, sourceColumnNum).toString();
                            int positionValue = (int) positionCombobox.getSelectedItem();

                            jdbcQueries.updateRssPosition(positionValue, countryValue, sourceValue);
                            this.setVisible(false);
                            new Dialogs("dialog_sources");
                        }
                    }
                });

                JButton addButton = new JButton("Add RSS");
                addButton.setForeground(FONT_COLOR);
                addButton.addActionListener(e -> {
                    JTextField rss = new JTextField();
                    JTextField link = new JTextField();

                    Object[] newSource = {"Source", rss, "Link to rss", link, "Country", Common.countriesCombobox};
                    int result = JOptionPane.showConfirmDialog(this, newSource,
                            "New source", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);

                    String linkValue = link.getText();

                    if (rss.getText().length() > 0 && linkValue.length() > 0) {
                        if (result == JOptionPane.OK_OPTION) {
                            Common.showInfo(TextLang.checkAddedRss);

                            if (!Common.checkRss(linkValue)) {
                                Common.showAlertHtml(TextLang.checkAddedRssFailed + linkValue);
                                return;
                            }

                            saveDialogPosition(nameXY);
                            jdbcQueries.addNewSource(rss.getText(), linkValue,
                                    (String) Common.countriesCombobox.getSelectedItem());
                            this.setVisible(false);
                            new Dialogs("dialog_sources");
                            setDialogPosition(nameXY);
                        }
                    }
                });

                container.add(addButton, "South");
                container.add(scrollPane);

                showDialogs("smi");
                break;
            }
            case "dialog_excluded_analysis": {
                this.setBounds(0, 0, 250, 306);
                this.setTitle("Excluded from analysis");
                this.setLocationRelativeTo(Gui.amountOfNewsLabel);
                Object[] columns = {"Num", "Word", "Del"};
                model = new DefaultTableModel(new Object[][]{
                }, columns) {
                    final boolean[] columnEditable = new boolean[]{false, false, true};

                    public boolean isCellEditable(int row, int column) {
                        return columnEditable[column];
                    }

                    // Сортировка
                    final Class[] types_unique = {Integer.class, String.class, Button.class};

                    @Override
                    public Class getColumnClass(int columnIndex) {
                        return this.types_unique[columnIndex];
                    }
                };
                table = new JTable(model);
                table.getColumn("Del").setCellRenderer(new ButtonColumn(table, 2));
                table.setAutoCreateRowSorter(true);
                table.getTableHeader().setReorderingAllowed(false);
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(JLabel.CENTER);
                table.setRowHeight(20);
                table.setFont(new Font("SansSerif", Font.PLAIN, 13));
                JTableHeader header = table.getTableHeader();
                header.setFont(new Font("Tahoma", Font.BOLD, 13));
                table.getColumnModel().getColumn(0).setCellRenderer(renderer);
                table.getColumnModel().getColumn(0).setMaxWidth(40);
                table.getColumnModel().getColumn(2).setMaxWidth(40);
                getContentPane().add(table, BorderLayout.CENTER);

                scrollPane.setBounds(10, 27, 324, 233);
                this.getContentPane().add(scrollPane);
                scrollPane.setViewportView(table);

                showDialogs("excl");
                break;
            }
            case "dialog_excluded_headlines": {
                this.setBounds(0, 0, 250, 298);
                this.setTitle("Excluded headlines");
                this.setLocationRelativeTo(Gui.mainTableScrollPane);

                Object[] columns = {"Num", "Word", "Del"};
                model = new DefaultTableModel(new Object[][]{
                }, columns) {
                    final boolean[] columnEditable = new boolean[]{false, false, true};

                    public boolean isCellEditable(int row, int column) {
                        return columnEditable[column];
                    }

                    // Сортировка
                    final Class[] types_unique = {Integer.class, String.class, Button.class};

                    @Override
                    public Class getColumnClass(int columnIndex) {
                        return this.types_unique[columnIndex];
                    }
                };
                table = new JTable(model);
                table.getColumn("Del").setCellRenderer(new ButtonColumn(table, 2));
                table.setAutoCreateRowSorter(true);
                table.getTableHeader().setReorderingAllowed(false);
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(JLabel.CENTER);
                table.setRowHeight(20);
                table.setFont(new Font("SansSerif", Font.PLAIN, 13));
                JTableHeader header = table.getTableHeader();
                header.setFont(new Font("Tahoma", Font.BOLD, 13));
                table.getColumnModel().getColumn(0).setCellRenderer(renderer);
                table.getColumnModel().getColumn(0).setMaxWidth(40);
                table.getColumnModel().getColumn(2).setMaxWidth(40);
                table.setColumnSelectionAllowed(true);
                table.setCellSelectionEnabled(true);
                table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                scrollPane.setBounds(10, 27, 324, 233);
                scrollPane.setViewportView(table);

                JPanel addPanel = new JPanel();

                JTextField word = new JTextField(11);
                addPanel.add(word);

                JButton addButton = new JButton("add word");
                addButton.setForeground(FONT_COLOR);
                addButton.addActionListener(e -> {
                    if (word.getText().length() > 0) {
                        saveDialogPosition(nameXY);
                        jdbcQueries.addWordToExcludeTitles(word.getText());
                        this.setVisible(false);
                        new Dialogs("dialog_excluded_headlines");
                        setDialogPosition(nameXY);
                    }
                });
                addPanel.add(addButton);

                container.add(addPanel, "South");
                container.add(scrollPane);

                showDialogs("title-excl");
                break;
            }
            case "dialog_keywords": {
                this.setBounds(0, 0, 250, 298);
                this.setTitle("Keywords");
                this.setLocationRelativeTo(Gui.mainTableScrollPane);

                Object[] columns = {"Pos", "Keyword", "", " "};
                model = new DefaultTableModel(new Object[][]{
                }, columns) {
                    final boolean[] columnEditable = new boolean[]{false, false, true, true};

                    public boolean isCellEditable(int row, int column) {
                        return columnEditable[column];
                    }

                    // Сортировка
                    final Class[] types_unique = {Integer.class, String.class, Boolean.class, Button.class};

                    @Override
                    public Class getColumnClass(int columnIndex) {
                        return this.types_unique[columnIndex];
                    }
                };
                table = new JTable(model);
                table.getColumnModel().getColumn(2).setCellEditor(new CheckBoxEditor(new JCheckBox()));
                table.getColumn(" ").setCellRenderer(new ButtonColumn(table, 3));
                table.setAutoCreateRowSorter(true);
                table.getTableHeader().setReorderingAllowed(false);
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(JLabel.CENTER);
                table.setRowHeight(20);
                table.setFont(new Font("SansSerif", Font.PLAIN, 13));
                JTableHeader header = table.getTableHeader();
                header.setFont(new Font("Tahoma", Font.BOLD, 13));
                table.getColumnModel().getColumn(0).setCellRenderer(renderer);
                table.getColumnModel().getColumn(0).setMaxWidth(30);
                table.getColumnModel().getColumn(2).setMaxWidth(30);
                table.getColumnModel().getColumn(3).setMaxWidth(30);
                table.setColumnSelectionAllowed(true);
                table.setCellSelectionEnabled(true);
                table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                scrollPane.setBounds(10, 27, 324, 233);
                scrollPane.setViewportView(table);

                JPanel addPanel = new JPanel();

                JTextField word = new JTextField(11);
                addPanel.add(word);

                JButton addButton = new JButton("add word");
                addButton.setForeground(FONT_COLOR);
                addButton.addActionListener(e -> {
                    if (word.getText().length() > 0) {
                        saveDialogPosition(nameXY);
                        jdbcQueries.addKeyword(word.getText());
                        this.setVisible(false);
                        new Dialogs("dialog_keywords");
                        setDialogPosition(nameXY);
                    }
                });

                addPanel.add(addButton);
                container.add(addPanel, "South");
                container.add(scrollPane);

                showDialogs("keywords");
                break;
            }
            case "dialog_favorites": {
                this.setBounds(0, 0, 800, 400);
                this.setTitle("Favorites");
                this.setLocationRelativeTo(Gui.mainTableScrollPane);

                Object[] columns = {"", "title", "added", "link", " "};
                model = new DefaultTableModel(new Object[][]{
                }, columns) {
                    final boolean[] columnEditable = new boolean[]{false, false, false, false, true};

                    public boolean isCellEditable(int row, int column) {
                        return columnEditable[column];
                    }

                    // Сортировка
                    final Class[] types_unique = {Integer.class, String.class, String.class, String.class, Button.class};

                    @Override
                    public Class getColumnClass(int columnIndex) {
                        return this.types_unique[columnIndex];
                    }
                };
                table = new JTable(model);
                table.getColumn(" ").setCellRenderer(new ButtonColumn(table, 4));
                table.setAutoCreateRowSorter(true);
                table.getTableHeader().setReorderingAllowed(false);
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(JLabel.CENTER);
                table.setRowHeight(20);
                table.setFont(new Font("SansSerif", Font.PLAIN, 13));
                JTableHeader header = table.getTableHeader();
                header.setFont(new Font("Tahoma", Font.BOLD, 13));
                table.getColumnModel().getColumn(0).setCellRenderer(renderer);
                table.getColumnModel().getColumn(0).setMaxWidth(30);
                table.getColumnModel().getColumn(2).setPreferredWidth(130);
                table.getColumnModel().getColumn(2).setMaxWidth(130);
                table.getColumnModel().getColumn(3).setMaxWidth(70);
                table.getColumnModel().getColumn(4).setMaxWidth(30);
                table.removeColumn(table.getColumnModel().getColumn(3)); // Скрыть ссылку
                table.setColumnSelectionAllowed(true);
                table.setCellSelectionEnabled(true);
                table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                getContentPane().add(table, BorderLayout.CENTER);

                // открытие вкладки двойным кликом
                table.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            int row = table.convertRowIndexToModel(table.rowAtPoint(new Point(e.getX(), e.getY()))); // при сортировке строк оставляет верные данные
                            int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
                            if (col == 1 || col == 3) {
                                String url = (String) table.getModel().getValueAt(row, 3);
                                Gui.openPage(url);
                            }
                        }
                    }
                });

                scrollPane.setBounds(10, 27, 324, 233);
                this.getContentPane().add(scrollPane);
                scrollPane.setViewportView(table);

                showDialogs("favorites");
                break;
            }
            case "dialog_dates": {
                this.setBounds(600, 200, 600, 338);
                this.setTitle("Dates");
                this.setLocationRelativeTo(Gui.mainTableScrollPane);
                Object[] columns = {"Type", "Description", "Day", "Month", "Year", "", "Del"};
                model = new DefaultTableModel(new Object[][]{
                }, columns) {
                    final boolean[] columnEditable = new boolean[]{false, false, false, false, false, true, true};

                    public boolean isCellEditable(int row, int column) {
                        return columnEditable[column];
                    }

                    // Сортировка
                    final Class[] types_unique = {String.class, String.class, Integer.class, Integer.class,
                            Integer.class, Boolean.class, Button.class};

                    @Override
                    public Class getColumnClass(int columnIndex) {
                        return this.types_unique[columnIndex];
                    }
                };
                table = new JTable(model);
                table.getColumnModel().getColumn(5).setCellEditor(new CheckBoxEditor(new JCheckBox()));
                table.getColumn("Del").setCellRenderer(new ButtonColumn(table, 6));
                table.setAutoCreateRowSorter(true);
                table.getTableHeader().setReorderingAllowed(false);
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(JLabel.CENTER);
                table.setRowHeight(20);
                table.setFont(new Font("SansSerif", Font.PLAIN, 13));
                JTableHeader header = table.getTableHeader();
                header.setFont(new Font("Tahoma", Font.BOLD, 13));
                table.getColumnModel().getColumn(0).setPreferredWidth(140);
                table.getColumnModel().getColumn(0).setMaxWidth(140);
                table.getColumnModel().getColumn(2).setPreferredWidth(60);
                table.getColumnModel().getColumn(2).setMaxWidth(60);
                table.getColumnModel().getColumn(2).setCellRenderer(renderer);
                table.getColumnModel().getColumn(3).setPreferredWidth(60);
                table.getColumnModel().getColumn(3).setMaxWidth(60);
                table.getColumnModel().getColumn(3).setCellRenderer(renderer);
                table.getColumnModel().getColumn(4).setPreferredWidth(60);
                table.getColumnModel().getColumn(4).setMaxWidth(60);
                table.getColumnModel().getColumn(4).setCellRenderer(renderer);
                table.getColumnModel().getColumn(5).setMaxWidth(40);
                table.getColumnModel().getColumn(6).setMaxWidth(30);
                table.setColumnSelectionAllowed(true);
                table.setCellSelectionEnabled(true);
                table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                scrollPane.setBounds(10, 27, 324, 233);
                scrollPane.setViewportView(table);

                // Панель добавления даты
                String[] dateTypes = {"Birthday", "Event", "Holiday"};
                String[] months = {"January", "February", "March", "April", "May", "June", "July",
                        "August", "September", "October", "November", "December"};
                Integer[] days = new Integer[31];
                for (int i = 0; i < days.length; i++) {
                    days[i] = i + 1;
                }

                JComboBox<String> typesComboBox = new JComboBox<>(dateTypes);
                JTextField description = new JTextField(12);
                JComboBox<Integer> daysComboBox = new JComboBox<>(days);
                JComboBox<String> monthsComboBox = new JComboBox<>(months);
                JTextField year = new JTextField(3);
                JButton addButton = new JButton("add date");
                addButton.setForeground(FONT_COLOR);

                JPanel panel = new JPanel();
                panel.add(typesComboBox);
                panel.add(description);
                panel.add(daysComboBox);
                panel.add(monthsComboBox);
                panel.add(year);
                panel.add(addButton);

                addButton.addActionListener(e -> {
                    if (description.getText().length() > 0) {
                        String type = String.valueOf(typesComboBox.getSelectedItem());
                        String descr = description.getText();
                        int dayToDatabase = (int) daysComboBox.getSelectedItem();
                        int monthToDatabase = monthsComboBox.getSelectedIndex() + 1;

                        int yearToDatabase = -1;
                        if (year.getText().length() != 0) {
                            yearToDatabase = Integer.parseInt(year.getText());
                        }

                        if (Common.isDateValid(monthToDatabase, dayToDatabase)) {
                            saveDialogPosition(nameXY);
                            jdbcQueries.addDate(type, descr, dayToDatabase, monthToDatabase, yearToDatabase);
                            this.setVisible(false);
                            new Dialogs("dialog_dates");
                            setDialogPosition(nameXY);
                        }
                    } else {
                        Common.console("enter a description for the date");
                    }
                });

                container.add(scrollPane);
                container.add(panel, "South");

                showDialogs("dates");
                break;
            }
            case "dialog_feelings": {
                this.setResizable(false);
                this.setBounds(600, 200, 530, 338);
                this.setTitle("Feelings");
                this.setLocationRelativeTo(Gui.mainTableScrollPane);
                Object[] columns = {"Like", "Feel", "Wt", "Order", "User", "", "Del"};
                model = new DefaultTableModel(new Object[][]{
                }, columns) {
                    final boolean[] columnEditable = new boolean[]{false, true, true, true, false, true, true};

                    public boolean isCellEditable(int row, int column) {
                        return columnEditable[column];
                    }

                    // Сортировка
                    final Class[] types_unique = {String.class, String.class, Integer.class, Integer.class,
                            String.class, Boolean.class, Button.class};

                    @Override
                    public Class getColumnClass(int columnIndex) {
                        return this.types_unique[columnIndex];
                    }
                };
                table = new JTable(model);
                table.getColumnModel().getColumn(5).setCellEditor(new CheckBoxEditor(new JCheckBox()));
                table.getColumn("Del").setCellRenderer(new ButtonColumn(table, 6));
                table.setAutoCreateRowSorter(true);
                table.getTableHeader().setReorderingAllowed(false);
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(JLabel.CENTER);
                table.setRowHeight(20);
                table.setFont(new Font("SansSerif", Font.PLAIN, 13));
                JTableHeader header = table.getTableHeader();
                header.setFont(new Font("Tahoma", Font.BOLD, 13));
                table.getColumnModel().getColumn(0).setMinWidth(220);
                table.getColumnModel().getColumn(0).setMaxWidth(220);
                table.getColumnModel().getColumn(1).setPreferredWidth(50);
                table.getColumnModel().getColumn(1).setMinWidth(50);
                table.getColumnModel().getColumn(1).setCellRenderer(renderer);
                table.getColumnModel().getColumn(2).setPreferredWidth(50);
                table.getColumnModel().getColumn(2).setMinWidth(50);
                table.getColumnModel().getColumn(2).setCellRenderer(renderer);
                table.getColumnModel().getColumn(3).setPreferredWidth(50);
                table.getColumnModel().getColumn(3).setMinWidth(50);
                table.getColumnModel().getColumn(3).setCellRenderer(renderer);
                table.getColumnModel().getColumn(4).setPreferredWidth(100);
                table.getColumnModel().getColumn(4).setMaxWidth(100);
                table.getColumnModel().getColumn(4).setCellRenderer(renderer);
                table.getColumnModel().getColumn(5).setMaxWidth(30);
                table.getColumnModel().getColumn(6).setMaxWidth(30);
                table.setColumnSelectionAllowed(true);
                table.setCellSelectionEnabled(true);
                table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

                int likeColumnNum = 0;
                int feelColumnNum = 1;
                int weightColumnNum = 2;
                int callOrderColumnNum = 3;

                // отношение к новости: позитив/негатив
                JComboBox<String> feeling = new JComboBox<>(new String[]{"+", "-"});
                feeling.addActionListener(e -> {
                    try {
                        int row = table.getSelectedRow();
                        int col = table.getSelectedColumn();
                        if (col == feelColumnNum && row != -1) {
                            if (table.getValueAt(row, col) != null) {
                                String like = table.getValueAt(row, likeColumnNum).toString();
                                String value = table.getValueAt(row, feelColumnNum).toString();

                                jdbcQueries.updateRules(like, value, "feel");
                            }
                        }
                    } catch (Exception exception) {
                        Common.showAlert(exception.getMessage());
                    }
                });
                table.getColumnModel().getColumn(feelColumnNum).setCellEditor(new DefaultCellEditor(feeling));
                table.getColumnModel().getColumn(feelColumnNum).setCellRenderer(renderer);
                table.getColumnModel().getColumn(feelColumnNum).setPreferredWidth(40);
                table.getColumnModel().getColumn(feelColumnNum).setMaxWidth(40);

                // Вес новости
                JComboBox<Integer> weight = new JComboBox<>(new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
                weight.addActionListener(e -> {
                    try {
                        int row = table.getSelectedRow();
                        int col = table.getSelectedColumn();
                        if (col == weightColumnNum && row != -1) {
                            if (table.getValueAt(row, col) != null) {
                                String like = table.getValueAt(row, likeColumnNum).toString();
                                String value = table.getValueAt(row, weightColumnNum).toString();

                                jdbcQueries.updateRules(like, value, "wt");
                            }
                        }
                    } catch (Exception exc) {
                        Common.showAlert(exc.getMessage());
                    }
                });
                table.getColumnModel().getColumn(weightColumnNum).setCellEditor(new DefaultCellEditor(weight));
                table.getColumnModel().getColumn(weightColumnNum).setCellRenderer(renderer);
                table.getColumnModel().getColumn(weightColumnNum).setPreferredWidth(40);
                table.getColumnModel().getColumn(weightColumnNum).setMaxWidth(40);

                // Call order
                JComboBox<Integer> callOrders = new JComboBox<>(new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
                callOrders.addActionListener(e -> {
                    try {
                        int row = table.getSelectedRow();
                        int col = table.getSelectedColumn();
                        if (col == callOrderColumnNum && row != -1) {
                            if (table.getValueAt(row, col) != null) {
                                String like = table.getValueAt(row, likeColumnNum).toString();
                                String value = table.getValueAt(row, callOrderColumnNum).toString();

                                jdbcQueries.updateRules(like, value, "call-order");
                            }
                        }
                    } catch (Exception exc) {
                        Common.showAlert(exc.getMessage());
                    }
                });
                table.getColumnModel().getColumn(callOrderColumnNum).setCellEditor(new DefaultCellEditor(callOrders));
                table.getColumnModel().getColumn(callOrderColumnNum).setCellRenderer(renderer);
                table.getColumnModel().getColumn(callOrderColumnNum).setPreferredWidth(40);
                table.getColumnModel().getColumn(callOrderColumnNum).setMaxWidth(40);

                scrollPane.setBounds(10, 27, 324, 233);
                scrollPane.setViewportView(table);

                // Панель добавления даты
                JTextField like = new JTextField(14);
                JButton addButton = new JButton("add");
                addButton.setForeground(FONT_COLOR);

                JComboBox<String> addRuleFeelingCombobox = new JComboBox<>(new String[]{"+", "-"});
                JComboBox<Integer> addRuleWtCombobox = new JComboBox<>(new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

                JPanel panel = new JPanel();
                panel.add(new JLabel("rule: "));
                panel.add(like);
                panel.add(new JLabel("feel: "));
                panel.add(addRuleFeelingCombobox);
                panel.add(new JLabel("weight: "));
                panel.add(addRuleWtCombobox);
                panel.add(addButton);

                addButton.addActionListener(e -> {
                    if (like.getText().length() > 0) {
                        String likeText = like.getText();
                        String feel = String.valueOf(addRuleFeelingCombobox.getSelectedItem());
                        Integer weightValue = Integer.parseInt(String.valueOf(addRuleWtCombobox.getSelectedItem()));

                        saveDialogPosition(nameXY);
                        jdbcQueries.addAutoFeelingRule(likeText, feel, weightValue);
                        this.setVisible(false);
                        new Dialogs("dialog_feelings");
                        setDialogPosition(nameXY);
                    } else {
                        Common.console("enter a description");
                    }
                });

                container.add(scrollPane);
                container.add(panel, "South");

                showDialogs("feelings");
                break;
            }
            case "dialog_read_only": {
                this.setBounds(-10, 0, guiSettings.get("width"), guiSettings.get("height") - 12);
                this.setTitle("Read mode");
                Object[] columns = {"Headlines", "Link", "Source"};
                model = new DefaultTableModel(new Object[][]{
                }, columns) {
                    final boolean[] columnEditable = new boolean[]{false, false, false};

                    public boolean isCellEditable(int row, int column) {
                        return columnEditable[column];
                    }

                    // Сортировка
                    final Class[] types_unique = {String.class, String.class, String.class};

                    @Override
                    public Class getColumnClass(int columnIndex) {
                        return this.types_unique[columnIndex];
                    }
                };
                table = new JTable(model) {
                    // Альтернативный цвет для строки таблицы
                    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                        Component component = super.prepareRenderer(renderer, row, column);

                        if (!component.getBackground().equals(getSelectionBackground())) {
                            Color color = (row % 2 == 0 ? Common.guiColors.get("tablesColor") :
                                    Common.guiColors.get("tablesAltColor"));
                            component.setBackground(color);
                        }
                        return component;
                    }
                };
                table.setAutoCreateRowSorter(true);
                table.getTableHeader().setReorderingAllowed(false);
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(JLabel.CENTER);
                table.setRowHeight(35);
                //table.removeColumn(table.getColumnModel().getColumn(2)); // Скрыть источник
                table.removeColumn(table.getColumnModel().getColumn(1)); // Скрыть ссылку
                table.getColumnModel().getColumn(1).setPreferredWidth(140);
                table.getColumnModel().getColumn(1).setMaxWidth(140);
                table.setFont(new Font("SansSerif", Font.PLAIN, 20));
                JTableHeader header = table.getTableHeader();
                header.setFont(new Font("Tahoma", Font.BOLD, 13));
                header.setForeground(new Color(241, 217, 84));
                scrollPane.setViewportView(table);

                // открыть новость в браузере по дабл клику на заголовке
                table.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            int row = table.convertRowIndexToModel(table.rowAtPoint(new Point(e.getX(), e.getY()))); // при сортировке строк оставляет верные данные
                            String url = (String) table.getModel().getValueAt(row, 1);
                            Gui.openPage(url);
                        }
                    }
                });

                // скрываем основной интерфейс
                Common.gui.setVisible(false);

                // новая панель
                JPanel panel = new JPanel();
                container.add(scrollPane);
                container.add(panel, "South");

                // Закрытие приложения при закрытии окна
                this.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        Common.gui.setVisible(true);
                    }
                });

                showRightClickMenu();

                showDialogs("read-only");
                break;
            }
        }

        setDialogPosition(nameXY);

        // делаем фокус на окно, чтобы работал захват клавиш
        this.requestFocusInWindow();

        // сохранение положения диалоговых окон в настройках персонализации
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                saveDialogPosition(nameXY);
            }
        });

        dialogName = nameXY;
    }

    private void showRightClickMenu() {
        final JPopupMenu popup = new JPopupMenu();

        // Show describe (menu)
        JMenuItem menuDescribe = new JMenuItem("Describe", Icons.SETTINGS_DESCRIBE_ICON);
        menuDescribe.addActionListener((e) -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String title = (String) table.getValueAt(row, 0);
                String source = (String) table.getValueAt(row, 1);

                JOptionPane.showMessageDialog(this,
                        jdbcQueries.getLinkOrDescribeByHash(source, title, "describe"));
            }
        });
        popup.add(menuDescribe);

        // Add to favorites (menu)
        JMenuItem menuFavorite = new JMenuItem("To favorites", Icons.WHEN_OK);
        menuFavorite.addActionListener((e) -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String title = (String) table.getValueAt(row, 0);
                String source = (String) table.getValueAt(row, 1);
                jdbcQueries.addFavoriteTitle(title, jdbcQueries.getLinkOrDescribeByHash(source, title, "link"));
            }
        });
        popup.add(menuFavorite);

        // Export titles to excel file (menu)
        JMenuItem exportToXls = new JMenuItem("Export", Icons.EXPORT_XLS_ICON);
        exportToXls.addActionListener((e) -> {
            if (model.getRowCount() != 0) {
                new Thread(new ExportToExcel()::exportMainTableToExcel).start();
            } else {
                Common.console("there is no data to export");
            }
        });
        popup.add(exportToXls);

        // Export titles to csv file (menu)
        JMenuItem exportToCsv = new JMenuItem("Export CSV", Icons.EXPORT_CSV_ICON);
        exportToCsv.addActionListener((e) -> {
            if (model.getRowCount() != 0) {
                new Thread(new ExportToCsv()::export).start();
            } else {
                Common.console("there is no data to export");
            }
        });
        popup.add(exportToCsv);

        // Delete row (menu)
        JMenuItem menuDeleteRow = new JMenuItem("Remove", Icons.EXIT_BUTTON_ICON);
        menuDeleteRow.addActionListener(e -> {
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            if (row != -1) model.removeRow(row);
        });
        popup.add(menuDeleteRow);

        // Mouse right click listener
        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    int row = table.convertRowIndexToModel(table.rowAtPoint(e.getPoint()));
                    table.setRowSelectionInterval(row, row);
                    int column = table.columnAtPoint(e.getPoint());
                    if (table.isRowSelected(row)) {
                        table.changeSelection(row, column, false, false);
                    }
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    // Заполнение диалоговых окон данными
    private void showDialogs(String name) {
        int id = 0;
        switch (name) {
            case "smi": {
                List<Source> sources = jdbcQueries.getSources("all");
                for (Source s : sources) {
                    Dialogs.model.addRow(new Object[]{++id, s.getCountry(), s.getSource(), s.getLink(),
                            s.getPosition(), s.getIsActive()});
                }
                break;
            }
            case "excl": {
                List<Excluded> excludes = jdbcQueries.getExcludedWords("top-ten");

                for (Excluded excluded : excludes) {
                    Object[] row = new Object[]{++id, excluded.getWord()};
                    Dialogs.model.addRow(row);
                }
                break;
            }
            case "title-excl": {
                List<Excluded> excludes = jdbcQueries.getExcludedWords("headline");

                for (Excluded excluded : excludes) {
                    Object[] row = new Object[]{++id, excluded.getWord()};
                    Dialogs.model.addRow(row);
                }
                break;
            }
            case "keywords": {
                List<Keyword> keywords = jdbcQueries.getKeywords(2);
                for (Keyword keyword : keywords) {
                    Dialogs.model.addRow(new Object[]{++id, keyword.getWord(), keyword.getIsActive()});
                }
                break;
            }
            case "favorites": {
                List<Favorite> favorites = jdbcQueries.getFavorites();
                for (Favorite favorite : favorites) {
                    Dialogs.model.addRow(new Object[]{++id, favorite.getTitle(),
                            favorite.getAddDate(), favorite.getLink()});
                }
                break;
            }
            case "dates": {
                List<Dates> dates = jdbcQueries.getDates(1);
                Integer year;

                for (Dates date : dates) {
                    year = date.getYear();
                    if (year == -1) year = null;

                    Dialogs.model.addRow(new Object[]{date.getType(), date.getDescription(), date.getDay(),
                            date.getMonth(), year, date.getIsActive()});
                }
                break;
            }
            case "feelings": {
                List<Feelings> feelings = jdbcQueries.getFeelings();
                for (Feelings feel : feelings) {
                    Integer order = feel.getCallOrder();
                    if (order == 0) order = null;

                    Dialogs.model.addRow(new Object[]{feel.getLike(), feel.getFeeling(), feel.getWeight(),
                            order, feel.getUsername(), feel.getIsActive()});
                }
                break;
            }
            case "read-only": {
                for (Headline headline : Search.getDataFromMainTable()) {
                    Dialogs.model.addRow(new Object[]{headline.getTitle(), headline.getLink(), headline.getSource()});
                }
                break;
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    // Закрываем диалоговые окна клавишей ESC
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            jdbcQueries.updateSettings(dialogName, getX() + "," + getY());
            setVisible(false);
        }
    }

    private void saveDialogPosition(String name) {
        jdbcQueries.updateSettings(name, getX() + "," + getY());
    }

    private void setDialogPosition(String name) {
        String[] dialogXY = jdbcQueries.getSetting(name).split(",");
        if (!dialogXY[0].equals("-1")) {
            setLocation(Integer.parseInt(dialogXY[0]), Integer.parseInt(dialogXY[1]));
        }
    }

    private int getColumnIndex(String name) {
        return table.getColumnModel().getColumnIndex(name);
    }
}