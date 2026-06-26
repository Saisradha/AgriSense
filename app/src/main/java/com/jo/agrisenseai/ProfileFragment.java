package com.jo.agrisenseai;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName;
    private TextView tvProfilePhone;
    private TextView languageValueText;

    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();

        // Bind profile elements
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfilePhone = view.findViewById(R.id.tvProfilePhone);

        // Language card — opens language selection dialog
        languageValueText = view.findViewById(R.id.languageValueText);
        MaterialCardView languageCard = view.findViewById(R.id.languageCard);
        languageValueText.setText(LanguageManager.getSavedDisplayName(requireContext()));
        languageCard.setOnClickListener(v -> showLanguageDialog());

        // Settings card — opens SettingsActivity
        MaterialCardView settingsCard = view.findViewById(R.id.settingsCard);
        settingsCard.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

        // Logout card — signs out of the app
        MaterialCardView logoutCard = view.findViewById(R.id.logoutCard);
        logoutCard.setOnClickListener(v -> performLogout());

        // Load profile data
        loadProfileData();

        return view;
    }

    private void loadProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userEmail = user.getEmail();
            if (userEmail != null && !userEmail.isEmpty()) {
                tvProfilePhone.setText(userEmail);
            } else {
                tvProfilePhone.setText("Email not configured");
            }

            FirebaseHelper.getInstance().getUserProfile(user.getUid(), profile -> {
                if (!isAdded()) return;
                if (profile != null) {
                    if (profile.getName() != null && !profile.getName().isEmpty()) {
                        tvProfileName.setText(profile.getName());
                    }
                    
                    StringBuilder contactInfo = new StringBuilder();
                    if (userEmail != null && !userEmail.isEmpty()) {
                        contactInfo.append(userEmail);
                    }
                    if (profile.getPhone() != null && !profile.getPhone().isEmpty()) {
                        if (contactInfo.length() > 0) {
                            contactInfo.append("  |  ");
                        }
                        contactInfo.append(profile.getPhone());
                    }
                    if (contactInfo.length() > 0) {
                        tvProfilePhone.setText(contactInfo.toString());
                    }
                }
            });
        }
    }

    private void performLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to sign out of AgriSense AI?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(requireContext(), SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
