package com.jo.agrisenseai;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EditFarmActivity extends AppCompatActivity {

    private static final String[] CROP_OPTIONS     = {"Rice", "Wheat", "Cotton", "Tomato"};
    private static final String[] SCHEDULE_OPTIONS = {"Morning", "Afternoon", "Evening", "Automatic"};

    private String farmId;

    private TextInputLayout tilFarmName, tilLocation, tilArea, tilCropType;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_farm);

        farmId = getIntent().getStringExtra("farmId");
        if (farmId == null) {
            finish();
            return;
        }

        // Bind views
        tilFarmName    = findViewById(R.id.tilFarmName);
        tilLocation    = findViewById(R.id.tilLocation);
        tilArea        = findViewById(R.id.tilArea);
        tilCropType    = findViewById(R.id.tilCropType);
        etFarmName     = findViewById(R.id.etFarmName);
        etLocation     = findViewById(R.id.etLocation);
        etArea         = findViewById(R.id.etArea);
        etNotes        = findViewById(R.id.etNotes);
        actvCropType   = findViewById(R.id.actvCropType);
        tvMoistureValue = findViewById(R.id.tvMoistureValue);
        sliderMoisture = findViewById(R.id.sliderMoisture);
        spinnerSchedule = findViewById(R.id.spinnerSchedule);
        btnSave        = findViewById(R.id.btnSave);
        btnCancel      = findViewById(R.id.btnCancel);

        // Back and Cancel
        findViewById(R.id.btnBack).setOnClickListener(v -> finishWithTransition());
        btnCancel.setOnClickListener(v -> finishWithTransition());

        // Crop dropdown
        ArrayAdapter<String> cropAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, CROP_OPTIONS);
        actvCropType.setAdapter(cropAdapter);

        actvCropType.setOnItemClickListener((parent, view, position, id) -> {
            if (tilCropType != null) {
                tilCropType.setError(null);
            }
        });

        // Slider
        sliderMoisture.addOnChangeListener((slider, value, fromUser) ->
                tvMoistureValue.setText(String.valueOf((int) value)));

        // Schedule spinner
        ArrayAdapter<String> scheduleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SCHEDULE_OPTIONS);
        scheduleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSchedule.setAdapter(scheduleAdapter);

        // Error clearing
        setupErrorClearing();

        // Save
        btnSave.setOnClickListener(v -> attemptSave());

        // Pre-fill all fields by fetching the current farm once
        loadFarmData();
    }

    private void loadFarmData() {
        btnSave.setEnabled(false);
        FirebaseHelper.getInstance().getFarmOnce(farmId, new FirebaseHelper.FarmByIdListener() {
            @Override
            public void onFarmLoaded(Farm farm) {
                prefillForm(farm);
                btnSave.setEnabled(true);
            }

            @Override
            public void onFarmNotFound() {
                showSnackbar("Farm not found. Closing.");
                finishWithTransition();
            }
        });
    }

    private void prefillForm(Farm farm) {
        etFarmName.setText(farm.getFarmName());
        etLocation.setText(farm.getLocation());
        etArea.setText(farm.getTotalAcres());
        etNotes.setText(farm.getNotes());

        // Crop type
        String crop = farm.getCropType() != null ? farm.getCropType() : CROP_OPTIONS[0];
        actvCropType.setText(crop, false);

        // Slider
        float threshold = Math.max(0, Math.min(1000, farm.getMoistureThreshold()));
        sliderMoisture.setValue(threshold);
        tvMoistureValue.setText(String.valueOf((int) threshold));

        // Spinner
        String schedule = farm.getIrrigationSchedule();
        if (schedule != null) {
            int idx = Arrays.asList(SCHEDULE_OPTIONS).indexOf(schedule);
            if (idx >= 0) spinnerSchedule.setSelection(idx);
        }
    }

    private void attemptSave() {
        String farmName = etFarmName.getText() != null ? etFarmName.getText().toString().trim() : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        String area     = etArea.getText() != null ? etArea.getText().toString().trim() : "";
        String crop     = actvCropType.getText().toString();
        int threshold   = (int) sliderMoisture.getValue();
        String schedule = spinnerSchedule.getSelectedItem() != null
                ? spinnerSchedule.getSelectedItem().toString() : "Automatic";
        String notes    = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

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
            if (tilCropType != null) {
                tilCropType.setError("Crop Type must be selected");
            }
            isValid = false;
        }

        if (!isValid) {
            showSnackbar("Please correct the errors in the form.");
            return;
        }

        btnSave.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("farmName",          farmName);
        updates.put("location",          location);
        updates.put("totalAcres",        area);
        updates.put("cropType",          crop);
        updates.put("moistureThreshold", threshold);
        updates.put("irrigationSchedule", schedule);
        updates.put("notes",             notes);

        FirebaseHelper.getInstance().updateFarm(farmId, updates, (error, ref) -> {
            if (error == null) {
                showSnackbar("Farm Updated Successfully");
                // Short delay so Snackbar is visible before closing
                btnSave.postDelayed(this::finishWithTransition, 1000);
            } else {
                btnSave.setEnabled(true);
                showSnackbar("Error updating farm: " + error.getMessage());
            }
        });
    }

    private void setupErrorClearing() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
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

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.primary_green))
                .setTextColor(ContextCompat.getColor(this, R.color.white))
                .show();
    }

    private void finishWithTransition() {
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
