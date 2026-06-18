package com.jo.agrisenseai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

public class ProfileFragment extends Fragment {

    private TextView languageValueText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Language card
        languageValueText = view.findViewById(R.id.languageValueText);
        MaterialCardView languageCard = view.findViewById(R.id.languageCard);
        languageValueText.setText(LanguageManager.getSavedDisplayName(requireContext()));
        languageCard.setOnClickListener(v -> showLanguageDialog());

        // Voice enabled switch
        MaterialSwitch switchVoice = view.findViewById(R.id.switchVoiceEnabled);
        switchVoice.setChecked(VoicePreferenceManager.isVoiceEnabled(requireContext()));
        switchVoice.setOnCheckedChangeListener((btn, checked) ->
                VoicePreferenceManager.setVoiceEnabled(requireContext(), checked));

        // Auto speak switch
        MaterialSwitch switchAutoSpeak = view.findViewById(R.id.switchAutoSpeak);
        switchAutoSpeak.setChecked(VoicePreferenceManager.isAutoSpeak(requireContext()));
        switchAutoSpeak.setOnCheckedChangeListener((btn, checked) ->
                VoicePreferenceManager.setAutoSpeak(requireContext(), checked));

        // Notifications enabled switch
        MaterialSwitch switchNotif = view.findViewById(R.id.switchNotifEnabled);
        switchNotif.setChecked(NotificationHelper.isEnabled(requireContext()));
        switchNotif.setOnCheckedChangeListener((btn, checked) ->
                NotificationHelper.setEnabled(requireContext(), checked));

        // Notification sound switch
        MaterialSwitch switchNotifSound = view.findViewById(R.id.switchNotifSound);
        switchNotifSound.setChecked(NotificationHelper.isSoundEnabled(requireContext()));
        switchNotifSound.setOnCheckedChangeListener((btn, checked) ->
                NotificationHelper.setSoundEnabled(requireContext(), checked));

        return view;
    }

    private void showLanguageDialog() {
        int currentIndex = LanguageManager.getSavedLocaleIndex(requireContext());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_select_language)
                .setSingleChoiceItems(
                        LanguageManager.DISPLAY_NAMES,
                        currentIndex,
                        (dialog, which) -> {
                            String selectedCode = LanguageManager.LOCALE_CODES[which];
                            String selectedName = LanguageManager.DISPLAY_NAMES[which];

                            LanguageManager.saveLanguage(requireContext(), selectedCode);
                            languageValueText.setText(selectedName);

                            dialog.dismiss();

                            if (getActivity() != null) {
                                getActivity().recreate();
                            }
                        })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
