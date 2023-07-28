package com.avandy.news.database;

import com.avandy.news.utils.Common;
import com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme;

import javax.swing.*;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLite {
    public static Connection connection;

    public void openConnection() {
        String url = "jdbc:sqlite:" + Common.DATABASE_PATH;

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(url);
        } catch (Exception e) {
            FlatHiberbeeDarkIJTheme.setup();
            JOptionPane.showMessageDialog(null, "There is no connection to the database at " +
                    Common.DATABASE_PATH);
            System.exit(0);
        }
    }

    public void closeConnection() {
        try {
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void transaction(String command) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(command);
        statement.execute();
        statement.close();
    }

    public boolean isValidPathToDatabase(String path) {
        return new File(path).exists() && path.endsWith(".db");
    }
}
