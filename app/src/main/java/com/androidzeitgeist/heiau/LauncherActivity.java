package com.androidzeitgeist.heiau;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;

public class LauncherActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // already signed in
            Toast.makeText(this, "Signed in", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Intent intent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setProviders(AuthUI.GOOGLE_PROVIDER)
                    .setTheme(R.style.AppTheme)
                    .build();

            startActivityForResult(intent, 101);

            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != 101) {
            return;
        }

        if (resultCode == RESULT_OK) {
            startActivity(new Intent(this, MainActivity.class));
        }

        finish();
    }
}
