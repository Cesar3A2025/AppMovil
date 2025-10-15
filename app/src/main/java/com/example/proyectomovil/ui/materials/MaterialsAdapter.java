package com.example.proyectomovil.ui.materials;

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
import com.example.proyectomovil.R;
import com.example.proyectomovil.data.api.ApiRoutes;
import com.example.proyectomovil.domain.models.Materials;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class MaterialsAdapter extends RecyclerView.Adapter<MaterialsAdapter.ViewHolder> {
    private final Context context;
    private List<Materials> items;

    public MaterialsAdapter(Context context, List<Materials> items) {
        this.context = context;
        this.items = (items != null) ? items : new ArrayList<>();
    }

    @NonNull
    @Override
    public MaterialsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_material, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MaterialsAdapter.ViewHolder holder, int position) {
        Materials m = items.get(position);

        holder.txtName.setText(m.getName());
        holder.txtDesc.setText(m.getDescription());
        holder.chipClasif.setText(m.getClasification());
        holder.chipAptitud.setText(m.getAptitude());
        holder.chipCategoria.setText(m.getTypeCategory());

        // Construir URL de imagen
        String path = m.getImage(); // m.getImage() debe contener lo que devuelve la API en image_url (relativa o absoluta)
        String url = null;
        if (path != null && !path.isEmpty()) {
            url = path.startsWith("http") ? path : ApiRoutes.BASE_IMAGES + (path.startsWith("/") ? "" : "/") + path;
        }

        Glide.with(holder.itemView.getContext())
                .load(url)
                .placeholder(R.drawable.home) // ajusta tu placeholder
                .error(R.drawable.home)
                .into(holder.img);

        holder.itemView.setOnClickListener(v -> {
            String clasif = m.getClasification() != null ? m.getClasification().toLowerCase() : "";

            Intent intent;
            switch (clasif) {
                case "verde":
                    intent = new Intent(context, GreenActivity.class);
                    break;
                case "marron":
                    intent = new Intent(context, BrownActivity.class);
                    break;
                case "no_compostable":
                    intent = new Intent(context, NoCompostableActivity.class);
                    break;
                default:
                    // fallback: abrir detalle genérico
                    intent = new Intent(context, MaterialDetailActivity.class);
                    break;
            }

            // Pasamos datos para que la Activity pueda mostrar info del material seleccionado
            intent.putExtra("id", m.getId());
            intent.putExtra("name", m.getName());
            intent.putExtra("description", m.getDescription());
            intent.putExtra("image", m.getImage()); // ruta relativa o absoluta
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
