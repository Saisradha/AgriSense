package com.jo.agrisenseai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Displays a live, reverse-chronological log of every sensor reading
 * permanently stored in the Firebase {@code history/} node.
 *
 * <p>The fragment attaches a real-time listener via
 * {@link FirebaseHelper#listenHistory} so the list refreshes automatically
 * whenever new sensor data is pushed, without any manual refresh.</p>
 */
public class HistoryFragment extends Fragment {

    private RecyclerView         historyRecyclerView;
    private HistoryAdapter       historyAdapter;
    private TextView             emptyStateText;

    private ValueEventListener   historyListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_history, container, false);

        historyRecyclerView = view.findViewById(R.id.historyRecyclerView);
        emptyStateText      = view.findViewById(R.id.historyEmptyText);

        historyAdapter = new HistoryAdapter(new ArrayList<>());
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyRecyclerView.setAdapter(historyAdapter);

        loadHistory();

        return view;
    }

    /**
     * Attaches a real-time Firebase listener that delivers the latest
     * {@code history/} entries and refreshes the RecyclerView automatically.
     */
    private void loadHistory() {
        historyListener = FirebaseHelper.getInstance().listenHistory(entries -> {
            if (!isAdded()) return;

            if (entries == null || entries.isEmpty()) {
                historyRecyclerView.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.VISIBLE);
                return;
            }

            // Reverse so newest entry appears at the top of the list.
            List<SensorHistory> reversed = new ArrayList<>(entries);
            Collections.reverse(reversed);

            historyRecyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
            historyAdapter.updateData(reversed);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Detach the listener to prevent memory leaks when the fragment is gone.
        if (historyListener != null) {
            FirebaseHelper.getInstance().removeHistoryListener(historyListener);
            historyListener = null;
        }
    }
}