package com.example.proyectomovil.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectomovil.R;
import com.example.proyectomovil.domain.models.Reading;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private final List<Reading> items;

    public HistoryAdapter(List<Reading> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reading, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reading r = items.get(position);

        holder.txtTemp.setText(String.format("T. Ambiente:\n %.1f °C", r.getTemperature()));
        holder.txtHum.setText(String.format("H. Ambiente:\n %.1f %%", r.getHumidity()));
        holder.txtMq135.setText(String.format("Gases:\n %d ppm", r.getMq135()));
        holder.txtAire.setText(String.format("%s",
                r.getAirQualityStatus() != null ? r.getAirQualityStatus() : "--"));
        holder.txtTemp2.setText(String.format("T. Suelo:\n %.1f °C", r.getDs18b20Temp()));
        holder.txtSuelo.setText(String.format("H. Suelo:\n %d %%", r.getSoilMoisture()));
        holder.txtFecha.setText(r.getDate());
        holder.txtHora.setText(r.getTime());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTemp, txtHum, txtMq135, txtAire, txtTemp2, txtSuelo, txtFecha, txtHora;

        ViewHolder(@NonNull View v) {
            super(v);
            txtTemp = v.findViewById(R.id.txtTemp);
            txtHum = v.findViewById(R.id.txtHum);
            txtMq135 = v.findViewById(R.id.txtMq135);
            txtAire = v.findViewById(R.id.txtAire);
            txtTemp2 = v.findViewById(R.id.txtTemp2);
            txtSuelo = v.findViewById(R.id.txtSuelo);
            txtFecha = v.findViewById(R.id.txtFecha);
            txtHora = v.findViewById(R.id.txtHora);
        }
    }
}
