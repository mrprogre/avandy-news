package com.avandy.news.utils;

import com.avandy.news.database.JdbcQueries;
import com.avandy.news.gui.Icons;

import javax.swing.*;
import java.awt.*;

public class Login {
    public static int userId;
    public static String username;
    public static final int USERNAME_LENGTH_MIN = 2;
    public static final int USERNAME_LENGTH_MAX = 12;
    private final JdbcQueries jdbcQueries = new JdbcQueries();
    private JComboBox<Object> usersCombobox = new JComboBox<>(jdbcQueries.getAllUsers().toArray());

    public void login() {
        String[] loginParams = showLoginDialog();
        int option = Integer.parseInt(loginParams[0]);

        if (option == 0) {
            removeUser();
        } else if (option == 1) {
            createUser();
        } else if (option == 2) {
            enter(loginParams);
        } else {
            System.exit(0);
        }
    }

    private void removeUser() {
        if (usersCombobox.getSelectedItem() != null) {
            String user = usersCombobox.getSelectedItem().toString();

            String[] opt = new String[]{"yes", "no"};
            int action = JOptionPane.showOptionDialog(null, "Do you confirm the deletion of " +
                            user.toUpperCase() + " user?",
                    "Remove user", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    Icons.EXIT_BUTTON_ICON, opt, opt[1]);
            if (action == 0) {
                jdbcQueries.removeFromUsers(user);
                usersCombobox = new JComboBox<>(jdbcQueries.getAllUsers().toArray());
                login();
            } else {
                login();
            }
        } else {
            Common.showAlert("There is no user to delete");
            login();
        }
    }

    public void createUser() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2, 0, 5));
        JLabel userLabel = new JLabel("Username");
        JTextField user = new JTextField();
        JLabel passwordLabel = new JLabel("Password");
        JPasswordField passwordField = new JPasswordField();
        JLabel countryLabel = new JLabel("Country");
        panel.add(userLabel);
        panel.add(user);
        panel.add(passwordLabel);
        panel.add(passwordField);
        panel.add(countryLabel);
        panel.add(Common.countriesCombobox);

        String[] menu = new String[]{"cancel", "add"};
        int action = JOptionPane.showOptionDialog(null, panel, "Add user",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                Icons.LIST_BUTTON_ICON, menu, menu[1]);

        String pwd = Common.getHash(new String(passwordField.getPassword()));
        int length = user.getText().length();
        if (action == 1 && length >= USERNAME_LENGTH_MIN && length <= USERNAME_LENGTH_MAX) {
            boolean added = jdbcQueries.addUser(user.getText(), pwd);
            if (added) {
                usersCombobox = new JComboBox<>(jdbcQueries.getAllUsers().toArray());
                username = user.getText();
                userId = jdbcQueries.getUserIdByUsername(username);
                jdbcQueries.initUser((String) Common.countriesCombobox.getSelectedItem());
            }
        } else if (action == 1 && (user.getText().length() < USERNAME_LENGTH_MIN || user.getText().length() > USERNAME_LENGTH_MAX)) {
            Common.showAlert("The username length between " + USERNAME_LENGTH_MIN + " and " + USERNAME_LENGTH_MAX + " chars");
            createUser();
        } else {
            login();
        }
    }

    public void enter(String[] loginParams) {
        username = loginParams[1];
        userId = jdbcQueries.getUserIdByUsername(username);
        String password = loginParams[2];
        String userHashPassword = jdbcQueries.getUserHashPassword(username);

        // Password check
        if (!password.equals(userHashPassword)) {
            Common.showAlert("Incorrect password");
            login();
        }
    }

    /*
     login[0] = "Remove", "Create", "Enter"
     login[1] = username
     login[2] = password
    */
    private String[] showLoginDialog() {
        String[] options = new String[3];

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2, 5, 5));
        JLabel userLabel = new JLabel("Username");
        JLabel passwordLabel = new JLabel("Password");
        JPasswordField passwordField = new JPasswordField(3);
        panel.add(userLabel);
        panel.add(usersCombobox);
        panel.add(passwordLabel);
        panel.add(passwordField);

        JOptionPane.getRootFrame().setAlwaysOnTop(true);
        String[] menu = new String[]{"Remove", "Create", "Enter"};
        int option = JOptionPane.showOptionDialog(null, panel, "Login",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                Icons.LOGO_ICON, menu, menu[2]); //menu[2] null

        options[0] = String.valueOf(option);
        if (usersCombobox.getSelectedItem() != null) {
            options[1] = usersCombobox.getSelectedItem().toString();
        }
        options[2] = Common.getHash(new String(passwordField.getPassword()));

        return options;
    }

}
