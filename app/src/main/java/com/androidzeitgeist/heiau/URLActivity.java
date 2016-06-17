package com.androidzeitgeist.heiau;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class URLActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.w("SKDBG", "onCreate()");

        super.onCreate(savedInstanceState);

        final Intent originalIntent = getIntent();

        URLProcessorService.queueURL(this, originalIntent.getDataString());

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(originalIntent.getData());
        intent.setPackage("org.mozilla.fennec");
        startActivity(intent);

        finish();
    }
}


