package com.jo.agrisenseai;

import android.content.Context;

import com.google.firebase.database.ValueEventListener;

/**
 * Watches the Firebase dashboard node and fires smart notifications when
 * the AI engine changes the riskLevel or pumpStatus.
 *
 * Triggered once from {@link MainActivity#onCreate} and kept alive for
 * the app session. The listener is intentionally not removed on Activity
 * stop so background Firebase changes still generate tray notifications
 * (simulating future cloud-function behaviour).
 *
 * Future hardware: ESP32 writes sensorData → AIEngine rewrites dashboard →
 * this watcher fires the appropriate notification automatically.
 * Future voice: read the notification aloud via TTSManager after posting.
 */
public final class AINotificationWatcher {

    private static String lastRiskLevel = null;
    private static String lastPumpStatus = null;

    private AINotificationWatcher() {}

    public static void start(Context context) {
        FirebaseHelper.getInstance().listenDashboard(data -> {
            if (data == null) return;

            String riskLevel  = data.getRiskLevel();
            String pumpStatus = data.getPumpStatus();

            // Avoid duplicate notifications on first load
            if (lastRiskLevel == null) {
                lastRiskLevel  = riskLevel;
                lastPumpStatus = pumpStatus;
                return;
            }

            // Fire notification when risk level escalates
            if (riskLevel != null && !riskLevel.equals(lastRiskLevel)) {
                switch (riskLevel) {
                    case AIEngine.RISK_HIGH:
                        NotificationHelper.notifyHighRisk(context);
                        NotificationHelper.notifyWaterRequired(context);
                        break;
                    case AIEngine.RISK_MEDIUM:
                        NotificationHelper.notifyWaterRequired(context);
                        break;
                    case AIEngine.RISK_LOW:
                        NotificationHelper.notifyFarmHealthy(context);
                        break;
                }
                lastRiskLevel = riskLevel;
            }

            // Fire notification when pump turns ON
            if (pumpStatus != null && !pumpStatus.equals(lastPumpStatus)) {
                if (FirebaseHelper.PUMP_ON.equalsIgnoreCase(pumpStatus)) {
                    NotificationHelper.notifyPumpActivated(context);
                }
                lastPumpStatus = pumpStatus;
            }
        });
    }
}
