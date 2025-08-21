package com.example.proyectomovil.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.proyectomovil.MaterialDetailActivity;
import com.example.proyectomovil.Models.Materials;
import com.example.proyectomovil.R;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class MaterialsAdapter extends RecyclerView.Adapter<MaterialsAdapter.ViewHolder>{
    private final Context context;
    private List<Materials> items;
    private final String baseImagesUrl;

    public MaterialsAdapter(Context context, List<Materials> items, String baseImagesUrl) {
        this.context = context;
        this.items = (items != null) ? items : new ArrayList<>();
        this.baseImagesUrl = (baseImagesUrl != null) ? baseImagesUrl : "";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_material, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Materials m = items.get(position);
        holder.txtName.setText(m.getName());
        holder.txtDesc.setText(m.getDescription());
        holder.chipClasif.setText(m.getClasification());
        holder.chipAptitud.setText(m.getAptitude());
        holder.chipCategoria.setText(m.getTypeCategory());

        String path = m.getImage();
        String url = (path != null && !path.isEmpty())
                ? (path.startsWith("http") ? path : concatUrl(baseImagesUrl, path))
                : null;

        Glide.with(holder.itemView.getContext())
                .load(url)
                .placeholder(R.drawable.home)
                .error(R.drawable.home)
                .into(holder.img);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MaterialDetailActivity.class);
            intent.putExtra("id", m.getId());
            intent.putExtra("name", m.getName());
            intent.putExtra("clasification", m.getClasification());
            intent.putExtra("aptitude", m.getAptitude());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<Materials> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    private String concatUrl(String base, String rel) {
        if (base == null) return rel;
        if (base.endsWith("/") && rel.startsWith("/")) return base + rel.substring(1);
        if (!base.endsWith("/") && !rel.startsWith("/")) return base + "/" + rel;
        return base + rel;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView txtName, txtDesc;
        Chip chipClasif, chipAptitud, chipCategoria;

        ViewHolder(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgMaterial);
            txtName = v.findViewById(R.id.txtName);
            txtDesc = v.findViewById(R.id.txtDescription);
            chipClasif = v.findViewById(R.id.chipClasif);
            chipAptitud = v.findViewById(R.id.chipAptitud);
            chipCategoria = v.findViewById(R.id.chipCategoria);
        }
    }
}
