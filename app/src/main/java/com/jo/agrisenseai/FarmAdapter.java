package com.jo.agrisenseai;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

public class FarmAdapter extends RecyclerView.Adapter<FarmAdapter.FarmViewHolder> {

    public interface OnFarmClickListener {
        void onFarmClick(View view, String farmId);
    }

    private final Context context;
    private final List<Farm> farmList;
    private OnFarmClickListener clickListener;

    public FarmAdapter(Context context, List<Farm> farmList) {
        this.context = context;
        this.farmList = farmList;
    }

    public void setOnFarmClickListener(OnFarmClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public FarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_farm, parent, false);
        return new FarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FarmViewHolder holder, int position) {
        Farm farm = farmList.get(position);

        holder.farmNameText.setText(farm.getFarmName());
        holder.farmLocationText.setText(farm.getLocation());
        holder.farmAreaText.setText(farm.getTotalAcres() + " Acres");
        holder.farmCropText.setText(farm.getCropType());
        holder.farmMoistureText.setText(String.valueOf(farm.getSoilMoisture()) + "%");
        holder.farmNextWateringText.setText(farm.getNextWatering());

        // Apply health status badge styling
        String status = farm.getHealthStatus();
        if (status == null) {
            status = "Healthy";
        }

        int textColorRes;
        int bgColorRes;
        int stripColorRes;

        switch (status) {
            case "Critical":
            case "High":
                textColorRes = R.color.status_critical;
                bgColorRes = R.color.icon_bg_red;
                stripColorRes = R.color.status_critical;
                break;
            case "Medium":
            case "Water Required":
            case "Monitor":
                textColorRes = R.color.status_medium;
                bgColorRes = R.color.icon_bg_orange;
                stripColorRes = R.color.status_medium;
                break;
            default: // "Healthy" or others
                textColorRes = R.color.status_healthy;
                bgColorRes = R.color.icon_bg_green;
                stripColorRes = R.color.status_healthy;
                break;
        }

        holder.farmStatusBadge.setText(status);
        holder.farmStatusBadge.setTextColor(ContextCompat.getColor(context, textColorRes));
        holder.farmStatusBadge.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(context, bgColorRes)));

        holder.farmStatusStrip.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(context, stripColorRes)));

        // Setup Quick Pump Switch status
        setupPumpSwitchListener(holder, farm);

        // Wire card click → forward farmId
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null && farm.getFarmId() != null) {
                clickListener.onFarmClick(holder.itemView, farm.getFarmId());
            }
        });
    }

    private void setupPumpSwitchListener(FarmViewHolder holder, Farm farm) {
        holder.switchFarmPump.setOnCheckedChangeListener(null);
        holder.switchFarmPump.setChecked("ON".equalsIgnoreCase(farm.getPumpStatus()));
        holder.switchFarmPump.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String newStatus = isChecked ? "ON" : "OFF";
            buttonView.setEnabled(false);
            FirebaseHelper.getInstance().setPumpStatus(farm.getFarmId(), newStatus, (error, ref) -> {
                buttonView.setEnabled(true);
                if (error == null) {
                    farm.setPumpStatus(newStatus);
                    Toast.makeText(context, farm.getFarmName() + " pump switched " + newStatus, Toast.LENGTH_SHORT).show();
                } else {
                    setupPumpSwitchListener(holder, farm);
                    Toast.makeText(context, "Failed to control pump: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return farmList.size();
    }

    public void updateList(List<Farm> newFarms) {
        farmList.clear();
        farmList.addAll(newFarms);
        notifyDataSetChanged();
    }

    public static class FarmViewHolder extends RecyclerView.ViewHolder {
        final View farmStatusStrip;
        final TextView farmNameText;
        final TextView farmStatusBadge;
        final TextView farmLocationText;
        final TextView farmAreaText;
        final TextView farmCropText;
        final TextView farmMoistureText;
        final TextView farmNextWateringText;
        final SwitchMaterial switchFarmPump;

        public FarmViewHolder(@NonNull View itemView) {
            super(itemView);
            farmStatusStrip = itemView.findViewById(R.id.farmStatusStrip);
            farmNameText = itemView.findViewById(R.id.farmNameText);
            farmStatusBadge = itemView.findViewById(R.id.farmStatusBadge);
            farmLocationText = itemView.findViewById(R.id.farmLocationText);
            farmAreaText = itemView.findViewById(R.id.farmAreaText);
            farmCropText = itemView.findViewById(R.id.farmCropText);
            farmMoistureText = itemView.findViewById(R.id.farmMoistureText);
            farmNextWateringText = itemView.findViewById(R.id.farmNextWateringText);
            switchFarmPump = itemView.findViewById(R.id.switchFarmPump);
        }
    }
}
