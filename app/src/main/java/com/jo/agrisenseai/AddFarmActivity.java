package com.jo.agrisenseai;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class AddFarmActivity extends AppCompatActivity {

    private TextInputLayout tilFarmName, tilLocation, tilArea, tilNotes, tilCropType;
    private TextInputEditText etFarmName, etLocation, etArea, etNotes;
    private AutoCompleteTextView actvCropType;
    private TextView tvMoistureValue;
    private Slider sliderMoisture;
    private AppCompatSpinner spinnerSchedule;
    private MaterialButton btnSave, btnCancel;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_farm);

        // Bind Views
        tilFarmName = findViewById(R.id.tilFarmName);
        tilLocation = findViewById(R.id.tilLocation);
        tilArea = findViewById(R.id.tilArea);
        tilNotes = findViewById(R.id.tilNotes);
        tilCropType = findViewById(R.id.tilCropType);

        etFarmName = findViewById(R.id.etFarmName);
        etLocation = findViewById(R.id.etLocation);
        etArea = findViewById(R.id.etArea);
        etNotes = findViewById(R.id.etNotes);

        actvCropType = findViewById(R.id.actvCropType);
        tvMoistureValue = findViewById(R.id.tvMoistureValue);
        sliderMoisture = findViewById(R.id.sliderMoisture);
        spinnerSchedule = findViewById(R.id.spinnerSchedule);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        // Setup Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finishWithTransition());

        // Setup Crop Type AutoCompleteTextView Dropdown
        String[] cropOptions = {"Rice", "Wheat", "Cotton", "Tomato"};
        ArrayAdapter<String> cropAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, cropOptions);
        actvCropType.setAdapter(cropAdapter);
        // Default select first item
        actvCropType.setText(cropOptions[0], false);

        actvCropType.setOnItemClickListener((parent, view, position, id) -> {
            if (tilCropType != null) {
                tilCropType.setError(null);
            }
        });

        // Setup Soil Moisture Slider
        sliderMoisture.addOnChangeListener((slider, value, fromUser) -> {
            tvMoistureValue.setText(String.valueOf((int) value));
        });

        // Setup Irrigation Schedule Spinner
        String[] scheduleOptions = {"Morning", "Afternoon", "Evening", "Automatic"};
        ArrayAdapter<String> scheduleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, scheduleOptions);
        scheduleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSchedule.setAdapter(scheduleAdapter);

        // Clear Errors on text change
        setupErrorClearing();

        // Button Actions
        btnCancel.setOnClickListener(v -> finishWithTransition());
        btnSave.setOnClickListener(v -> attemptSaveFarm());
    }

    private void setupErrorClearing() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                tilFarmName.setError(null);
                tilLocation.setError(null);
                tilArea.setError(null);
                if (tilCropType != null) {
                    tilCropType.setError(null);
                }
            }
        };
        etFarmName.addTextChangedListener(watcher);
        etLocation.addTextChangedListener(watcher);
        etArea.addTextChangedListener(watcher);
    }

    private void attemptSaveFarm() {
        String farmName = etFarmName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String area = etArea.getText().toString().trim();
        String crop = actvCropType.getText().toString();
        int threshold = (int) sliderMoisture.getValue();
        String schedule = spinnerSchedule.getSelectedItem() != null ?
                spinnerSchedule.getSelectedItem().toString() : "Automatic";
        String notes = etNotes.getText().toString().trim();

        // 3. VALIDATION
        boolean isValid = true;

        if (TextUtils.isEmpty(farmName)) {
            tilFarmName.setError("Farm Name cannot be empty");
            isValid = false;
        }
        if (TextUtils.isEmpty(location)) {
            tilLocation.setError("Location cannot be empty");
            isValid = false;
        }

        double areaVal = 0;
        try {
            String cleanArea = area.replaceAll("[^0-9.]", "");
            areaVal = Double.parseDouble(cleanArea);
        } catch (NumberFormatException e) {
            areaVal = 0;
        }

        if (TextUtils.isEmpty(area)) {
            tilArea.setError("Area cannot be empty");
            isValid = false;
        } else if (areaVal <= 0) {
            tilArea.setError("Area must be greater than zero");
            isValid = false;
        }

        if (TextUtils.isEmpty(crop)) {
            tilCropType.setError("Crop Type must be selected");
            isValid = false;
        }

        if (!isValid) {
            showSnackbar("Please correct the errors in the form.");
            return;
        }

        // Show loading progress / disable save button
        btnSave.setEnabled(false);

        // Prevent duplicate farm names check
        FirebaseHelper.getInstance().checkDuplicateFarmName(farmName, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    btnSave.setEnabled(true);
                    tilFarmName.setError("Farm name already exists");
                    showSnackbar("Duplicate Farm Name! Please choose a unique name.");
                } else {
                    // Create farm object with default details
                    Farm newFarm = new Farm(
                            null,
                            farmName,
                            location,
                            area,
                            crop,
                            threshold,
                            schedule,
                            notes,
                            System.currentTimeMillis(),
                            "Healthy",
                            "Tomorrow 6:00 AM",
                            500
                    );

                    // Save Farm to Firebase
                    FirebaseHelper.getInstance().addFarm(newFarm, (error, ref) -> {
                        if (error == null) {
                            Toast.makeText(AddFarmActivity.this, "Farm Added Successfully", Toast.LENGTH_SHORT).show();
                            finishWithTransition();
                        } else {
                            btnSave.setEnabled(true);
                            showSnackbar("Error saving farm: " + error.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnSave.setEnabled(true);
                showSnackbar("Database error: " + error.getMessage());
            }
        });
    }

    private void showSnackbar(String message) {
        View contextView = findViewById(android.R.id.content);
        Snackbar.make(contextView, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.primary_green))
                .setTextColor(getResources().getColor(R.color.white))
                .show();
    }

    private void finishWithTransition() {
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
