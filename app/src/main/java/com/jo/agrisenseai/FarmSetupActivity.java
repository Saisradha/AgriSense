package com.jo.agrisenseai;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

/**
 * FarmSetupActivity — A 3-step setup wizard using ViewPager2 to configure the first farm.
 */
public class FarmSetupActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearProgressIndicator setupProgress;
    private MaterialButton btnBack, btnNext;
    private ProgressBar progressSetup;
    private TextView tvSkip, tvSetupError;
    private LinearLayout dotsLayout;

    private SetupPagerAdapter mAdapter;
    private FirebaseAuth mAuth;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farm_setup);

        mAuth = FirebaseAuth.getInstance();

        // Bind Views
        viewPager = findViewById(R.id.viewPagerFarmSetup);
        setupProgress = findViewById(R.id.setupProgress);
        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);
        progressSetup = findViewById(R.id.progressSetup);
        tvSkip = findViewById(R.id.tvSkip);
        tvSetupError = findViewById(R.id.tvSetupError);
        dotsLayout = findViewById(R.id.dotsLayout);

        // Configure ViewPager
        mAdapter = new SetupPagerAdapter();
        viewPager.setAdapter(mAdapter);
        viewPager.setOffscreenPageLimit(2); // Keep all 3 pages in memory
        viewPager.setUserInputEnabled(false); // Disable swipe to force button validation

        // Configure Dots Indicator
        setupDotsIndicator();

        // Setup Page Change Listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateProgress(position);
            }
        });

        // Setup Button Listeners
        btnBack.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current > 0) {
                viewPager.setCurrentItem(current - 1, true);
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (validatePage(current)) {
                if (current < 2) {
                    viewPager.setCurrentItem(current + 1, true);
                } else {
                    saveFarmDetails();
                }
            }
        });

        tvSkip.setOnClickListener(v -> navigateToDeviceCheck());
    }

    private void setupDotsIndicator() {
        ImageView[] dots = new ImageView[3];
        dotsLayout.removeAllViews();
        for (int i = 0; i < 3; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageResource(android.R.drawable.presence_invisible); // placeholder size
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dots[i].setLayoutParams(params);

            // Draw active/inactive dot helper
            View dotView = new View(this);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(24, 24);
            dotParams.setMargins(8, 0, 8, 0);
            dotView.setLayoutParams(dotParams);
            dotView.setBackgroundResource(R.drawable.bg_language_card);
            dotsLayout.addView(dotView);
        }
        updateDots(0);
    }

    private void updateDots(int position) {
        for (int i = 0; i < dotsLayout.getChildCount(); i++) {
            View dot = dotsLayout.getChildAt(i);
            if (i == position) {
                dot.setBackgroundColor(getResources().getColor(R.color.primary_green));
            } else {
                dot.setBackgroundColor(getResources().getColor(R.color.divider));
            }
        }
    }

    private void updateProgress(int position) {
        updateDots(position);
        tvSetupError.setVisibility(View.GONE);

        if (position == 0) {
            setupProgress.setProgress(33);
            btnBack.setVisibility(View.INVISIBLE);
            btnNext.setText(R.string.btn_next);
        } else if (position == 1) {
            setupProgress.setProgress(66);
            btnBack.setVisibility(View.VISIBLE);
            btnNext.setText(R.string.btn_next);
        } else {
            setupProgress.setProgress(100);
            btnBack.setVisibility(View.VISIBLE);
            btnNext.setText(R.string.btn_get_started);
        }
    }

    private boolean validatePage(int page) {
        tvSetupError.setVisibility(View.GONE);
        View pageView = mAdapter.getView(page);
        if (pageView == null) return false;

        if (page == 0) {
            EditText etFarmName = pageView.findViewById(R.id.etFarmName);
            EditText etFarmArea = pageView.findViewById(R.id.etFarmArea);
            String name = etFarmName.getText() != null ? etFarmName.getText().toString().trim() : "";
            String area = etFarmArea.getText() != null ? etFarmArea.getText().toString().trim() : "";

            if (TextUtils.isEmpty(name)) {
                tvSetupError.setText("Field Name is required");
                tvSetupError.setVisibility(View.VISIBLE);
                return false;
            }
            if (TextUtils.isEmpty(area)) {
                tvSetupError.setText("Field Area is required");
                tvSetupError.setVisibility(View.VISIBLE);
                return false;
            }
        } else if (page == 1) {
            EditText etCropType = pageView.findViewById(R.id.etCropType);
            String crop = etCropType.getText() != null ? etCropType.getText().toString().trim() : "";

            if (TextUtils.isEmpty(crop)) {
                tvSetupError.setText("Crop Type is required");
                tvSetupError.setVisibility(View.VISIBLE);
                return false;
            }
        }
        return true;
    }

    private void saveFarmDetails() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            return;
        }

        View view1 = mAdapter.getView(0);
        View view2 = mAdapter.getView(1);
        View view3 = mAdapter.getView(2);

        if (view1 == null || view2 == null || view3 == null) {
            tvSetupError.setText("Setup forms are not properly loaded.");
            tvSetupError.setVisibility(View.VISIBLE);
            return;
        }

        // Get inputs
        String name = ((EditText) view1.findViewById(R.id.etFarmName)).getText().toString().trim();
        String location = ((EditText) view1.findViewById(R.id.etFarmLocation)).getText().toString().trim();
        String area = ((EditText) view1.findViewById(R.id.etFarmArea)).getText().toString().trim();
        String soilType = ((EditText) view1.findViewById(R.id.etSoilType)).getText().toString().trim();

        String cropType = ((EditText) view2.findViewById(R.id.etCropType)).getText().toString().trim();
        String plantingDate = ((EditText) view2.findViewById(R.id.etPlantingDate)).getText().toString().trim();

        String pumpId = ((EditText) view3.findViewById(R.id.etPumpId)).getText().toString().trim();
        String thresholdStr = ((EditText) view3.findViewById(R.id.etMoistureThreshold)).getText().toString().trim();

        int moistureThreshold = 45;
        if (!TextUtils.isEmpty(thresholdStr)) {
            try {
                moistureThreshold = Integer.parseInt(thresholdStr);
            } catch (NumberFormatException ignored) {}
        }

        btnNext.setVisibility(View.GONE);
        progressSetup.setVisibility(View.VISIBLE);
        tvSkip.setEnabled(false);

        // Fallbacks
        if (TextUtils.isEmpty(location)) location = "Main Plot";
        if (TextUtils.isEmpty(soilType)) soilType = "Clay Loam";
        if (TextUtils.isEmpty(plantingDate)) plantingDate = "Not Specified";
        if (TextUtils.isEmpty(pumpId)) pumpId = "DEMO_PUMP_" + (int)(Math.random() * 100);

        // Build Farm Model
        Farm farm = new Farm(
                null, // will be generated by push()
                name,
                location,
                area,
                cropType,
                moistureThreshold,
                "Auto (Daily)", // default irrigation schedule
                "Soil Type: " + soilType + ", Planting Date: " + plantingDate, // notes
                System.currentTimeMillis(),
                "Healthy", // health status
                "Calculating...", // next watering
                60, // initial simulated soil moisture
                user.getUid(),
                soilType,
                plantingDate,
                "OFF" // pumpStatus
        );

        FirebaseHelper.getInstance().addFarmForUser(user.getUid(), farm, (error, ref) -> {
            btnNext.setVisibility(View.VISIBLE);
            progressSetup.setVisibility(View.GONE);
            tvSkip.setEnabled(true);

            if (error == null) {
                navigateToDeviceCheck();
            } else {
                tvSetupError.setText("Failed to save farm details: " + error.getMessage());
                tvSetupError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void navigateToDeviceCheck() {
        Intent intent = new Intent(FarmSetupActivity.this, DeviceCheckActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // ── Setup ViewPager Adapter ───────────────────────────────────────────
    private class SetupPagerAdapter extends RecyclerView.Adapter<SetupPagerAdapter.PageViewHolder> {

        private final View[] views = new View[3];

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layoutId;
            if (viewType == 0) {
                layoutId = R.layout.item_farm_setup_page1;
            } else if (viewType == 1) {
                layoutId = R.layout.item_farm_setup_page2;
            } else {
                layoutId = R.layout.item_farm_setup_page3;
            }

            View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            views[position] = holder.itemView;

            if (position == 1) {
                // Attach DatePickerDialog to Planting Date input field
                EditText etPlantingDate = holder.itemView.findViewById(R.id.etPlantingDate);
                etPlantingDate.setOnClickListener(v -> {
                    Calendar calendar = Calendar.getInstance();
                    int year = calendar.get(Calendar.YEAR);
                    int month = calendar.get(Calendar.MONTH);
                    int day = calendar.get(Calendar.DAY_OF_MONTH);

                    DatePickerDialog pickerDialog = new DatePickerDialog(
                            v.getContext(),
                            (view, y, m, d) -> etPlantingDate.setText(String.format("%04d-%02d-%02d", y, m + 1, d)),
                            year, month, day
                    );
                    pickerDialog.show();
                });
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return 3;
        }

        public View getView(int position) {
            return views[position];
        }

        class PageViewHolder extends RecyclerView.ViewHolder {
            public PageViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
