package com.avandy.news.utils;

import com.avandy.news.database.JdbcQueries;
import com.avandy.news.gui.Gui;
import com.avandy.news.gui.TextLang;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;

public class ImportFromCsv {
    private final JdbcQueries jdbcQueries = new JdbcQueries();
    private volatile Thread importThread = null;
    private volatile boolean isCancelled = false;

    /**
     * Возвращает путь к рабочему столу пользователя в зависимости от ОС
     */
    private File getDesktopDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        String desktopPath;

        if (os.contains("win")) {
            // Windows
            desktopPath = System.getProperty("user.home") + File.separator + "Documents";
        } else if (os.contains("mac")) {
            // MacOS
            desktopPath = System.getProperty("user.home") + File.separator + "Desktop";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            // Linux - проверяем разные варианты
            String possibleDesktop = System.getProperty("user.home") + File.separator + "Desktop";
            File desktop = new File(possibleDesktop);
            if (!desktop.exists()) {
                // Для русской локали
                possibleDesktop = System.getProperty("user.home") + File.separator + "Рабочий стол";
                desktop = new File(possibleDesktop);
            }
            if (!desktop.exists()) {
                // Если нет Desktop, используем домашнюю директорию
                possibleDesktop = System.getProperty("user.home");
            }
            desktopPath = possibleDesktop;
        } else {
            // fallback для других ОС
            desktopPath = System.getProperty("user.home");
        }

        File desktopDir = new File(desktopPath);

        // Если директория не существует, используем домашнюю
        if (!desktopDir.exists()) {
            desktopDir = new File(System.getProperty("user.home"));
        }

