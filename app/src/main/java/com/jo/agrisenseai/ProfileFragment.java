package com.jo.agrisenseai;

import android.content.Intent;
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

public class ProfileFragment extends Fragment {

    private TextView languageValueText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Language card — opens language selection dialog
        languageValueText = view.findViewById(R.id.languageValueText);
        MaterialCardView languageCard = view.findViewById(R.id.languageCard);
        languageValueText.setText(LanguageManager.getSavedDisplayName(requireContext()));
        languageCard.setOnClickListener(v -> showLanguageDialog());

        // Settings card — opens SettingsActivity
        MaterialCardView settingsCard = view.findViewById(R.id.settingsCard);
        settingsCard.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

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
