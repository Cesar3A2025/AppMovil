package com.example.proyectomovil.ui.materials;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.proyectomovil.R;

public class GreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_green);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Extras (pasados por el adapter) - Solo el nombre si es necesario
        String name = getIntent().getStringExtra("name");


        // Imagen LOCAL - ya está configurada en el XML con android:src
        // No necesitamos cargarla desde la API
        ImageView headerImage = findViewById(R.id.headerImage);

        // Si quieres usar Glide para optimizar la imagen local:
        Glide.with(this)
                .load(R.drawable.materiales_verdes)
                .into(headerImage);
    }
}