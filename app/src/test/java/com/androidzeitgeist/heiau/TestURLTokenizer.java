package com.androidzeitgeist.heiau;

import com.androidzeitgeist.heiau.tokenizer.URLTokenizer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collection;
import java.util.List;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class TestURLTokenizer {
    @Test
    public void testAndroidDevelopersURL() {
        URLTokenizer tokenizer = new URLTokenizer();

        List<String> tokens = tokenizer.tokenize("https://developer.android.com/reference/java/util/StringTokenizer.html");

        Assert.assertEquals(10, tokens.size());

        assertContains(tokens, "developer.android.com");
        assertContains(tokens, "developer");
        assertContains(tokens, "android");
        assertContains(tokens, "com");
        assertContains(tokens, "reference");
        assertContains(tokens, "java");
        assertContains(tokens, "util");
        assertContains(tokens, "StringTokenizer.html");
        assertContains(tokens, "StringTokenizer");
        assertContains(tokens, "html");
    }

    @Test
    public void testMediumURL() {
        URLTokenizer tokenizer = new URLTokenizer();

        List<String> tokens = tokenizer.tokenize("https://medium.com/xeneta/boosting-sales-with-machine-learning-fbcf2e618be3#.ihnf87zcq");

        Assert.assertEquals(11, tokens.size());

        assertContains(tokens, "medium.com");
        assertContains(tokens, "medium");
        assertContains(tokens, "com");
        assertContains(tokens, "xeneta");
        assertContains(tokens, "boosting-sales-with-machine-learning-fbcf2e618be3");
        assertContains(tokens, "boosting");
        assertContains(tokens, "sales");
        assertContains(tokens, "with");
        assertContains(tokens, "machine");
        assertContains(tokens, "learning");
        assertContains(tokens, "fbcf2e618be3");
    }

    private <E> void assertContains(Collection<E> collection, E object) {
        Assert.assertTrue(collection + " contains " + object, collection.contains(object));
    }
}
