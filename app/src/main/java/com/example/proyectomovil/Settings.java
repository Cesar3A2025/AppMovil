package com.example.proyectomovil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.proyectomovil.ui.base.BaseDrawerActivity;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class Settings extends BaseDrawerActivity {

    private RadioGroup rgFases;
    private Button btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Usamos la función del BaseDrawerActivity para inflar el layout con Drawer
        setContentWithDrawer(R.layout.activity_settings);

        // EdgeToEdge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rgFases = findViewById(R.id.rgFases);
        btnGuardar = findViewById(R.id.btnGuardar);

        btnGuardar.setOnClickListener(v -> {
            int selectedPhase = 0;
            int checkedId = rgFases.getCheckedRadioButtonId();
            if (checkedId == R.id.rbInicial) selectedPhase = 1;
            else if (checkedId == R.id.rbMedia) selectedPhase = 2;
            else if (checkedId == R.id.rbFinal) selectedPhase = 3;

            if (selectedPhase != 0) {
                if (currentUserId != -1) {
                    updateTypeAlert(currentUserId, selectedPhase);
                } else {
                    Toast.makeText(this, "Error: Usuario no válido", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Seleccione una fase", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTypeAlert(int userId, int typeAlert) {
        OkHttpClient client = new OkHttpClient();
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("user_id", userId);
            jsonBody.put("type_alert", typeAlert);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url("https://compos.alwaysdata.net/api/updateTypeAlert")
                .post(body)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(Settings.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body() != null ? response.body().string() : "";
                int code = response.code();
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(resp);
                        if (json.getBoolean("success")) {
                            Toast.makeText(Settings.this, "Fase actualizada correctamente", Toast.LENGTH_SHORT).show();

                            // ---------- REDIRECCIÓN AL MAINACTIVITY ----------
                            Intent intent = new Intent(Settings.this, com.example.proyectomovil.ui.main.MainActivity.class);
                            intent.putExtra("USER_ID", userId);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();

                        } else {
                            Toast.makeText(Settings.this, "Error: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(Settings.this, "Error parseando respuesta\nHTTP: " + code + "\n" + resp, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
