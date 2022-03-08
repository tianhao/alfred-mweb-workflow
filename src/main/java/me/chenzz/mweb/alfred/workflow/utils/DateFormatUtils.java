package me.chenzz.mweb.alfred.workflow.utils;

import java.text.SimpleDateFormat;

public class DateFormatUtils {

    public static String format(long currentTimeMillis, String pattern) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String result = simpleDateFormat.format(currentTimeMillis);

        return result;
    }
}