        return desktopDir;
    }

    public void importCsvFile(JFrame parentFrame) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select CSV file to import");

        // Устанавливаем рабочий стол как начальную директорию (с проверкой на разные ОС)
        File desktopDir = getDesktopDirectory();
        fileChooser.setCurrentDirectory(desktopDir);

        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files (*.csv)", "csv"));

        int result = fileChooser.showOpenDialog(parentFrame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            // Запрашиваем подтверждение
            int confirm = JOptionPane.showConfirmDialog(parentFrame,
                    "Import headlines from " + selectedFile.getName() +
                            "\n\n" +
                            "Format:" +
                            "\n" +
                            "title1,yyyy-mm-dd\n" +
                            "title2,yyyy-mm-dd\n\n" +
                            "Continue?",
                    "Confirm Import",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                isCancelled = false;
                importThread = new Thread(() -> importCsv(selectedFile, parentFrame));
                importThread.start();
            }
        }
    }

    private void importCsv(File file, JFrame parentFrame) {
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger duplicateCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();
        int totalLines = 0;

        // Сначала подсчитываем количество строк в файле
        try (BufferedReader countReader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = countReader.readLine()) != null) {
                // Пропускаем пустые строки и заголовок
                if (!line.trim().isEmpty() && !line.toLowerCase().contains("title") && !line.toLowerCase().contains("pub_date")) {
                    totalLines++;
                }
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> Common.showAlert("Error reading file: " + e.getMessage()));
            return;
        }

        // Прогресс бар
        JProgressBar progressBar = new JProgressBar(0, totalLines);
        progressBar.setStringPainted(true);
        progressBar.setString("Importing..");

        JDialog progressDialog = new JDialog(parentFrame, "Importing CSV", false); // Изменено на false
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progressDialog.setLayout(new BorderLayout());
        progressDialog.add(progressBar, BorderLayout.CENTER);
        progressDialog.setSize(300, 80);
        progressDialog.setLocationRelativeTo(parentFrame);

        // Запускаем импорт в отдельном потоке
        int finalTotalLines = totalLines;

        // Создаем поток для импорта
        Thread currentImportThread = new Thread(() -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                int currentLine = 0;
                String line;

                while ((line = br.readLine()) != null && !isCancelled) {
                    currentLine++;

                    // Пропускаем пустые строки
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    // Пропускаем заголовок если есть
                    if (currentLine == 1 && (line.toLowerCase().contains("title") || line.toLowerCase().contains("pub_date"))) {
                        Common.console("Skipping header: " + line);
                        continue;
                    }

                    final int lineNumber = currentLine;

                    // Обновляем прогресс
                    SwingUtilities.invokeLater(() -> {
                        int processed = successCount.get() + duplicateCount.get() + errorCount.get();
                        progressBar.setValue(Math.min(processed, finalTotalLines));
                        progressBar.setString("Processed: " + processed + " of " + finalTotalLines);
                    });

                    // Разделяем CSV
                    String[] parts = parseCsvLine(line);

                    if (parts.length >= 2) {
                        String title = parts[0].trim();
                        String pubDate = parts[1].trim();

                        // Убираем кавычки если есть
                        if (title.startsWith("\"") && title.endsWith("\"")) {
                            title = title.substring(1, title.length() - 1);
                        }

                        // Проверяем, что дата в правильном формате
                        if (isValidDate(pubDate, dateFormat)) {
                            // Проверяем, существует ли уже такая новость
                            if (!jdbcQueries.isNewsExists(title)) {
                                // Вставляем новость
                                boolean inserted = jdbcQueries.insertNewsFromCsv(
                                        title,
                                        pubDate
                                );

                                if (inserted) {
                                    successCount.getAndIncrement();
                                } else {
                                    errorCount.getAndIncrement();
                                    Common.console("Failed to insert: " + title);
                                }
                            } else {
                                duplicateCount.getAndIncrement();
                            }
                        } else {
                            errorCount.getAndIncrement();
                            Common.console("Invalid date format (expected yyyy-mm-dd): " + pubDate + " in line " + lineNumber);
                        }
                    } else {
                        errorCount.getAndIncrement();
                        Common.console("Invalid CSV format (expected 2 columns: title,yyyy-mm-dd): " + line);
                    }
                }

                // Завершаем импорт
                final int finalSuccess = successCount.get();
                final int finalDuplicate = duplicateCount.get();
                final int finalError = errorCount.get();
                final boolean wasCancelled = isCancelled;

                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();

                    String message;
                    if (wasCancelled) {
                        message = String.format(
                                "Import cancelled!\n\n" +
                                        "Imported: %d\n" +
                                        "Duplicates: %d\n" +
                                        "Errors: %d\n" +
                                        "━━━━━━━━━━━━━━━━━━━━\n" +
                                        "Total processed: %d",
                                finalSuccess, finalDuplicate, finalError, finalSuccess + finalDuplicate + finalError
                        );
                    } else {
                        message = String.format(
                                "Import completed!\n\n" +
                                        "Imported: %d\n" +
                                        "Duplicates: %d\n" +
                                        "Errors: %d\n" +
                                        "━━━━━━━━━━━━━━━━━━━━\n" +
                                        "Total processed: %d",
                                finalSuccess, finalDuplicate, finalError, finalSuccess + finalDuplicate + finalError
                        );
                    }

                    JOptionPane.showMessageDialog(parentFrame,
                            message,
                            wasCancelled ? "Import Cancelled" : "Import Results",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Обновляем интерфейс
                    if (finalSuccess > 0 && !wasCancelled) {
                        Gui.newsInArchiveLabel.setText(
                                TextLang.newsInArchiveLabelText +
                                        jdbcQueries.archiveNewsCount() +
                                        TextLang.newsInArchiveAtLabelText +
                                        jdbcQueries.getSetting("last_update_news")
                        );
                    }
                });

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    Common.showAlert("Error reading file: " + e.getMessage());
                });
            }
        });

        // Обработка закрытия диалога
        progressDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (currentImportThread.isAlive() && !isCancelled) {
                    int confirm = JOptionPane.showConfirmDialog(progressDialog,
                            "Import in progress. Do you want to cancel?",
                            "Cancel Import",
                            JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        isCancelled = true;
                        currentImportThread.interrupt();
                    }
                }
            }
        });

        currentImportThread.start();
        progressDialog.setVisible(true);

        // Дожидаемся завершения потока импорта
        try {
            currentImportThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Убеждаемся, что диалог закрыт
        if (progressDialog.isVisible()) {
            progressDialog.dispose();
        }
    }

    /**
     * Парсит строку CSV с учетом кавычек
     */
    private String[] parseCsvLine(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        result.add(currentField.toString());

        return result.toArray(new String[0]);
    }

    /**
     * Проверяет корректность даты
     */
    private boolean isValidDate(String dateStr, SimpleDateFormat dateFormat) {
        try {
            dateFormat.setLenient(false);
            dateFormat.parse(dateStr);
            return true;
        } catch (java.text.ParseException e) {
            return false;
        }
    }
}