package com.jo.agrisenseai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    private final List<NotificationModel> items;
    private final Context context;

    public NotificationAdapter(Context context, List<NotificationModel> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_notification, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NotificationModel n = items.get(position);

        h.title.setText(n.getTitle());
        h.message.setText(n.getMessage());
        h.timestamp.setText(formatTimestamp(n.getTimestamp()));

        h.unreadBadge.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);

        applyTypeStyle(h, n.getType());
    }

    @Override
    public int getItemCount() { return items.size(); }

    public void updateItems(List<NotificationModel> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void applyTypeStyle(VH h, String type) {
        int iconRes;
        int tintRes;
        int bgTintRes;

        if (type == null) type = NotificationModel.TYPE_SYSTEM;

        switch (type) {
            case NotificationModel.TYPE_WATER:
                iconRes  = R.drawable.ic_water_drop;
                tintRes  = R.color.accent_blue;
                bgTintRes = R.color.icon_bg_blue;
                break;
            case NotificationModel.TYPE_PUMP:
                iconRes  = R.drawable.ic_pump;
                tintRes  = R.color.primary_green;
                bgTintRes = R.color.icon_bg_green;
                break;
            case NotificationModel.TYPE_HEALTHY:
                iconRes  = R.drawable.ic_leaf;
                tintRes  = R.color.status_healthy;
                bgTintRes = R.color.icon_bg_green;
                break;
            case NotificationModel.TYPE_RISK:
                iconRes  = R.drawable.ic_trending_up;
                tintRes  = R.color.status_critical;
                bgTintRes = R.color.icon_bg_red;
                break;
            default: // system
                iconRes  = R.drawable.ic_tune;
                tintRes  = R.color.text_secondary;
                bgTintRes = R.color.icon_bg_teal;
                break;
        }

        h.typeIcon.setImageResource(iconRes);
        h.typeIcon.setColorFilter(ContextCompat.getColor(context, tintRes));
        h.typeIcon.setBackgroundTintList(
                ContextCompat.getColorStateList(context, bgTintRes));
    }

    private String formatTimestamp(long millis) {
        if (millis <= 0) return context.getString(R.string.notif_just_now);
        long diff = System.currentTimeMillis() - millis;
        if (diff < 60_000) return context.getString(R.string.notif_just_now);
        if (diff < 3_600_000) return (diff / 60_000) + " min ago";
        if (diff < 86_400_000) return (diff / 3_600_000) + " hr ago";
        return new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date(millis));
    }

    // ---------------------------------------------------------------
    // ViewHolder
    // ---------------------------------------------------------------

    static class VH extends RecyclerView.ViewHolder {
        ImageView typeIcon;
        TextView title, message, timestamp, unreadBadge;

        VH(View v) {
            super(v);
            typeIcon     = v.findViewById(R.id.notifTypeIcon);
            title        = v.findViewById(R.id.notifTitle);
            message      = v.findViewById(R.id.notifMessage);
            timestamp    = v.findViewById(R.id.notifTimestamp);
            unreadBadge  = v.findViewById(R.id.notifUnreadBadge);
        }
    }
}
