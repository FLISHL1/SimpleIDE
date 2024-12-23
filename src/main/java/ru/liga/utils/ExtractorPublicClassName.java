package ru.liga.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractorPublicClassName {
    public String extractClassName(String code) {
        Pattern pattern = Pattern.compile("public\\s+class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
