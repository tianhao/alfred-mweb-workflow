package me.chenzz.mweb.alfred.workflow.utils;

public class StringUtils {

    public static boolean isEmpty(String s) {
        if (null == s || s.length() == 0) {
            return true;
        } else {
            return false;
        }
    }

}
