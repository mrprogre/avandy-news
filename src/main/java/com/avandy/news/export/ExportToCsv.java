package com.avandy.news.export;

import com.avandy.news.gui.Gui;
import com.avandy.news.utils.Common;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ExportToCsv {

    public void export() {
        JFileChooser saver = new JFileChooser();
        File file = new File(System.getProperty("user.home") +
                System.getProperty("file.separator") + "Desktop");
        saver.setCurrentDirectory(file);
        int res = saver.showDialog(null, "Save");
        if (res == JFileChooser.APPROVE_OPTION) {
            try {
                FileWriter fileWriter = new FileWriter(saver.getSelectedFile() + ".csv", false);
                StringBuilder titles = new StringBuilder();
                titles.append(String.join(";", getHeadersAsString())).append("\n");
                getDataFromGuiTable(titles);
                //fileWriter.write(titles.toString().replace("null", ""));
                fileWriter.write(titles.toString());
                fileWriter.close();
                Common.console("export is done");
            } catch (IOException e) {
                Common.console("ExportToCsv error: " + e.getMessage());
            }
        }
    }

    private static void getDataFromGuiTable(StringBuilder titles) {
        for (int i = 0; i < Gui.modelMain.getRowCount(); i++) {
            for (int j = 0; j < Gui.modelMain.getColumnCount(); j++) {

                Object value = Gui.modelMain.getValueAt(i, j);
                if (value == null) value = "";
                titles.append(value);

                if (j < Gui.modelMain.getColumnCount() - 1) {
                    titles.append(";");
                    Gui.amountOfNewsLabel.setText("Export row: " + (i + 1));
                }
            }
            titles.append("\n");
        }
    }

    private String getHeadersAsString() {
        int iMax = Gui.MAIN_TABLE_HEADERS.length - 1;

        StringBuilder b = new StringBuilder();
        for (int i = 0; ; i++) {
            b.append(Gui.MAIN_TABLE_HEADERS[i]);
            if (i == iMax)
                return b.toString();
            b.append(";");
        }
    }
}