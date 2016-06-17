package com.androidzeitgeist.heiau.tokenizer;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleTokenizer {
    public List<String> tokenize(String title) {
        List<String> tokens = new ArrayList<>();
        if (TextUtils.isEmpty(title)) {
            return tokens;
        }

        Pattern pattern = Pattern.compile("\\w+");
        Matcher matcher = pattern.matcher(title);

        while (matcher.find()) {
            String part = matcher.group();
            if (part.length() > 2) {
                tokens.add(part.toLowerCase());
            }
        }

        return tokens;
    }
}
