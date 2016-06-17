package com.androidzeitgeist.heiau;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.androidzeitgeist.heiau.classifier.Classification;
import com.androidzeitgeist.heiau.classifier.Classifier;
import com.androidzeitgeist.heiau.feature.Featurizer;
import com.androidzeitgeist.heiau.feature.WebsiteFeatures;
import com.androidzeitgeist.heiau.util.HashUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import okhttp3.OkHttpClient;

public class URLProcessorService extends IntentService {
    private static final String EXTRA_KEY_URL = "url";

    private Classifier classifier;
    private OkHttpClient client;

    public URLProcessorService() {
        super(URLProcessorService.class.getSimpleName());

        classifier = new Classifier();
        client = new OkHttpClient();
    }

    public static void queueURL(Context context, String url) {
        Intent intent = new Intent(context, URLProcessorService.class);
        intent.putExtra(EXTRA_KEY_URL, url);
        context.startService(intent);

        Log.w("SKDBG", "queueURL()");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.w("SKDBG", "onHandleIntent()");

        final String url = intent.getStringExtra(EXTRA_KEY_URL);

        WebsiteFeatures features = Featurizer.featurize(client, url);
        if (features == null) {
            return;
        }

        Log.w("SKDBG", "Now starting to classify...");

        Future<Classification> classify = classifier.classify(features);

        Classification classification = new Classification();

        try {
            classification = classify.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        Log.w("SKDBG", "Classified: (" + classification.classification_bayes_url + "," + classification.classification_bayes_title + "): " + features.getTitle());
        Log.w("SKDBG", "            " + features.getUrl());

        saveData(features, classification);

        toast("(" + classification.classification_bayes_url + "," + classification.classification_bayes_title + "): " + features.getTitle());
    }

    private void saveData(final WebsiteFeatures features, final Classification classification) {
        if (features == null) {
            return;
        }

        if (TextUtils.isEmpty(features.getUrl())) {
            return;
        }

        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        final String hash = HashUtil.SHA1(features.getUrl());
        if (hash == null) {
            return;
        }

        final DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        database.child(user.getUid()).child("urls").child(hash).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.w("SKDBG", "onDataChange(): exists=" + dataSnapshot.exists());

                if (!dataSnapshot.exists()) {
                    createEntry(database, user, hash, features, classification);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("SKDBG", "onCancelled(): " + databaseError.getMessage());
            }
        });
    }

    private void createEntry(DatabaseReference database, FirebaseUser user, String hash, WebsiteFeatures features, Classification classification) {
        Map<String, Object> update = new HashMap<>();
        update.put("url", features.getUrl());
        update.put("title", features.getTitle());
        update.put("image_url", features.getImageURL());
        update.put("seen_at", System.currentTimeMillis());
        update.put("source", "intent");
        update.put("rating", null);
        update.put("classification_bayes_title", classification.classification_bayes_title);
        update.put("classification_bayes_url", classification.classification_bayes_url);

        database.child(user.getUid())
                .child("urls")
                .child(hash)
                .updateChildren(update);
    }

    private void toast(final String text) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(URLProcessorService.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
