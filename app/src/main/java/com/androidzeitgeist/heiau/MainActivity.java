package com.androidzeitgeist.heiau;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidzeitgeist.heiau.ui.ItemClickSupport;
import com.androidzeitgeist.heiau.util.HashUtil;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class MainActivity extends AppCompatActivity {
    private FirebaseRecyclerAdapter<IncomingURL, URLHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.data);
        assert recyclerView != null;

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;

        Query query = FirebaseDatabase.getInstance().getReference()
                .child(user.getUid())
                .child("urls")
                .orderByChild("rating")
                .equalTo(null);

        adapter = new FirebaseRecyclerAdapter<IncomingURL, URLHolder>(IncomingURL.class, R.layout.item_url, URLHolder.class, query) {
            @Override
            protected void populateViewHolder(URLHolder viewHolder, final IncomingURL model, final int modelposition) {

                ((TextView) viewHolder.itemView.findViewById(R.id.url)).setText(model.url);
                //((TextView) viewHolder.itemView.findViewById(android.R.id.text2)).setText();


                viewHolder.itemView.findViewById(R.id.like).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        like(model);
                    }
                });

                viewHolder.itemView.findViewById(R.id.dislike).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dislike(model);
                    }
                });

                viewHolder.itemView.findViewById(R.id.remove).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        remove(model);
                    }
                });

                ((TextView) viewHolder.itemView.findViewById(R.id.title)).setText(model.title);

                ImageView imageView = (ImageView) viewHolder.itemView.findViewById(R.id.image);
                if (model.image_url == null) {
                    imageView.setImageBitmap(null);
                } else {
                    Picasso.with(MainActivity.this)
                            .load(model.image_url)
                            .into(imageView);
                }

                assignDrawable(model.classification_bayes_title, (ImageView) viewHolder.itemView.findViewById(R.id.classifier_title));
                assignDrawable(model.classification_bayes_url, (ImageView) viewHolder.itemView.findViewById(R.id.classifier_url));
            }
        };

        recyclerView.setAdapter(adapter);

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, View view, int position) {
                Log.w("SKDBG", "Clicked: " + position);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(adapter.getItem(position).url));
                intent.setPackage("org.mozilla.fennec");
                startActivity(intent);

            }
        });
    }

    private void assignDrawable(int rating, ImageView imageView) {
        if (rating == 0) {
            imageView.setImageResource(R.drawable.ic_help_black_24dp);
        } else if (rating == 1) {
            imageView.setImageResource(R.drawable.ic_thumb_up_black_24dp);
        } else if (rating == -1) {
            imageView.setImageResource(R.drawable.ic_thumb_down_black_24dp);
        }
    }

    public void like(IncomingURL url) {
        Log.w("SKDBG", "Like: " + url.url);
        rate(url.url, 1);
    }

    public void dislike(IncomingURL url) {
        Log.w("SKDBG", "Dislike: " + url.url);
        rate(url.url, -1);
    }

    public void remove(IncomingURL url) {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        final String hash = HashUtil.SHA1(url.url);
        if (hash == null) {
            return;
        }

        final DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        database.child(user.getUid())
                .child("urls")
                .child(hash)
                .removeValue();
    }

    private void rate(String url, int rating) {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        final String hash = HashUtil.SHA1(url);
        if (hash == null) {
            return;
        }

        final DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        Map<String, Object> update = new HashMap<>();
        update.put("rating", rating);

        database.child(user.getUid())
                .child("urls")
                .child(hash)
                .updateChildren(update);
    }
}
