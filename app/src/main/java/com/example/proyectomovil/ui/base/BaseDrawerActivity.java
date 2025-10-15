package com.example.proyectomovil.ui.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.proyectomovil.R;
import com.example.proyectomovil.Reports;
import com.example.proyectomovil.Settings;
import com.example.proyectomovil.ui.history.HistoryActivity;
import com.example.proyectomovil.ui.main.MainActivity;
import com.example.proyectomovil.ui.materials.MaterialsActivity;
import com.example.proyectomovil.ui.user.EditUser;
import com.google.android.material.navigation.NavigationView;

/**
 * BaseDrawerActivity
 * --------------------
 * Clase base para Activities con menú lateral (Drawer)
 * Permite reutilizar el Navigation Drawer y pasar el USER_ID entre pantallas.
 */
public abstract class BaseDrawerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected ActionBarDrawerToggle toggle;

    /** 🔹 Variable global para mantener el ID del usuario actual */
    protected int currentUserId = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Nuevo manejo del botón "Atrás" para DrawerLayout
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    BaseDrawerActivity.super.onBackPressed();
                }
            }
        });
    }

    /**
     * 🔹 Carga la estructura principal del Drawer y el layout específico de la pantalla
     */
    protected void setContentWithDrawer(@LayoutRes int layoutResID) {
        setContentView(R.layout.activity_base_drawer);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // ✅ Guardamos el USER_ID recibido desde el intent
        currentUserId = getIntent().getIntExtra("USER_ID", -1);

        // Inflamos el contenido específico de la pantalla (por ejemplo activity_history.xml)
        View content = getLayoutInflater().inflate(layoutResID, findViewById(R.id.content_frame), false);
        ((android.widget.FrameLayout) findViewById(R.id.content_frame)).addView(content);

        navigationView.setNavigationItemSelectedListener(this);

        // Configuración del Drawer Toggle (icono hamburguesa)
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Mostrar icono en la barra superior
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // Personalización del header (nombre, correo)
        View headerView = navigationView.getHeaderView(0);
        TextView tvName = headerView.findViewById(R.id.tvHeaderName);
        TextView tvEmail = headerView.findViewById(R.id.tvHeaderEmail);
        tvName.setText("Usuario Ejemplo");
        tvEmail.setText("correo@example.com");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }

    /**
     * 🔹 Maneja la navegación del Drawer
     * Pasa automáticamente el USER_ID a todas las Activities.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;

        if (id == R.id.nav_home) {
            intent = new Intent(this, MainActivity.class);
        } else if (id == R.id.nav_historial) {
            intent = new Intent(this, HistoryActivity.class);
        } else if (id == R.id.nav_materials) {
            intent = new Intent(this, MaterialsActivity.class);
        } else if (id == R.id.nav_customers) {
            intent = new Intent(this, EditUser.class);
        } else if (id == R.id.nav_reports) {
            intent = new Intent(this, Reports.class);
        }else if (id == R.id.nav_settings) {
            intent = new Intent(this, Settings.class);
        }
        // 🔹 NUEVO: Cerrar sesión
        else if (id == R.id.nav_log_out) {
            logoutUser();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        // ✅ Pasar siempre el USER_ID al navegar
        if (intent != null) {
            if (currentUserId != -1) {
                intent.putExtra("USER_ID", currentUserId);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    private void logoutUser() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply();

                    Intent intent = new Intent(this, com.example.proyectomovil.ui.login.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

}
