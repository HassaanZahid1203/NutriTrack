package com.nutritrack.app.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static String dayHeader() {
        return new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date());
    }

    public static String greeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "GOOD MORNING 👋";
        if (hour < 18) return "GOOD AFTERNOON 👋";
        return "GOOD EVENING 👋";
    }

    public static long startOfDay() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long endOfDay() {
        return startOfDay() + 24L * 60 * 60 * 1000;
    }
}
