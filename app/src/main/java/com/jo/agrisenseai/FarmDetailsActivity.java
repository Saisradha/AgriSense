package com.jo.agrisenseai;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FarmDetailsActivity extends AppCompatActivity {

    private String farmId;
    private ValueEventListener farmListener;

    // Views
    private TextView tvFarmName, tvFarmLocation, tvHealthBadge;
    private TextView tvCropType, tvArea, tvSoilMoisture;
    private TextView tvNextWatering, tvMoistureThreshold, tvIrrigationSchedule;
    private TextView tvCreatedAt, tvNotes;
    private MaterialButton btnEditFarm, btnDeleteFarm;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farm_details);

        farmId = getIntent().getStringExtra("farmId");
        if (farmId == null) {
            finish();
            return;
        }

        // Bind views
        tvFarmName           = findViewById(R.id.tvFarmName);
        tvFarmLocation       = findViewById(R.id.tvFarmLocation);
        tvHealthBadge        = findViewById(R.id.tvHealthBadge);
        tvCropType           = findViewById(R.id.tvCropType);
        tvArea               = findViewById(R.id.tvArea);
        tvSoilMoisture       = findViewById(R.id.tvSoilMoisture);
        tvNextWatering       = findViewById(R.id.tvNextWatering);
        tvMoistureThreshold  = findViewById(R.id.tvMoistureThreshold);
        tvIrrigationSchedule = findViewById(R.id.tvIrrigationSchedule);
        tvCreatedAt          = findViewById(R.id.tvCreatedAt);
        tvNotes              = findViewById(R.id.tvNotes);
        btnEditFarm          = findViewById(R.id.btnEditFarm);
        btnDeleteFarm        = findViewById(R.id.btnDeleteFarm);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finishWithTransition());

        // Edit button → launch EditFarmActivity
        btnEditFarm.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditFarmActivity.class);
            intent.putExtra("farmId", farmId);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                    this, android.R.anim.fade_in, android.R.anim.fade_out);
            startActivity(intent, options.toBundle());
        });

        // Delete button → confirmation dialog
        btnDeleteFarm.setOnClickListener(v -> showDeleteDialog());

        // Start real-time listener for this specific farm
        listenForFarm();
    }

    private void listenForFarm() {
        farmListener = FirebaseHelper.getInstance().listenFarmById(farmId, new FirebaseHelper.FarmByIdListener() {
            @Override
            public void onFarmLoaded(Farm farm) {
                populateUi(farm);
            }

            @Override
            public void onFarmNotFound() {
                // Farm was deleted → return to My Farm
                finish();
            }
        });
    }

    private void populateUi(Farm farm) {
        tvFarmName.setText(farm.getFarmName() != null ? farm.getFarmName() : "—");
        tvFarmLocation.setText(farm.getLocation() != null ? farm.getLocation() : "—");
        tvCropType.setText(farm.getCropType() != null ? farm.getCropType() : "—");
        tvArea.setText(farm.getTotalAcres() != null ? farm.getTotalAcres() : "—");
        tvSoilMoisture.setText(String.valueOf(farm.getSoilMoisture()));
        tvNextWatering.setText(farm.getNextWatering() != null ? farm.getNextWatering() : "—");
        tvMoistureThreshold.setText(String.valueOf(farm.getMoistureThreshold()));
        tvIrrigationSchedule.setText(farm.getIrrigationSchedule() != null ? farm.getIrrigationSchedule() : "—");
        tvNotes.setText(farm.getNotes() != null && !farm.getNotes().isEmpty() ? farm.getNotes() : "No notes");

        // Format created date
        if (farm.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
            tvCreatedAt.setText(sdf.format(new Date(farm.getCreatedAt())));
        } else {
            tvCreatedAt.setText("—");
        }

        // Health status badge
        String status = farm.getHealthStatus() != null ? farm.getHealthStatus() : "Healthy";
        tvHealthBadge.setText(status);

        int textColorRes;
        int bgColorRes;
        switch (status) {
            case "Critical":
            case "High":
                textColorRes = R.color.status_critical;
                bgColorRes   = R.color.icon_bg_red;
                break;
            case "Medium":
                textColorRes = R.color.status_medium;
                bgColorRes   = R.color.icon_bg_orange;
                break;
            default:
                textColorRes = R.color.status_healthy;
                bgColorRes   = R.color.icon_bg_green;
                break;
        }
        tvHealthBadge.setTextColor(ContextCompat.getColor(this, textColorRes));
        tvHealthBadge.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, bgColorRes)));
    }

    private void showDeleteDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Farm?")
                .setMessage("Are you sure you want to permanently delete this farm? This action cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> performDelete())
                .show();
    }

    private void performDelete() {
        btnDeleteFarm.setEnabled(false);
        FirebaseHelper.getInstance().deleteFarm(farmId, (error, ref) -> {
            if (error == null) {
                showSnackbar("Farm Deleted Successfully");
                // Small delay to show Snackbar before finishing
                findViewById(android.R.id.content).postDelayed(this::finishWithTransition, 1200);
            } else {
                btnDeleteFarm.setEnabled(true);
                showSnackbar("Error deleting farm: " + error.getMessage());
            }
        });
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.primary_green))
                .setTextColor(ContextCompat.getColor(this, R.color.white))
                .show();
    }

    private void finishWithTransition() {
        supportFinishAfterTransition();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove real-time listener to avoid leaks
        if (farmListener != null && farmId != null) {
            FirebaseHelper.getInstance().removeListenerForFarm(farmId, farmListener);
        }
    }
}
