package com.example.proyectomovil.ui.base;

import android.content.Intent;
import android.content.SharedPreferences;

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
import com.example.proyectomovil.ui.login.LoginActivity;

import com.google.android.material.navigation.NavigationView;

public abstract class BaseDrawerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected ActionBarDrawerToggle toggle;

    protected int currentUserId = -1;
    protected String currentUserName = "";
    protected String currentUserEmail = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    protected void setContentWithDrawer(@LayoutRes int layoutResID) {
        setContentView(R.layout.activity_base_drawer);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("USER_ID", -1);
        currentUserName = prefs.getString("USER_NAME", "Usuario");
        currentUserEmail = prefs.getString("USER_EMAIL", "correo@example.com");

        View content = getLayoutInflater().inflate(layoutResID, findViewById(R.id.content_frame), false);
        ((android.widget.FrameLayout) findViewById(R.id.content_frame)).addView(content);

        navigationView.setNavigationItemSelectedListener(this);

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        View headerView = navigationView.getHeaderView(0);
        TextView tvName = headerView.findViewById(R.id.tvHeaderName);
        TextView tvEmail = headerView.findViewById(R.id.tvHeaderEmail);

        tvName.setText(currentUserName);
        tvEmail.setText(currentUserEmail);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }

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
        } else if (id == R.id.nav_settings) {
            intent = new Intent(this, Settings.class);
        } else if (id == R.id.nav_log_out) {
            logoutUser();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (intent != null) {
            intent.putExtra("USER_ID", currentUserId);
            intent.putExtra("USER_NAME", currentUserName);
            intent.putExtra("USER_EMAIL", currentUserEmail);
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

                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
