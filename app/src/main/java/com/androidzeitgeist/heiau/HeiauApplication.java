package com.androidzeitgeist.heiau;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class HeiauApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        //FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
