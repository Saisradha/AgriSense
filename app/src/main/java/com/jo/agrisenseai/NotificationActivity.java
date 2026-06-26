package com.jo.agrisenseai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-app Notification Center.
 * Reads the "notifications/" node from Firebase in real time and displays
 * them newest-first in a RecyclerView.
 *
 * Tapping "Mark all read" sets isRead=true on every record in Firebase.
 */
public class NotificationActivity extends AppCompatActivity {

    private NotificationAdapter adapter;
    private LinearLayout emptyState;
    private RecyclerView recyclerView;
    private ValueEventListener notifListener;
    private DatabaseReference notifRef;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        emptyState   = findViewById(R.id.emptyState);
        recyclerView = findViewById(R.id.notifRecyclerView);
        TextView btnMarkAll = findViewById(R.id.btnMarkAllRead);

        adapter = new NotificationAdapter(this, new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        notifRef = FirebaseDatabase.getInstance().getReference("notifications");

        listenForNotifications();

        btnMarkAll.setOnClickListener(v -> markAllRead());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSettings).setOnClickListener(v ->
                startActivity(new Intent(NotificationActivity.this, NotificationSettingsActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notifListener != null) notifRef.removeEventListener(notifListener);
    }

    // ---------------------------------------------------------------
    // Firebase
    // ---------------------------------------------------------------

    private void listenForNotifications() {
        notifListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<NotificationModel> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NotificationModel n = child.getValue(NotificationModel.class);
                    if (n != null) list.add(n);
                }
                // Newest first
                Collections.sort(list, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                adapter.updateItems(list);
                emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        };
        notifRef.addValueEventListener(notifListener);
    }

    private void markAllRead() {
        notifRef.get().addOnSuccessListener(snapshot -> {
            for (DataSnapshot child : snapshot.getChildren()) {
                child.getRef().child("read").setValue(true);
            }
        });
    }
}
