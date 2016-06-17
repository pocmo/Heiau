package com.androidzeitgeist.heiau;

import com.androidzeitgeist.heiau.feature.Featurizer;
import com.androidzeitgeist.heiau.feature.WebsiteFeatures;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import okhttp3.OkHttpClient;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class TestFeaturizer {
    @Test
    public void testGrabbingImage() {
        WebsiteFeatures features = Featurizer.featurize(new OkHttpClient(), "https://en.m.wikipedia.org/wiki/Henry_Drisler");

        if (features == null) {
            System.out.println("No features!");
            return;
        }

        System.out.println("Title: " + features.getTitle());
        System.out.println("Image: " + features.getImageURL());
    }
}
