package com.androidzeitgeist.heiau.classifier;

import android.util.Log;

import com.androidzeitgeist.heiau.IncomingURL;
import com.androidzeitgeist.heiau.feature.WebsiteFeatures;
import com.androidzeitgeist.heiau.tokenizer.TitleTokenizer;
import com.androidzeitgeist.heiau.tokenizer.URLTokenizer;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Classifier implements ValueEventListener {
    private TitleTokenizer titleTokenizer;
    private URLTokenizer urlTokenizer;

    private boolean initialize = false;

    public enum Category {
        GOOD,
        BAD
    }

    private ExecutorService executorService;

    // feature -> { category1: count1, category2: count2 }
    private Map<String, Map<Category, Integer>> titleFeatureCount;
    // category -> count
    private Map<Category, Integer> titleCategoryCount;

    // feature -> { category1: count1, category2: count2 }
    private Map<String, Map<Category, Integer>> urlFeatureCount;
    // category -> count
    private Map<Category, Integer> urlCategoryCount;

    public Classifier() {
        this.titleTokenizer = new TitleTokenizer();
        this.urlTokenizer = new URLTokenizer();

        this.titleFeatureCount = new HashMap<>();
        this.titleCategoryCount = new HashMap<>();

        this.urlFeatureCount = new HashMap<>();
        this.urlCategoryCount = new HashMap<>();

        this.executorService = Executors.newSingleThreadExecutor();

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                init();
            }
        });
    }

    private void init() {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        FirebaseDatabase
                .getInstance()
                .getReference()
                .child(user.getUid())
                .child("urls")
                .addListenerForSingleValueEvent(this);
    }

    public Future<Classification> classify(final WebsiteFeatures features) {
        return executorService.submit(new Callable<Classification>() {
            @Override
            public Classification call() throws Exception {
                synchronized (Classifier.this) {
                    while (!initialize) {
                        Log.w("SKDBG", "WAITING");
                        Classifier.this.wait(100);
                    }
                }

                return classifyInBackground(features);
            }
        });
    }

    private Classification classifyInBackground(WebsiteFeatures features) {
        Classification classification = new Classification();

        List<String> titleFeatures = titleTokenizer.tokenize(features.getTitle());
        classification.classification_bayes_title = classify(titleCategoryCount, titleFeatureCount, titleFeatures);

        List<String> urlFeatures = urlTokenizer.tokenize(features.getUrl());
        classification.classification_bayes_url = classify(urlCategoryCount, urlFeatureCount, urlFeatures);

        return classification;
    }

    private int classify(Map<Category, Integer> categoryCount, Map<String, Map<Category, Integer>> featureCount, List<String> features) {
        double goodProbablity = calculateProbability(categoryCount, featureCount, features, Category.GOOD);
        double badProbablity = calculateProbability(categoryCount, featureCount, features, Category.BAD);

        Log.w("SKDBG", "(+) GOOD: " + goodProbablity);
        Log.w("SKDBG", "(-) BAD:  " + badProbablity);
        Log.w("SKDBG", "  RATIO:  " + (goodProbablity / badProbablity));

        if (goodProbablity > badProbablity * 3) {
            return 1;
        } else if (badProbablity > goodProbablity * 3) {
            return -1;
        } else {
            return 0;
        }
    }

    private double calculateProbability(Map<Category, Integer> categoryCount, Map<String, Map<Category, Integer>> featureCount, List<String> features, Category category) {
        double categoryProbability = (double) getCategoryCount(categoryCount, category) / (double) getTotalCount(categoryCount);
        double documentProbablity = calculateDocumentProbability(categoryCount, featureCount, features, category);
        return documentProbablity * categoryProbability;
    }

    // P (document | category)
    private double calculateDocumentProbability(Map<Category, Integer> categoryCount, Map<String, Map<Category, Integer>> featureCount, List<String> features, Category category) {
        double probability = 1;

        for (String feature : features) {
            probability *= getWeightedProbability(categoryCount, featureCount, feature, category);
        }

        return probability;
    }


    private void incrementFeatureCount(Map<String, Map<Category, Integer>> count, String feature, Category category) {
        Map<Category, Integer> mapping = count.get(feature);

        if (mapping == null) {
            mapping = new HashMap<>();
            count.put(feature, mapping);
        }

        Integer c = mapping.get(category);
        if (c == null) {
            c = 0;
        }

        mapping.put(category, c + 1);
    }

    private void incrementCategoryCount(Map<Category, Integer> count, Category category) {
        Integer c = count.get(category);
        if (c == null) {
            c = 0;
        }

        count.put(category, c + 1);
    }

    // Number of times a feature appeared in a category
    private int getFeatureCount(Map<String, Map<Category, Integer>> count, String feature, Category category) {
        Map<Category, Integer> mapping = count.get(feature);
        if (mapping == null) {
            return 0;
        }

        Integer c = mapping.get(category);
        if (c == null) {
            return 0;
        }

        return c;
    }

    // Number of items in a category
    private int getCategoryCount(Map<Category, Integer> count, Category category) {
        Integer c = count.get(category);
        if (c == null) {
            return 0;
        }

        return c;
    }

    // Get total number of items
    private int getTotalCount(Map<Category, Integer> count) {
        int sum = 0;

        for (Category category : Category.values()) {
            sum += getCategoryCount(count, category);
        }

        return sum;
    }

    private int getTotalCountOfFeature(Map<String, Map<Category, Integer>> featureCount, String feature) {
        int sum = 0;

        Map<Category, Integer> mapping = featureCount.get(feature);
        if (mapping == null) {
            return 0;
        }

        for (Category category : Category.values()) {
            Integer c = mapping.get(category);
            if (c == null) {
                continue;
            }

            sum += c;
        }

        return sum;
    }

    // P (feature | category)
    private double getFeatureProbability(Map<Category, Integer> categoryCount, Map<String, Map<Category, Integer>> featureCount, String feature, Category category) {
        int itemsInCategory = getCategoryCount(categoryCount, category);

        if (itemsInCategory == 0) {
            return 0;
        }

        return (double) getFeatureCount(featureCount, feature, category)
                / (double) itemsInCategory;
    }

    private double getWeightedProbability(Map<Category, Integer> categoryCount, Map<String, Map<Category, Integer>> featureCount, String feature, Category category) {
        double basicProbability = getFeatureProbability(categoryCount, featureCount, feature, category);

        double totals = getTotalCountOfFeature(featureCount, feature);

        double weight = 1.0;
        double assumedProbability = 0.5;

        return ((weight * assumedProbability) + (totals * basicProbability)) / (weight + totals);
    }

    private void trainTitle(Category category, String title) {
        List<String> features = titleTokenizer.tokenize(title);
        if (features.isEmpty()) {
            return; // Nothing to train
        }

        for (String feature : features) {
            incrementFeatureCount(titleFeatureCount, feature, category);
        }

        incrementCategoryCount(titleCategoryCount, category);
    }

    private void trainURL(Category category, String url) {
        List<String> features = urlTokenizer.tokenize(url);
        if (features.isEmpty()) {
            return; // Nothing to train
        }

        for (String feature : features) {
            incrementFeatureCount(urlFeatureCount, feature, category);
        }

        incrementCategoryCount(urlCategoryCount, category);
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        Log.w("SKDBG", "LOADING DATA (" + dataSnapshot.getChildrenCount() + ")");

        for (DataSnapshot urlSnapshot : dataSnapshot.getChildren()) {
            IncomingURL url = urlSnapshot.getValue(IncomingURL.class);

            if (url.rating == 1) {
                trainTitle(Category.GOOD, url.title);
                trainURL(Category.GOOD, url.url);
            } else if (url.rating == -1) {
                trainTitle(Category.BAD, url.title);
                trainURL(Category.BAD, url.url);
            }
        }

        Log.w("SKDBG", "LOADING DONE");

        synchronized (Classifier.this) {
            initialize = true;
            notifyAll();
        }

        Log.w("SKDBG", "NOTIFIED");
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        initialize = true;

        synchronized (Classifier.this) {
            initialize = true;
            notifyAll();
        }
    }
}
