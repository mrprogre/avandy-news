package com.avandy.news.utils;

import com.avandy.news.database.JdbcQueries;
import com.avandy.news.model.Dates;

import java.time.LocalDate;

import static com.avandy.news.gui.TextLang.*;

public class Reminder {

    public void remind() {
        String [] now = LocalDate.now().toString().split("-");
        int year = Integer.parseInt(now[0]);
        int month = Integer.parseInt(now[1]);
        int day = Integer.parseInt(now[2]);

        String [] tomorrow = LocalDate.now().plusDays(1).toString().split("-");
        int yearTomorrow = Integer.parseInt(tomorrow[0]);
        int monthTomorrow = Integer.parseInt(tomorrow[1]);
        int dayTomorrow = Integer.parseInt(tomorrow[2]);

        for (Dates date : new JdbcQueries().getDates(0)) {
            if (date.getMonth() == month && date.getDay() == day) {
                if ((date.getType().equals("Birthday")  || date.getType().equals("Event")) && date.getYear() > 0) {
                    Common.console(reminderToday + ": " + date.getDescription() + " " + reminderIs + " " +
                            (year - date.getYear()));
                } else {
                    Common.console(reminderToday + ": " + date.getType() + " - " + date.getDescription());
                }
            }

            if (date.getMonth() == monthTomorrow && date.getDay() == dayTomorrow) {
                if ((date.getType().equals("Birthday") || date.getType().equals("Event")) && date.getYear() > 0) {
                    Common.console(reminderTomorrow + ": " + date.getDescription() + " " + reminderWillBe + " " +
                            (yearTomorrow - date.getYear()));
                } else {
                    Common.console(reminderTomorrow + ": " + date.getType() + " - " + date.getDescription());
                }
            }
        }
    }

}
