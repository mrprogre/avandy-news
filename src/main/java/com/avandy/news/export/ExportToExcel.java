package com.avandy.news.export;

import com.avandy.news.model.Headline;
import com.avandy.news.model.TopTenRow;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import com.avandy.news.search.Search;
import com.avandy.news.utils.Common;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

public class ExportToExcel {
    private final List<Headline> newsList = Search.getDataFromMainTable();
    private static final String[] HEADERS = {"Num", "Title", "Source", "Feel", "Weight", "Date", "Link"};
    private static final String[] TOP_TEN_HEADERS = {"Word", "Frequency"};
    private static final String SHEET_FONT = "Arial";
    private static final short SHEET_FONT_SIZE = (short) 13;
    private static final short SHEET_ROWS_HEIGHT = (short) 400;
    private final Workbook workbook = new HSSFWorkbook();
    private final Sheet sheet = workbook.createSheet("Avandy-news");

    public void exportMainTableToExcel() {
        try {
            JFileChooser saveToDirectory = saveFileToDirectory();
            int ret = saveToDirectory.showDialog(null, "Save");
            if (ret == JFileChooser.APPROVE_OPTION) {
                File file = new File(saveToDirectory.getSelectedFile() + ".xls");

                sheet.createFreezePane(0, 1);
                sheet.setColumnWidth(0, 3000);
                sheet.setColumnWidth(1, 30000);
                sheet.setColumnWidth(2, 4000);
                sheet.setColumnWidth(3, 2000);
                sheet.setColumnWidth(4, 3000);
                sheet.setColumnWidth(5, 5000);
                sheet.setColumnWidth(6, 2000);

                // Headers
                Row headerRow = sheet.createRow(0);
                headerRow.setHeight(SHEET_ROWS_HEIGHT);

                CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setAlignment(HorizontalAlignment.CENTER);
                headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                headerStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);
                headerStyle.setBorderBottom(BorderStyle.THIN);

                Font headersFont = workbook.createFont();
                headersFont.setFontName(SHEET_FONT);
                headersFont.setFontHeightInPoints(SHEET_FONT_SIZE);
                headersFont.setBold(true);
                headerStyle.setFont(headersFont);

                // Cells
                CellStyle cellStyle = workbook.createCellStyle();
                cellStyle.setBorderLeft(BorderStyle.THIN);
                cellStyle.setBorderRight(BorderStyle.THIN);
                cellStyle.setBorderBottom(BorderStyle.THIN);
                cellStyle.setAlignment(HorizontalAlignment.CENTER);
                cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

                // Sources, Titles
                CellStyle leftCellStyle = workbook.createCellStyle();
                leftCellStyle.setBorderLeft(BorderStyle.THIN);
                leftCellStyle.setBorderRight(BorderStyle.THIN);
                leftCellStyle.setBorderBottom(BorderStyle.THIN);
                leftCellStyle.setAlignment(HorizontalAlignment.LEFT);
                leftCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

                Font cellFont = workbook.createFont();
                cellFont.setFontName(SHEET_FONT);
                cellFont.setFontHeightInPoints(SHEET_FONT_SIZE);

                cellStyle.setFont(cellFont);
                leftCellStyle.setFont(cellFont);

                for (int i = 0; i < HEADERS.length; i++) {
                    Cell header = headerRow.createCell(i);
                    header.setCellValue(HEADERS[i]);
                    header.setCellStyle(headerStyle);
                }

                for (int i = 0; i < newsList.size(); i++) {
                    int rowNum = i + 1;
                    // ограничение выгрузки == 65535
                    if (rowNum > 10000) {
                        Common.showAlert("Export to xls is limited to 10,000 lines. " +
                                "For downloading more than 10,000 rows, use the export in CSV format");
                        break;
                    }
                    Row row = sheet.createRow(rowNum);
                    row.setHeight(SHEET_ROWS_HEIGHT);

                    // "Num"
                    Cell number = row.createCell(0);
                    number.setCellValue(i + 1);
                    number.setCellStyle(cellStyle);

                    // "Title"
                    Cell title = row.createCell(1);
                    title.setCellValue(newsList.get(i).getTitle());
                    title.setCellStyle(leftCellStyle);

                    // "Source"
                    Cell source = row.createCell(2);
                    source.setCellValue(newsList.get(i).getSource());
                    source.setCellStyle(leftCellStyle);

                    // "Feel"
                    Cell feel = row.createCell(3);
                    String feeling = newsList.get(i).getFeel();
                    if (feeling != null) {
                        feel.setCellValue(feeling);
                    } else {
                        feel.setCellValue("");
                    }
                    feel.setCellStyle(cellStyle);

                    // "Weight"
                    Cell weightCell = row.createCell(4);
                    Integer weight = newsList.get(i).getWeight();
                    if (weight != null) {
                        weightCell.setCellValue(weight);
                    } else {
                        weightCell.setCellValue("");
                    }
                    weightCell.setCellStyle(cellStyle);

                    // "Date"
                    Cell date = row.createCell(5);
                    date.setCellValue(newsList.get(i).getNewsDate());
                    date.setCellStyle(cellStyle);

                    // "Link"
                    Hyperlink hyperlink = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                    hyperlink.setAddress(newsList.get(i).getLink());
                    Cell link = row.createCell(6);
                    link.setCellValue("⟶");
                    link.setHyperlink(hyperlink);
                    link.setCellStyle(cellStyle);
                }

                // write to file
                OutputStream outputStream = Files.newOutputStream(file.toPath());
                workbook.write(outputStream);
                workbook.close();
                outputStream.close();
                Common.console("export is done");
            }
        } catch (IOException e) {
            Common.console("export error");
        }
    }

    public void exportTopTenTableToExcel() {
        try {
            JFileChooser saveToDirectory = saveFileToDirectory();
            int ret = saveToDirectory.showDialog(null, "Save");
            if (ret == JFileChooser.APPROVE_OPTION) {
                File file = new File(saveToDirectory.getSelectedFile() + ".xls");

                sheet.createFreezePane(0, 1);
                sheet.setColumnWidth(0, 6000);
                sheet.setColumnWidth(1, 4000);

                // Headers
                Row headerRow = sheet.createRow(0);
                headerRow.setHeight(SHEET_ROWS_HEIGHT);

                CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setAlignment(HorizontalAlignment.CENTER);
                headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                headerStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);
                headerStyle.setBorderBottom(BorderStyle.THIN);

                Font headersFont = workbook.createFont();
                headersFont.setFontName(SHEET_FONT);
                headersFont.setFontHeightInPoints(SHEET_FONT_SIZE);
                headersFont.setBold(true);
                headerStyle.setFont(headersFont);

                // Cells
                CellStyle cellStyle = workbook.createCellStyle();
                cellStyle.setBorderLeft(BorderStyle.THIN);
                cellStyle.setBorderRight(BorderStyle.THIN);
                cellStyle.setBorderBottom(BorderStyle.THIN);
                cellStyle.setAlignment(HorizontalAlignment.CENTER);
                cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

                CellStyle leftCellStyle = workbook.createCellStyle();
                leftCellStyle.setBorderLeft(BorderStyle.THIN);
                leftCellStyle.setBorderRight(BorderStyle.THIN);
                leftCellStyle.setBorderBottom(BorderStyle.THIN);
                leftCellStyle.setAlignment(HorizontalAlignment.LEFT);
                leftCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

                Font cellFont = workbook.createFont();
                cellFont.setFontName(SHEET_FONT);
                cellFont.setFontHeightInPoints(SHEET_FONT_SIZE);

                cellStyle.setFont(cellFont);
                leftCellStyle.setFont(cellFont);

                for (int i = 0; i < TOP_TEN_HEADERS.length; i++) {
                    Cell header = headerRow.createCell(i);
                    header.setCellValue(TOP_TEN_HEADERS[i]);
                    header.setCellStyle(headerStyle);
                }

                List<TopTenRow> topTenList = Search.getDataFromTopTenTable();
                if (topTenList.size() > 65535)
                    topTenList = topTenList.subList(0, 65534);

                for (int i = 0; i < topTenList.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    row.setHeight(SHEET_ROWS_HEIGHT);

                    // "word"
                    Cell word = row.createCell(0);
                    word.setCellValue(topTenList.get(i).getWord());
                    word.setCellStyle(leftCellStyle);

                    // "frequency"
                    Cell frequency = row.createCell(1);
                    frequency.setCellValue(topTenList.get(i).getFrequency());
                    frequency.setCellStyle(cellStyle);
                }

                // write to file
                OutputStream outputStream = Files.newOutputStream(file.toPath());
                workbook.write(outputStream);
                workbook.close();
                outputStream.close();
                Common.console("top ten export is done");
            }
        } catch (IOException e) {
            Common.console("top ten export error");
        }
    }

    private static JFileChooser saveFileToDirectory() {
        FileNameExtensionFilter filter = new FileNameExtensionFilter("*.xls",
                "*.xls", "*.XLS", "*.*");
        JFileChooser saveToDirectory = new JFileChooser();
        saveToDirectory.setFileFilter(filter);
        saveToDirectory.setCurrentDirectory(new File
                (System.getProperty("user.home") + System.getProperty("file.separator") + "Desktop"));
        return saveToDirectory;
    }
}