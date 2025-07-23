package com.example.proyectomovil;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private final Handler handler = new Handler();
    private Runnable updateTask;
    private DrawerLayout drawerLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Referencias de los gráficos circulares
        PieChart dashTemperatura = findViewById(R.id.dashTemperatura);
        PieChart dashHumedad = findViewById(R.id.dashHumedad);
        PieChart dashGas = findViewById(R.id.dashGas);
        PieChart dashTemperaturaSuelo = findViewById(R.id.dashTemperaturaSuelo);
        PieChart dashHumedadSuelo = findViewById(R.id.dashHumedadSuelo);
        BarChart dashHistorico = findViewById(R.id.dashHistorico);

        int userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId != -1) {
            updateTask = new Runnable() {
                @Override
                public void run() {
                    fetchSensorData(userId);
                    handler.postDelayed(this, 10000); // Repetir cada 10 segundos
                }
            };
            handler.post(updateTask); // Inicia el ciclo
        }

        setupBarChart(dashHistorico); // Esto puedes mantener para histórico simulado

        //navegacion menu
        drawerLayout = findViewById(R.id.drawer_layout);

        ImageView btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START); // ✅ Abre el menú lateral
        });

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                Toast.makeText(this, "Perfil seleccionado", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_logout) {
                Toast.makeText(this, "Cerrar sesión", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTask); // ✅ Detiene el ciclo de actualizaciones
    }
    private void fetchSensorData(int userId) {
        OkHttpClient client = new OkHttpClient();
        String url = "http://192.168.0.4/ProyectoGrado/get_last_reading.php?idUser=" + userId;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body().string();
                try {
                    JSONObject json = new JSONObject(resp);
                    if (json.getBoolean("success")) {
                        JSONObject data = json.getJSONObject("data");

                        float temperatura = (float) data.getDouble("temperature");
                        float humedad = (float) data.getDouble("humidity");
                        float tempSuelo = (float) data.getDouble("ds18b20_temp");
                        float humedadSuelo = (float) data.getDouble("soil_moisture");
                        float gas = (float) data.getDouble("mq135");

                        runOnUiThread(() -> {
                            setupPieChart(findViewById(R.id.dashTemperatura), temperatura, Color.parseColor("#7FE1AD"), 35f, "mayor");
                            setupPieChart(findViewById(R.id.dashHumedad), humedad, Color.parseColor("#F85F6A"), 40f, "mayor");
                            setupPieChart(findViewById(R.id.dashGas), gas, Color.parseColor("#5F6AF8"), 15f, "mayor");
                            setupPieChart(findViewById(R.id.dashTemperaturaSuelo), tempSuelo, Color.parseColor("#2F073B"), 15f, "menor");
                            setupPieChart(findViewById(R.id.dashHumedadSuelo), humedadSuelo, Color.parseColor("#C238EB"), 30f, "menor");
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Sin datos encontrados", Toast.LENGTH_SHORT).show());
                    }
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al parsear respuesta", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void setupPieChart(PieChart chart, float value, int ringColor, float limiteMax, String modoCritico) {
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(80f);
        chart.setTransparentCircleRadius(0f);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setRotationEnabled(false);
        chart.setDrawEntryLabels(false);
        chart.getLegend().setEnabled(false);

        // 💥 Determinar color del texto según sensor y criterio
        int centerColor;

        if (modoCritico.equals("mayor")) {
            // Ej: si excede el valor -> color rojo
            if (value > limiteMax) {
                centerColor = Color.parseColor("#F44336"); // rojo
            } else {
                centerColor = Color.parseColor("#4CAF50"); // verde
            }
        }
        else {
            // Por defecto: gris
            centerColor = Color.DKGRAY;
        }

        // Texto en el centro
        chart.setCenterText((int) value + "%");
        chart.setCenterTextSize(16f);
        chart.setCenterTextColor(centerColor);
        chart.setCenterTextTypeface(Typeface.DEFAULT_BOLD);

        // Datos
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(value, ""));
        entries.add(new PieEntry(100f - value, ""));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ringColor, Color.LTGRAY);
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        chart.setData(data);
        chart.invalidate();
    }


    private void setupBarChart(BarChart barChart) {
        barChart.getDescription().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);

        java.util.List<BarEntry> entries = new java.util.ArrayList<>();
        entries.add(new BarEntry(1f, 60f));
        entries.add(new BarEntry(2f, 40f));
        entries.add(new BarEntry(3f, 75f));
        entries.add(new BarEntry(4f, 30f));

        BarDataSet dataSet = new BarDataSet(entries, "Histórico");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);
        barChart.setData(data);
        barChart.setFitBars(true);
        barChart.invalidate();
    }
}