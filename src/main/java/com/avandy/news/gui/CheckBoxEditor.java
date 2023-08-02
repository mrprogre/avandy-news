package com.avandy.news.gui;

import com.avandy.news.database.JdbcQueries;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class CheckBoxEditor extends DefaultCellEditor implements ItemListener {
    private final JCheckBox checkBox;
    private int row;

    public CheckBoxEditor(JCheckBox checkBox) {
        super(checkBox);
        this.checkBox = checkBox;
        this.checkBox.addItemListener(this);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.row = row;
        checkBox.setSelected((Boolean) value);
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    public void itemStateChanged(ItemEvent e) {
        JdbcQueries jdbcQueries = new JdbcQueries();
        this.fireEditingStopped();
        String columnName = Dialogs.model.getColumnName(1);
        String itemInSecondColumn = (String) Dialogs.table.getValueAt(row, 1);
        String itemInThirdColumn = (String) Dialogs.table.getValueAt(row, 2);

        switch (columnName) {
            case "Country":
                jdbcQueries.updateIsActiveCountry(checkBox.isSelected(), itemInSecondColumn, itemInThirdColumn);
                break;
            case "Keyword":
                jdbcQueries.updateIsActiveCheckboxes(checkBox.isSelected(), itemInSecondColumn, "keywords");
                break;
            case "Description":
                String itemInFirstColumn = (String) Dialogs.model.getValueAt(row, 0);
                jdbcQueries.updateIsActiveDates(checkBox.isSelected(), itemInFirstColumn, itemInSecondColumn);
                break;
            case "Feel":
                String like = (String) Dialogs.model.getValueAt(row, 0);
                jdbcQueries.updateIsActiveCheckboxes(checkBox.isSelected(), like, "feel");
                break;
        }
    }

}