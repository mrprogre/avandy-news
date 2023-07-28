package com.avandy.news.utils;

/*
* В линуксе не работает правая кнопка мыши, шрифты больше и не помещаются, sqlite открывается как архив
* */
public class OsChecker {
    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS.contains("win");
    }

    public static boolean isMac() {
        return OS.contains("mac");
    }

    public static boolean isUnix() {
        return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
    }

    public static boolean isSolaris() {
        return OS.contains("sunos");
    }

}