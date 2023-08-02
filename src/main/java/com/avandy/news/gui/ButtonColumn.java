package com.avandy.news.gui;

import com.avandy.news.database.JdbcQueries;

import javax.swing.FocusManager;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {
    final JTable table;
    final JButton renderButton;
    final JButton editButton;

    public ButtonColumn(JTable table, int column) {
        super();
        this.table = table;
        renderButton = new JButton();

        editButton = new JButton();
        editButton.setFocusPainted(false);
        editButton.addActionListener(this);

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(column).setCellRenderer(this);
        columnModel.getColumn(column).setCellEditor(this);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (hasFocus) {
            renderButton.setForeground(table.getForeground());
            renderButton.setBackground(UIManager.getColor("Button.background"));
            renderButton.setIcon(Icons.DELETE_UNIT);
        } else if (isSelected) {
            renderButton.setForeground(table.getSelectionForeground());
            renderButton.setIcon(Icons.DELETE_UNIT);
        } else {
            renderButton.setForeground(table.getForeground());
            renderButton.setBackground(UIManager.getColor("Button.background"));
            renderButton.setIcon(Icons.DELETE_UNIT);
        }
        return renderButton;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return editButton;
    }

    public Object getCellEditorValue() {
        try {
            return ButtonColumn.class.getMethod("getCellEditorValue");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public void actionPerformed(ActionEvent e) {
        JdbcQueries jdbcQueries = new JdbcQueries();
        fireEditingStopped();
        int rowWithSource = table.getSelectedRow();
        int rowWithExcludeWord = Gui.topTenTable.getSelectedRow();
        int delRowWithExcludeWord = 0;
        int delRowWithExcludeTitlesWord = 0;
        int delKeyword = 0;
        int delFavorite = 0;
        int delDate = 0;
        int delRule = 0;

        // определяем активное окно
        Window window = FocusManager.getCurrentManager().getActiveWindow();
        int activeWindow = 0;
        if (window.toString().contains("Avandy")) {
            activeWindow = 1;
        } else if (window.toString().contains("Sources")) {
            activeWindow = 2;
        } else if (window.toString().contains("analysis")) {
            activeWindow = 3;
            delRowWithExcludeWord = Dialogs.table.getSelectedRow();
        } else if (window.toString().contains("headlines")) {
            activeWindow = 4;
            delRowWithExcludeTitlesWord = Dialogs.table.getSelectedRow();
        } else if (window.toString().contains("Keywords")) {
            activeWindow = 5;
            delKeyword = Dialogs.table.getSelectedRow();
        } else if (window.toString().contains("Favorites")) {
            activeWindow = 6;
            delFavorite = Dialogs.table.getSelectedRow();
        } else if (window.toString().contains("Dates")) {
            activeWindow = 7;
            delDate = Dialogs.table.getSelectedRow();
        }else if (window.toString().contains("Feelings")) {
            activeWindow = 8;
            delRule = Dialogs.table.getSelectedRow();
        }

        // окно таблицы с анализом частоты слов на основной панели (добавляем в базу)
        if (activeWindow == 1 && rowWithExcludeWord != -1) {
            rowWithExcludeWord = Gui.topTenTable.convertRowIndexToModel(rowWithExcludeWord);
            String source = (String) Gui.modelTopTen.getValueAt(rowWithExcludeWord, 0);
            // удаление из диалогового окна
            if (!source.contains("- -")) {
                Gui.modelTopTen.removeRow(rowWithExcludeWord);
                // добавление в базу данных
                jdbcQueries.addExcludedWord(source);
            }
        }

        // окно источников RSS
        if (activeWindow == 2 && rowWithSource != -1) {
            rowWithSource = table.convertRowIndexToModel(rowWithSource);

            String country = (String) Dialogs.model.getValueAt(rowWithSource, 1);
            String source = (String) Dialogs.model.getValueAt(rowWithSource, 2);

            // удаление из диалогового окна
            Dialogs.model.removeRow(rowWithSource);
            jdbcQueries.removeFromRssList(country, source);
        }

        // окно с исключенными из "Топ 10" слов (удаляем из базы)
        if (activeWindow == 3 && delRowWithExcludeWord != -1) {
            delRowWithExcludeWord = Dialogs.table.convertRowIndexToModel(delRowWithExcludeWord);
            String source = (String) Dialogs.model.getValueAt(delRowWithExcludeWord, 1);
            // удаление из диалогового окна
            Dialogs.model.removeRow(delRowWithExcludeWord);
            jdbcQueries.removeItem(source, 3);
        }

        // окно с исключенными из поиска слов (удаляем из базы)
        if (activeWindow == 4 && delRowWithExcludeTitlesWord != -1) {
            delRowWithExcludeTitlesWord = Dialogs.table.convertRowIndexToModel(delRowWithExcludeTitlesWord);
            String word = (String) Dialogs.model.getValueAt(delRowWithExcludeTitlesWord, 1);
            // удаление из диалогового окна
            Dialogs.model.removeRow(delRowWithExcludeTitlesWord);
            jdbcQueries.removeItem(word, 4);
        }

        // окно с ключевыми словами (удаляем из базы)
        if (activeWindow == 5 && delKeyword != -1) {
            delKeyword = Dialogs.table.convertRowIndexToModel(delKeyword);
            String word = (String) Dialogs.model.getValueAt(delKeyword, 1);
            // удаление из диалогового окна
            Dialogs.model.removeRow(delKeyword);
            jdbcQueries.removeItem(word, 5);
        }

        // окно с избранными заголовками (удаляем из базы)
        if (activeWindow == 6 && delFavorite != -1) {
            delFavorite = Dialogs.table.convertRowIndexToModel(delFavorite);
            String title = (String) Dialogs.model.getValueAt(delFavorite, 1);
            // удаление из диалогового окна
            Dialogs.model.removeRow(delFavorite);
            jdbcQueries.removeItem(title, 6);
        }

        // окно с датами (удаляем из базы)
        if (activeWindow == 7 && delDate != -1) {
            delDate = Dialogs.table.convertRowIndexToModel(delDate);
            String type = (String) Dialogs.model.getValueAt(delDate, 0);
            String description = (String) Dialogs.model.getValueAt(delDate, 1);
            // удаление из диалогового окна
            Dialogs.model.removeRow(delDate);
            jdbcQueries.removeItem(type + " " + description, 7);
        }

        // окно с правилами для ощущений (удаляем из базы)
        if (activeWindow == 8 && delRule != -1) {
            delRule = Dialogs.table.convertRowIndexToModel(delRule);
            String rule = (String) Dialogs.model.getValueAt(delRule, 0);
            // удаление из диалогового окна
            Dialogs.model.removeRow(delRule);
            jdbcQueries.removeItem(rule, 8);
        }
    }

}