package com.avandy.news.utils;

import java.util.Objects;

public class JaroWinklerDistance {

    public int compare(String text1, String text2) {
        if (Objects.equals(text1, text2)) return 100;

        int textLength1 = text1.length();
        int textLength2 = text2.length();
        int maxDist = (int) (Math.floor((double) Math.max(textLength1, textLength2) / 2) - 1);
        int match = 0;
        int[] hashS1 = new int[text1.length()];
        int[] hashS2 = new int[text2.length()];

        for (int i = 0; i < textLength1; i++) {
            for (int j = Math.max(0, i - maxDist);
                 j < Math.min(textLength2, i + maxDist + 1); j++)

                if (text1.charAt(i) == text2.charAt(j) && hashS2[j] == 0) {
                    hashS1[i] = 1;
                    hashS2[j] = 1;
                    match++;
                    break;
                }
        }

        if (match == 0) return 0;

        double t = 0;
        int point = 0;
        for (int i = 0; i < textLength1; i++)
            if (hashS1[i] == 1) {

                while (hashS2[point] == 0)
                    point++;
                if (text1.charAt(i) != text2.charAt(point++))
                    t++;
            }
        t /= 2;

        return (int) Math.floor((((double) (match) / (textLength1) + (double) (match) / (textLength2) + (match - t) / (match))/ 3.0) * 100);
    }

}