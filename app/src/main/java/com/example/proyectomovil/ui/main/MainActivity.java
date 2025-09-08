package com.example.proyectomovil.ui.main;

import android.content.Intent;
import android.graphics.Color;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;

import com.example.proyectomovil.ui.user.EditUser;
import com.example.proyectomovil.ui.materials.MaterialsActivity;
import com.example.proyectomovil.R;
import com.example.proyectomovil.Reports;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieData;

import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.components.XAxis;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

// Usa cliente y rutas centralizadas
import com.example.proyectomovil.data.api.ApiClient;
import com.example.proyectomovil.data.api.ApiRoutes;

public class MainActivity extends AppCompatActivity {

    private final Handler handler = new Handler();
    private Runnable updateTask;
    private DrawerLayout drawerLayout;
    private PieChart chartGases;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        LineChart dashHistorico = findViewById(R.id.dashHistorico);
        chartGases = findViewById(R.id.chartGases);

        int userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId != -1) {
            updateTask = () -> {
                fetchSensorData(userId);
                fetchHistoricalData(userId, findViewById(R.id.dashHistorico));
                handler.postDelayed(updateTask, 5000); //cada 5s
            };
            handler.post(updateTask);
        }

        setupLineChart(dashHistorico);

        drawerLayout = findViewById(R.id.drawer_layout);
        ImageView btnMenu = findViewById(R.id.btn_menu);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        NavigationView navigationView = findViewById(R.id.nav_view);

        View headerView = navigationView.getHeaderView(0);
        ((TextView) headerView.findViewById(R.id.tvHeaderName)).setText(getIntent().getStringExtra("USER_NAME"));
        ((TextView) headerView.findViewById(R.id.tvHeaderEmail)).setText(getIntent().getStringExtra("USER_EMAIL"));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Toast.makeText(this, "Inicio", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_reports) {
                Intent intent = new Intent(this, Reports.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("USER_NAME", getIntent().getStringExtra("USER_NAME"));
                startActivity(intent);
            } else if (id == R.id.nav_customers) {
                Intent intent = new Intent(this, EditUser.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_materials) {
                Toast.makeText(this, "Materials", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MaterialsActivity.class);
                startActivity(intent);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        fetchHistoricalData(userId, dashHistorico);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (updateTask != null) handler.removeCallbacks(updateTask);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId != -1) {
            if (updateTask == null) {
                updateTask = () -> {
                    fetchSensorData(userId);
                    fetchHistoricalData(userId, findViewById(R.id.dashHistorico));
                    handler.postDelayed(updateTask, 2000); //cada 2s
                };
            }
            handler.post(updateTask);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateTask != null) handler.removeCallbacks(updateTask);
    }

    private void fetchSensorData(int userId) {

        OkHttpClient client = ApiClient.get();

        // /readings/latest + idUser
        HttpUrl url = HttpUrl.parse(ApiRoutes.READINGS_LATEST)
                .newBuilder()
                .addQueryParameter("idUser", String.valueOf(userId))
                .build();

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error de red: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "HTTP " + response.code() + ": " + preview(resp), Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    JSONObject json = new JSONObject(resp);
                    if (json.getBoolean("success")) {
                        JSONObject data = json.getJSONObject("data");

                        float temperatura = (float) safeDouble(data, "temperature");
                        float humedad = (float) safeDouble(data, "humidity");
                        float tempSuelo = (float) safeDouble(data, "ds18b20_temp");
                        float humedadSuelo = (float) safeDouble(data, "soil_moisture");
                        float gas = (float) safeDouble(data, "mq135");

                        // Datos para gráfico de gases
                        float nh3 = (float) safeDouble(data, "ammonia");
                        float co2 = (float) safeDouble(data, "co2");
                        float co = (float) safeDouble(data, "co");
                        float benzene = (float) safeDouble(data, "benzene");
                        float alcohol = (float) safeDouble(data, "alcohol");
                        float smoke = (float) safeDouble(data, "smoke");

                        runOnUiThread(() -> {
                            //Actilizacion de graficos circulares
                            setupPieChart(findViewById(R.id.dashTemperatura), temperatura, Color.parseColor("#7FE1AD"), 35f, "mayor");
                            setupPieChart(findViewById(R.id.dashHumedad), humedad, Color.parseColor("#F85F6A"), 40f, "mayor");
                            setupPieChart(findViewById(R.id.dashGas), gas, Color.parseColor("#5F6AF8"), 15f, "mayor");
                            setupPieChart(findViewById(R.id.dashTemperaturaSuelo), tempSuelo, Color.parseColor("#2F073B"), 15f, "menor");
                            setupPieChart(findViewById(R.id.dashHumedadSuelo), humedadSuelo, Color.parseColor("#C238EB"), 30f, "menor");

                            // Actualizacion de gráfico de gases
                            setupGasChart(chartGases, nh3, co2, co, benzene, alcohol, smoke);
                        });
                    } else {
                        String message = json.optString("message", "Respuesta inválida");
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
                    }
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al parsear respuesta: " + preview(resp), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void setupPieChart(PieChart chart, float value, int ringColor, float limiteMax, String modoCritico) {
        float v = Math.max(0f, Math.min(100f, value));

        // Estilo del donut
        chart.setUsePercentValues(false);
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setHoleRadius(78f);
        chart.setTransparentCircleRadius(0f);
        chart.setDrawRoundedSlices(true);

        // Nada de textos/labels/leyenda
        chart.setDrawCenterText(false);
        chart.setDrawEntryLabels(false);
        chart.getLegend().setEnabled(false);

        // Sin interacción/animación por toque
        chart.setRotationEnabled(false);
        chart.setTouchEnabled(false);
        chart.setHighlightPerTapEnabled(false);

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(v));
        entries.add(new PieEntry(100f - v));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ringColor, Color.parseColor("#ECECEC")); // color del aro + track gris
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(0f);
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        chart.setData(data);
        chart.invalidate();
    }


    private void setupGasChart(PieChart chart, float nh3, float co2, float co, float benzene, float alcohol, float smoke) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(nh3, "NH₃"));
        entries.add(new PieEntry(co2, "CO₂"));
        entries.add(new PieEntry(co, "CO"));
        entries.add(new PieEntry(benzene, "Benceno"));
        entries.add(new PieEntry(alcohol, "Alcohol"));
        entries.add(new PieEntry(smoke, "Humo"));

        PieDataSet dataSet = new PieDataSet(entries, "Composición Estimada de Gases");
        dataSet.setColors(new int[]{
                Color.rgb(76, 175, 80), Color.rgb(255, 152, 0),
                Color.RED, Color.BLUE, Color.MAGENTA, Color.DKGRAY
        });

        dataSet.setSliceSpace(2f);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        chart.setData(new PieData(dataSet));
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(50f);
        chart.setTransparentCircleRadius(55f);
        chart.setEntryLabelColor(Color.BLACK);
        chart.setEntryLabelTextSize(12f);
        chart.getDescription().setEnabled(false);
        chart.setCenterText("Composición de Gases");
        chart.setCenterTextSize(14f);
        chart.getLegend().setEnabled(true);
        chart.invalidate();
    }

    private void setupLineChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);
        chart.getAxisRight().setEnabled(false);
    }

    private void fetchHistoricalData(int userId, LineChart chart) {
        OkHttpClient client = ApiClient.get();

        // Armamos rango de fechas para el histórico (últimos 7 días)
        String to = formatDate(new Date());
        String from = formatDate(daysAgo(7));

        // /readings?from=YYYY-MM-DD&to=YYYY-MM-DD + idUser
        HttpUrl url = HttpUrl.parse(ApiRoutes.READINGS_HIST)
                .newBuilder()
                .addQueryParameter("idUser", String.valueOf(userId))
                .build();

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al obtener histórico: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "HTTP " + response.code() + ": " + preview(json), Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    JSONObject obj = new JSONObject(json);
                    if (obj.getBoolean("success")) {
                        JSONArray dataArray = obj.getJSONArray("data");

                        List<Entry> tempEntries = new ArrayList<>();
                        List<Entry> humidityEntries = new ArrayList<>();
                        List<Entry> dsEntries = new ArrayList<>();
                        List<Entry> soilEntries = new ArrayList<>();
                        List<Entry> gasEntries = new ArrayList<>();
                        List<String> labels = new ArrayList<>();

                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject d = dataArray.getJSONObject(i);
                            labels.add(d.getString("datetime"));
                            tempEntries.add(new Entry(i, (float) safeDouble(d, "temperature")));
                            humidityEntries.add(new Entry(i, (float) safeDouble(d, "humidity")));
                            dsEntries.add(new Entry(i, (float) safeDouble(d, "ds18b20_temp")));
                            soilEntries.add(new Entry(i, (float) safeDouble(d, "soil_moisture")));
                            gasEntries.add(new Entry(i, (float) safeDouble(d, "mq135")));
                        }

                        runOnUiThread(() -> updateLineChart(chart, labels, tempEntries, humidityEntries, dsEntries, soilEntries, gasEntries));
                    } else {
                        String msg = obj.optString("message", "Respuesta inválida");
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
                    }
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al parsear histórico: " + preview(json), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void updateLineChart(LineChart chart, List<String> labels,
                                 List<Entry> temp, List<Entry> hum, List<Entry> ds,
                                 List<Entry> soil, List<Entry> gas) {

        LineDataSet setTemp = new LineDataSet(temp, "Temp. (°C)");
        setTemp.setColor(Color.RED); setTemp.setLineWidth(2f); setTemp.setCircleColor(Color.RED);

        LineDataSet setHum = new LineDataSet(hum, "Humedad (%)");
        setHum.setColor(Color.BLUE); setHum.setLineWidth(2f); setHum.setCircleColor(Color.BLUE);

        LineDataSet setDS = new LineDataSet(ds, "Temp. DS18B20 (°C)");
        setDS.setColor(Color.rgb(255, 165, 0)); setDS.setLineWidth(2f); setDS.setCircleColor(Color.rgb(255, 165, 0));

        LineDataSet setSoil = new LineDataSet(soil, "Humedad Suelo (%)");
        setSoil.setColor(Color.MAGENTA); setSoil.setLineWidth(2f); setSoil.setCircleColor(Color.MAGENTA);

        LineDataSet setGas = new LineDataSet(gas, "MQ-135 (calidad)");
        setGas.setColor(Color.GREEN); setGas.setLineWidth(2f); setGas.setCircleColor(Color.GREEN);

        LineData lineData = new LineData(setTemp, setHum, setDS, setSoil, setGas);
        chart.setData(lineData);
        chart.getAxisRight().setEnabled(false);

        // Eje X con fechas
        XAxis xAxis = chart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setLabelCount(labels.size(), true);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return (index >= 0 && index < labels.size()) ? labels.get(index) : "";
            }
        });

        chart.invalidate();
    }

    // -------- Helpers --------

    private double safeDouble(JSONObject o, String key) {
        try {
            if (o.isNull(key)) return 0.0;
            Object v = o.get(key);
            if (v instanceof Number) return ((Number) v).doubleValue();
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return 0.0;
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String preview(String s) {
        if (s == null) return "";
        s = s.replace("\n", " ").replace("\r", " ");
        return s.length() > 160 ? s.substring(0, 160) + "..." : s;
    }

    private String formatDate(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(date);
    }

    private Date daysAgo(int days){
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        return cal.getTime();
    }
}
