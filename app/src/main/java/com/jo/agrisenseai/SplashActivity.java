package com.jo.agrisenseai;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * SplashActivity — Animated entry point for the application.
 *
 * Displays a premium green gradient splash with fade-in logo and text animations.
 * After 2.5 seconds, checks Firebase Auth state:
 *   - If user is signed in → check profile → MainActivity
 *   - If not signed in → LanguageSelectionActivity
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2500L;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animate splash elements
        ImageView icon = findViewById(R.id.splashIcon);
        TextView appName = findViewById(R.id.splashAppName);
        TextView tagline = findViewById(R.id.splashTagline);

        // Fade-in + scale animations
        ObjectAnimator iconFade = ObjectAnimator.ofFloat(icon, View.ALPHA, 0f, 1f);
        ObjectAnimator iconScaleX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 0.5f, 1f);
        ObjectAnimator iconScaleY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 0.5f, 1f);
        iconFade.setDuration(800);
        iconScaleX.setDuration(800);
        iconScaleY.setDuration(800);
        iconScaleX.setInterpolator(new OvershootInterpolator(1.5f));
        iconScaleY.setInterpolator(new OvershootInterpolator(1.5f));

        ObjectAnimator nameFade = ObjectAnimator.ofFloat(appName, View.ALPHA, 0f, 1f);
        ObjectAnimator nameSlide = ObjectAnimator.ofFloat(appName, View.TRANSLATION_Y, 40f, 0f);
        nameFade.setDuration(600);
        nameSlide.setDuration(600);
        nameFade.setStartDelay(400);
        nameSlide.setStartDelay(400);

        ObjectAnimator tagFade = ObjectAnimator.ofFloat(tagline, View.ALPHA, 0f, 1f);
        ObjectAnimator tagSlide = ObjectAnimator.ofFloat(tagline, View.TRANSLATION_Y, 30f, 0f);
        tagFade.setDuration(500);
        tagSlide.setDuration(500);
        tagFade.setStartDelay(700);
        tagSlide.setStartDelay(700);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(iconFade, iconScaleX, iconScaleY, nameFade, nameSlide, tagFade, tagSlide);
        animSet.start();

        // Navigate after delay
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, SPLASH_DELAY);
    }

    private void navigateNext() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // User is signed in — check if profile exists
            FirebaseHelper.getInstance().getUserProfile(user.getUid(), profile -> {
                Intent intent;
                if (profile != null && profile.getName() != null && !profile.getName().isEmpty()) {
                    // Profile exists → go to main app
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    // Signed in but no profile → complete registration
                    intent = new Intent(SplashActivity.this, FarmerRegistrationActivity.class);
                }
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            });
        } else {
            // Not signed in → language selection → login flow
            Intent intent = new Intent(this, LanguageSelectionActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    }
}
