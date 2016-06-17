package com.androidzeitgeist.heiau;

import com.androidzeitgeist.heiau.testing.classifier.Classification;
import com.androidzeitgeist.heiau.testing.classifier.Classifier;
import com.androidzeitgeist.heiau.testing.classifier.bayes.BayesClassifier;
import com.androidzeitgeist.heiau.tokenizer.URLTokenizer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.text.DecimalFormat;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class TestBayesClassifier {
    @Test
    public void testHackerNewsClassification() {
        // Create a new bayes classifier with string categories and string features.
        Classifier<String, String> bayes = new BayesClassifier<>();
        //bayes.setMemoryCapacity(10000);

        URLTokenizer tokenizer = new URLTokenizer();

        for (String url : TestData.INTERESTING_URLS) {
            bayes.learn("interesting", tokenizer.tokenize(url));
        }

        for (String url : TestData.BORING_URLS) {
            bayes.learn("boring", tokenizer.tokenize(url));
        }

        String classifyURL = "https://github.com/rasbt/python-machine-learning-book/blob/master/faq/difference-deep-and-normal-learning.md";

        Classification<String, String> classification = bayes.classify(tokenizer.tokenize(classifyURL));

        System.out.println("Category:    " + classification.getCategory());
        System.out.println("Probability: " + format(classification.getProbability()));
        System.out.println("Features:    " + classification.getFeatureset());

        /*
        Collection<Classification<String, String>> classifications = ((BayesClassifier<String, String>) bayes).classifyDetailed(tokenizer.tokenize(classifyURL));
        for (Classification<String, String> c : classifications) {
            System.out.println("    " +  c.getCategory() + " -> " + format(c.getProbability()));
        }
        */
    }

    private String format(float value) {
        if (value == Float.POSITIVE_INFINITY) {
            return "INFINITY+";
        }
        if (value == Float.NEGATIVE_INFINITY) {
            return "INFINITY-";
        }
        return new DecimalFormat("#.##########").format(value);
    }

}
