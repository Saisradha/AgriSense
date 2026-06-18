package com.jo.agrisenseai;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class AIAssistantFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai_assistant, container, false);

        FloatingActionButton fabMic = view.findViewById(R.id.fabMicLaunch);
        fabMic.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), VoiceAssistantActivity.class);
            startActivity(intent);
        });

        return view;
    }
}
