package com.jo.agrisenseai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter that renders a list of {@link SensorHistory} entries
 * — one card per sensor reading stored in Firebase {@code history/}.
 *
 * <p>Call {@link #updateData(List)} to refresh the list with new data from
 * {@link FirebaseHelper.HistoryListener#onHistoryUpdate}.</p>
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy  HH:mm:ss", Locale.getDefault());

    private List<SensorHistory> dataList;

    public HistoryAdapter(List<SensorHistory> dataList) {
        this.dataList = dataList;
    }

    /**
     * Replaces the current dataset and notifies the RecyclerView to redraw.
     */
    public void updateData(List<SensorHistory> newData) {
        this.dataList = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SensorHistory entry = dataList.get(position);

        // Format the epoch timestamp into a human-readable date/time string.
        String formattedTime = DATE_FORMAT.format(new Date(entry.getTimestamp()));
        holder.timestampText.setText(formattedTime);

        holder.temperatureText.setText(
                String.format(Locale.getDefault(), "%.1f °C", entry.getTemperature()));
        holder.humidityText.setText(
                String.format(Locale.getDefault(), "%.1f %%", entry.getHumidity()));
        holder.soilMoistureText.setText(
                String.format(Locale.getDefault(), "%.0f", entry.getSoilMoisture()));

        // Show entry number (1 = newest at top, since list is already reversed by fragment).
        holder.entryIndexText.setText(String.format(Locale.getDefault(), "#%d", position + 1));
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

    // -----------------------------------------------------------------------

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView entryIndexText;
        TextView timestampText;
        TextView temperatureText;
        TextView humidityText;
        TextView soilMoistureText;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            entryIndexText    = itemView.findViewById(R.id.historyEntryIndex);
            timestampText     = itemView.findViewById(R.id.historyTimestamp);
            temperatureText   = itemView.findViewById(R.id.historyTemperature);
            humidityText      = itemView.findViewById(R.id.historyHumidity);
            soilMoistureText  = itemView.findViewById(R.id.historySoilMoisture);
        }
    }
}
