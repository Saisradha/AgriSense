package com.jo.agrisenseai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * LanguageSelectionActivity — First screen in the onboarding flow.
 *
 * Displays a grid of 8 Indian language options. User selects one,
 * the selection is persisted via LanguageManager, and the app
 * locale is applied before navigating to PhoneLoginActivity.
 */
public class LanguageSelectionActivity extends AppCompatActivity {

    private int selectedIndex = 0; // Default to English
    private MaterialCardView[] languageCards;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);

        GridLayout grid = findViewById(R.id.languageGrid);
        MaterialButton btnContinue = findViewById(R.id.btnContinue);

        selectedIndex = LanguageManager.getSavedLocaleIndex(this);
        languageCards = new MaterialCardView[LanguageManager.LOCALE_CODES.length];

        // Build language cards dynamically
        for (int i = 0; i < LanguageManager.LOCALE_CODES.length; i++) {
            final int index = i;

            MaterialCardView card = new MaterialCardView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 2, 1, 1f);
            params.rowSpec = GridLayout.spec(i / 2);
            params.setMargins(8, 8, 8, 8);
            card.setLayoutParams(params);
            
            // Make the outer card completely transparent
            card.setCardElevation(0f);
            card.setRadius(getResources().getDimensionPixelSize(R.dimen.radius_card_sm));
            card.setStrokeWidth(0);
            card.setStrokeColor(android.graphics.Color.TRANSPARENT);
            card.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
            card.setClickable(true);
            card.setFocusable(true);
            card.setCheckable(true);

            // Container layout to hold text and draw glassmorphic background
            android.widget.LinearLayout container = new android.widget.LinearLayout(this);
            container.setOrientation(android.widget.LinearLayout.VERTICAL);
            container.setGravity(Gravity.CENTER);
            container.setPadding(24, 24, 24, 24);

            // Language name text
            TextView label = new TextView(this);
            label.setText(LanguageManager.DISPLAY_NAMES[i]);
            label.setTextSize(18f);
            label.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
            label.setGravity(Gravity.CENTER);
            label.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            container.addView(label);

            // Subtitle: English name for non-English languages
            String subtitle = getEnglishName(i);
            if (subtitle != null) {
                label.setLineSpacing(4f, 1f);
                TextView sub = new TextView(this);
                sub.setText(subtitle);
                sub.setTextSize(12f);
                sub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                sub.setGravity(Gravity.CENTER);
                container.addView(sub);
            }
            
            card.addView(container);

            card.setOnClickListener(v -> {
                selectedIndex = index;
                updateCardSelection();
            });

            languageCards[i] = card;
            grid.addView(card);
        }

        updateCardSelection();

        btnContinue.setOnClickListener(v -> {
            String code = LanguageManager.LOCALE_CODES[selectedIndex];
            LanguageManager.saveLanguage(this, code);

            Intent intent = new Intent(this, EmailLoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    private void updateCardSelection() {
        for (int i = 0; i < languageCards.length; i++) {
            MaterialCardView card = languageCards[i];
            android.widget.LinearLayout container = (android.widget.LinearLayout) card.getChildAt(0);
            if (container != null) {
                if (i == selectedIndex) {
                    container.setBackgroundResource(R.drawable.bg_language_card_selected);
                    card.setChecked(true);
                } else {
                    container.setBackgroundResource(R.drawable.bg_language_card);
                    card.setChecked(false);
                }
            }
        }
    }

    private String getEnglishName(int index) {
        switch (index) {
            case 1: return "Telugu";
            case 2: return "Hindi";
            case 3: return "Tamil";
            case 4: return "Kannada";
            case 5: return "Malayalam";
            case 6: return "Marathi";
            case 7: return "Bengali";
            default: return null;
        }
    }
}
