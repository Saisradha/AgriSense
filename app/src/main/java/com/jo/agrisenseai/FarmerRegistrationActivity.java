package com.jo.agrisenseai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * FarmerRegistrationActivity — Collects profile info (name, location) from the farmer.
 */
public class FarmerRegistrationActivity extends AppCompatActivity {

    private TextInputEditText etRegName, etRegPhone, etRegVillage, etRegDistrict, etRegState;
    private TextView tvRegError;
    private MaterialButton btnCompleteProfile;
    private ProgressBar progressReg;

    private FirebaseAuth mAuth;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_registration);

        mAuth = FirebaseAuth.getInstance();

        // Bind Views
        etRegName = findViewById(R.id.etRegName);
        etRegPhone = findViewById(R.id.etRegPhone);
        etRegVillage = findViewById(R.id.etRegVillage);
        etRegDistrict = findViewById(R.id.etRegDistrict);
        etRegState = findViewById(R.id.etRegState);
        tvRegError = findViewById(R.id.tvRegError);
        btnCompleteProfile = findViewById(R.id.btnCompleteProfile);
        progressReg = findViewById(R.id.progressReg);

        btnCompleteProfile.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String name = etRegName.getText() != null ? etRegName.getText().toString().trim() : "";
        String phone = etRegPhone.getText() != null ? etRegPhone.getText().toString().trim() : "";
        String village = etRegVillage.getText() != null ? etRegVillage.getText().toString().trim() : "";
        String district = etRegDistrict.getText() != null ? etRegDistrict.getText().toString().trim() : "";
        String state = etRegState.getText() != null ? etRegState.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(village) || TextUtils.isEmpty(district) || TextUtils.isEmpty(state)) {
            tvRegError.setText(R.string.reg_error_empty);
            tvRegError.setVisibility(View.VISIBLE);
            return;
        }

        tvRegError.setVisibility(View.GONE);
        btnCompleteProfile.setEnabled(false);
        progressReg.setVisibility(View.VISIBLE);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            btnCompleteProfile.setEnabled(true);
            progressReg.setVisibility(View.GONE);
            tvRegError.setText("Authentication expired. Please log in again.");
            tvRegError.setVisibility(View.VISIBLE);
            return;
        }

        String uid = user.getUid();
        UserProfile profile = new UserProfile(
                uid,
                name,
                phone,
                village,
                district,
                state,
                false, // hasDevice initially false
                false, // demoMode initially false
                System.currentTimeMillis()
        );

        FirebaseHelper.getInstance().saveUserProfile(profile, (error, ref) -> {
            btnCompleteProfile.setEnabled(true);
            progressReg.setVisibility(View.GONE);

            if (error == null) {
                // Navigate to Farm Setup/Onboarding
                Intent intent = new Intent(FarmerRegistrationActivity.this, FarmSetupActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } else {
                tvRegError.setText("Failed to save profile: " + error.getMessage());
                tvRegError.setVisibility(View.VISIBLE);
            }
        });
    }
}
