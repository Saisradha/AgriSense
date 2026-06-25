package com.jo.agrisenseai;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MyFarmFragment extends Fragment {

    private RecyclerView recyclerView;
    private FarmAdapter adapter;
    private ValueEventListener farmsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_farm, container, false);

        recyclerView = view.findViewById(R.id.farmsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        adapter = new FarmAdapter(requireContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Click listener for the dash card
        View addFarmCard = view.findViewById(R.id.addFarmCard);
        if (addFarmCard != null) {
            addFarmCard.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AddFarmActivity.class);
                ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                        requireContext(), android.R.anim.fade_in, android.R.anim.fade_out);
                startActivity(intent, options.toBundle());
            });
        }

        // Wire farm card clicks → FarmDetailsActivity
        adapter.setOnFarmClickListener((clickedView, farmId) -> {
            Intent intent = new Intent(requireContext(), FarmDetailsActivity.class);
            intent.putExtra("farmId", farmId);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(), clickedView, "farm_card_transition");
            startActivity(intent, options.toBundle());
        });

        loadFarms();

        return view;
    }

    private void loadFarms() {
        farmsListener = FirebaseHelper.getInstance().listenFarms(farms -> {
            if (!isAdded()) return;
            adapter.updateList(farms);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (farmsListener != null) {
            FirebaseHelper.getInstance().removeListener(FirebaseHelper.NODE_FARMS, farmsListener);
        }
    }
}
