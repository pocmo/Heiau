package com.androidzeitgeist.heiau.feature;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Featurizer {
    public static WebsiteFeatures featurize(OkHttpClient client, String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.code() != 200) {
                return null;
            }

            WebsiteFeatures features = new WebsiteFeatures();
            features.setUrl(response.request().url().toString());

            Document document = Jsoup.parse(
                    response.body().byteStream(),
                    response.body().contentType().charset(StandardCharsets.UTF_8).name(),
                    features.getUrl());

            features.setTitle(grabTitle(document));
            features.setImageURL(grabImageURL(document));

            return features;
        } catch (IOException e) {
            return null;
        }
    }

    private static String grabTitle(Document document) {
        Elements elements = document.select("head title");
        if (elements.size() > 0) {
            return elements.get(0).text();
        }

        return null;
    }

    /**
     * 1. Facebook OpenGraph Image
     * 2. W3C Open Graph Image
     * 3. Twitter Card Image
     * 4. Find largest image in content
     *
     * https://tech.shareaholic.com/2012/11/02/how-to-find-the-image-that-best-respresents-a-web-page/
     *
     * @param document
     * @return
     */
    private static String grabImageURL(Document document) {
        Elements facebookGraphImages = document.select("meta[property='og:image']");
        if (facebookGraphImages.size() > 0) {
            Element element = facebookGraphImages.get(0);
            if (element.hasAttr("content")) {
                return element.attr("abs:content");
            }
        }

        Elements openGraphImages = document.select("meta[property='http://ogp.me/ns#image']");
        if (openGraphImages.size() > 0) {
            Element element = openGraphImages.get(0);
            if (element.hasAttr("content")) {
                return element.attr("abs:content");
            }
        }

        Elements twitterImages = document.select("meta[property='twitter:image']");
        if (twitterImages.size() > 0) {
            Element element = twitterImages.get(0);
            if (element.hasAttr("content")) {
                return element.attr("abs:content");
            }
        }

        int largestSize = 0;
        String imageURL = null;

        // This should be optimized to search in content divs!
        Elements images = document.select("img");

        System.out.println("Images: " + images.size());

        for (Element image : images) {
            if (image.hasAttr("width") && image.hasAttr("height")) {
                int width;
                int height;

                try {
                    width = Integer.parseInt(image.attr("width"));
                    height = Integer.parseInt(image.attr("height"));
                } catch (NumberFormatException e) {
                    continue;
                }

                double ratio = (double) width / (double) height;

                System.out.println("Image " + width + "x" + height + " (" + (width * height) + ") => " + ratio);

                if (ratio > 3.0) {
                    continue; // Not interested
                }

                // If we do not have those attributes then we should download some bits to determine the size;
                // See https://github.com/sdsykes/fastimage
                int size = width * height;
                if (size > largestSize) {
                    String src = image.attr("abs:src");

                    System.out.println("  (!) SRC: " + src);

                    imageURL = src;

                    largestSize = size;
                } else {
                }
            }
        }

        if (largestSize >= 5000) {
            return imageURL;
        }

        return null;
    }
}
