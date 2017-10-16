package com.ebsco.entarch.mappings.streamsets;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EscapeUtil {
    public static final Pattern pattern = Pattern.compile("\\W+?", 2);
    public static final char QUOTE_CHAR = '\'';

    private EscapeUtil() {
    }

    public static String singleQuoteEscape(String path) {
        return path != null && pattern.matcher(path).find() ? escapeQuotesAndBackSlash(path, true) : path;
    }

    public static String singleQuoteUnescape(String path) {
        if (path != null) {
            Matcher matcher = pattern.matcher(path);
            if (matcher.find() && path.length() > 2) {
                path = unescapeQuotesAndBackSlash(path, true);
                return path.substring(1, path.length() - 1);
            }
        }

        return path;
    }

    public static String doubleQuoteEscape(String path) {
        return path != null && pattern.matcher(path).find() ? escapeQuotesAndBackSlash(path, false) : path;
    }

    public static String doubleQuoteUnescape(String path) {
        if (path != null) {
            Matcher matcher = pattern.matcher(path);
            if (matcher.find() && path.length() > 2) {
                path = unescapeQuotesAndBackSlash(path, false);
                return path.substring(1, path.length() - 1);
            }
        }

        return path;
    }

    public static String getLastFieldNameFromPath(String path) {
        String[] pathSplit = path != null ? path.split("/") : null;
        if (pathSplit != null && pathSplit.length > 0) {
            String lastFieldName = pathSplit[pathSplit.length - 1];
            boolean singleQuoted = lastFieldName.charAt(0) == '\'' && lastFieldName.charAt(lastFieldName.length() - 1) == '\'';
            if (lastFieldName.contains("'") && !singleQuoted) {
                pathSplit = path.split("/'");
                if (pathSplit.length > 0) {
                    lastFieldName = "'" + pathSplit[pathSplit.length - 1];
                    singleQuoted = true;
                }
            }

            return singleQuoted ? singleQuoteUnescape(lastFieldName) : lastFieldName;
        } else {
            return path;
        }
    }

    private static String escapeQuotesAndBackSlash(String path, boolean isSingleQuoteEscape) {
        String quoteChar = isSingleQuoteEscape ? "'" : "\"";
        StringBuilder sb = (new StringBuilder(path.length() * 2)).append(quoteChar);
        char[] chars = path.toCharArray();
        char[] var5 = chars;
        int var6 = chars.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            char c = var5[var7];
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '"') {
                sb.append(isSingleQuoteEscape ? "\\\"" : "\\\\\"");
            } else if (c == '\'') {
                sb.append(isSingleQuoteEscape ? "\\\\'" : "\\'");
            } else {
                sb.append(c);
            }
        }

        return sb.append(quoteChar).toString();
    }

    private static String unescapeQuotesAndBackSlash(String path, boolean isSingleQuoteUnescape) {
        path = isSingleQuoteUnescape ? path.replace("\\\"", "\"").replace("\\\\'", "'") : path.replace("\\\\\"", "\"").replace("\\'", "'");
        return path.replace("\\\\", "\\");
    }

    public static String standardizePathForParse(String path, boolean isSingleQuoteEscape) {
        path = isSingleQuoteEscape ? path.replace("\\\\'", "\\'") : path.replace("\\\\\"", "\\\"");
        return path.replace("\\\\", "\\");
    }
}
