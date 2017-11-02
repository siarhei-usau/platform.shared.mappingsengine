package com.ebsco.platform.shared.mappingsengine.core;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringUtils {

     public String stripStart(final String str, final String stripChars) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        int start = 0;
        if (stripChars == null) {
            while (start != strLen && Character.isWhitespace(str.charAt(start))) {
                start++;
            }
        } else if (stripChars.isEmpty()) {
            return str;
        } else {
            while (start != strLen && stripChars.indexOf(str.charAt(start)) != -1) {
                start++;
            }
        }
        return str.substring(start);
    }

     public static String stripEnd(final String str, final String stripChars) {
        int end;
        if (str == null || (end = str.length()) == 0) {
            return str;
        }

        if (stripChars == null) {
            while (end != 0 && Character.isWhitespace(str.charAt(end - 1))) {
                end--;
            }
        } else if (stripChars.isEmpty()) {
            return str;
        } else {
            while (end != 0 && stripChars.indexOf(str.charAt(end - 1)) != -1) {
                end--;
            }
        }
        return str.substring(0, end);
    }

    public static int indexOfAny(String str, char... searchChars) {
        if (str.isEmpty() || searchChars.length == 0) {
            return -1;
        }
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            for (char searchChar : searchChars) {
                if (searchChar == ch) {
                    return i;
                }
            }
        }
        return -1;
    }

     public static String strip(String str, final String stripChars) {
        if (str.isEmpty()) {
            return str;
        }
        str = stripStart(str, stripChars);
        return stripEnd(str, stripChars);
    }

     public boolean isNumber(String s) {
        return s.chars().allMatch(Character::isDigit);
    }

    public String removePrefix(String s, String prefix) {
        if (s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }

    public String removeSuffix(String s, String suffix) {
         if (s.endsWith(suffix)) {
             return s.substring(0, s.length() - suffix.length());
         }
         return s;
    }

    public String substringBefore(String s, String delimiter, String missingDelimiterValue) {
         int index = s.indexOf(delimiter);
         if (index == -1) {
             return missingDelimiterValue;
         }
         return s.substring(0, index);
    }

    public String substringAfter(String s, String delimiter, String missingDelimiterValue) {
        int index = s.indexOf(delimiter);
        if (index == -1) {
            return missingDelimiterValue;
        }
        return s.substring(index + 1, s.length());
    }
}
