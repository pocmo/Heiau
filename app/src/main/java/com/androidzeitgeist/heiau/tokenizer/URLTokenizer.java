package com.androidzeitgeist.heiau.tokenizer;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class URLTokenizer implements Tokenizer {
    /**
     * Input:  https://developer.android.com/reference/java/util/StringTokenizer.html
     * Tokens: developer.android.com
     *         developer
     *         android
     *         com
     *         reference
     *         java
     *         util
     *         StringTokenizer.html
     *         StringTokenizer
     *         html
     *
     * @param url
     * @return
     */
    @Override
    public List<String> tokenize(String url) {
        List<String> tokens = new ArrayList<>();

        Uri uri = Uri.parse(url);

        final String host = uri.getHost();
        if (host != null) {
            tokens.add(uri.getHost());

            Collections.addAll(tokens, uri.getHost().split("\\."));
        }

        for (String segment : uri.getPathSegments()) {
            String[] parts = segment.split("[-_\\.]");
            if (parts.length > 1) {
                Collections.addAll(tokens, parts);
            }

            tokens.add(segment);
        }

        // Temporary
        List<String> filter = Arrays.asList("www", "com");

        Iterator<String> iterator = tokens.iterator();
        while (iterator.hasNext()) {
            String token = iterator.next();

            if (token.length() <= 2 || filter.contains(token)) {
                iterator.remove();
            }
        }

        return tokens;
    }
}
