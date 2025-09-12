package com.example.proyectomovil.ui.main;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.proyectomovil.R;
import com.example.proyectomovil.Reports;
import com.example.proyectomovil.data.api.ApiClient;
import com.example.proyectomovil.data.api.ApiRoutes;
import com.example.proyectomovil.ui.materials.MaterialsActivity;
import com.example.proyectomovil.ui.user.EditUser;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private final Handler handler = new Handler();
    private Runnable updateTask;
    private DrawerLayout drawerLayout;

    // Si más adelante usas el gráfico de composición, quedará listo.
    private PieChart chartGases;

    // SOLO número de gas (PPM)
    private TextView tvValGas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // --- INICIALIZA PRIMERO LAS VISTAS QUE USARÁS ---
        tvValGas = findViewById(R.id.valGas);

        LineChart dashHistorico = findViewById(R.id.dashHistorico);
        chartGases = findViewById(R.id.chartGases); // puede ser null si no existe en el layout

        int userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId != -1) {
            updateTask = () -> {
                fetchSensorData(userId);
                fetchHistoricalData(userId, dashHistorico);
                handler.postDelayed(updateTask, 5000); // cada 5s
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
                startActivity(new Intent(this, MaterialsActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Primera carga del histórico
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
                    handler.postDelayed(updateTask, 2000); // cada 2s
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

    // -------- Networking --------

    private void fetchSensorData(int userId) {
        OkHttpClient client = ApiClient.get();

        HttpUrl url = HttpUrl.parse(ApiRoutes.READINGS_LATEST)
                .newBuilder()
                .addQueryParameter("idUser", String.valueOf(userId))
                .build();

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Error de red: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "HTTP " + response.code() + ": " + preview(resp), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                try {
                    JSONObject json = new JSONObject(resp);
                    if (json.getBoolean("success")) {
                        JSONObject data = json.getJSONObject("data");

                        float temperatura  = (float) safeDouble(data, "temperature");
                        float humedad      = (float) safeDouble(data, "humidity");
                        float tempSuelo    = (float) safeDouble(data, "ds18b20_temp");
                        float humedadSuelo = (float) safeDouble(data, "soil_moisture");
                        float gas          = (float) safeDouble(data, "mq135");

                        // gases (si luego usas composición)
                        float nh3     = (float) safeDouble(data, "ammonia");
                        float co2     = (float) safeDouble(data, "co2");
                        float co      = (float) safeDouble(data, "co");
                        float benzene = (float) safeDouble(data, "benzene");
                        float alcohol = (float) safeDouble(data, "alcohol");
                        float smoke   = (float) safeDouble(data, "smoke");

                        runOnUiThread(() -> {
                            // Colores para tus rings existentes
                            int rojo     = Color.parseColor("#E84D4D");  // T°
                            int morado   = Color.parseColor("#7B43C5");  // Humedad
                            int amarillo = Color.parseColor("#F69621");  // T° suelo
                            int naranja  = Color.parseColor("#FCC813");  // Humedad suelo
                            int track    = Color.parseColor("#F1F2F6");  // pista

                            // Rings existentes (si ya están en tu layout)
                            bindRing(findViewById(R.id.dashTemperatura),
                                    (TextView) findViewById(R.id.valTemp),
                                    temperatura, rojo, track);

                            bindRing(findViewById(R.id.dashHumedad),
                                    (TextView) findViewById(R.id.valHum),
                                    humedad, morado, track);

                            bindRing(findViewById(R.id.dashTemperaturaSuelo),
                                    (TextView) findViewById(R.id.valTemp2),
                                    tempSuelo, amarillo, track);

                            bindRing(findViewById(R.id.dashHumedadSuelo),
                                    (TextView) findViewById(R.id.valHum2),
                                    humedadSuelo, naranja, track);

                            // --- SOLO ACTUALIZA EL NÚMERO DE GAS COMO ENTERO ---
                            int gasInt = Math.round(gas); // 0..1000
                            // O con animación suave:
                            // int actual = 0;
                            // try { actual = Integer.parseInt(tvValGas.getText().toString()); } catch (Exception ignored) {}
                            // animateInt(tvValGas, actual, gasInt, 400);
                            tvValGas.setText(String.valueOf(gasInt));

                            if (chartGases != null) {
                                setupGasChart(chartGases, nh3, co2, co, benzene, alcohol, smoke);
                                chartGases.setCenterText("Concentracion de Gases\n" + gasInt);
                                chartGases.setCenterTextSize(14f);
                            }
                        });
                    } else {
                        String message = json.optString("message", "Respuesta inválida");
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
                    }
                } catch (JSONException e) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Error al parsear respuesta: " + preview(resp), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void fetchHistoricalData(int userId, LineChart chart) {
        OkHttpClient client = ApiClient.get();

        // Rango últimos 7 días (si tu API lo usa)
        String to = formatDate(new Date());
        String from = formatDate(daysAgo(7));

        HttpUrl url = HttpUrl.parse(ApiRoutes.READINGS_HIST)
                .newBuilder()
                .addQueryParameter("idUser", String.valueOf(userId))
                // .addQueryParameter("from", from)
                // .addQueryParameter("to", to)
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

    private void setupLineChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);
        chart.getAxisRight().setEnabled(false);
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

    private void setupPieChart(PieChart chart, float value, int ringColor, float limiteMax, String modoCritico) {
        float v = Math.max(0f, Math.min(100f, value));

        chart.setUsePercentValues(false);
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setHoleRadius(78f);
        chart.setTransparentCircleRadius(0f);
        chart.setDrawRoundedSlices(true);

        chart.setDrawCenterText(false);
        chart.setDrawEntryLabels(false);
        chart.getLegend().setEnabled(false);

        chart.setRotationEnabled(false);
        chart.setTouchEnabled(false);
        chart.setHighlightPerTapEnabled(false);

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(v));
        entries.add(new PieEntry(100f - v));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ringColor, Color.parseColor("#ECECEC"));
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(0f);
        dataSet.setDrawValues(false);

        chart.setData(new PieData(dataSet));
        chart.invalidate();
    }

    private void setupGasChart(PieChart chart, float nh3, float co2, float co,
                               float benzene, float alcohol, float smoke) {
        if (chart == null) return;

        // Si todo es 0, muestra un placeholder
        boolean allZero = (nh3 == 0 && co2 == 0 && co == 0 && benzene == 0 && alcohol == 0 && smoke == 0);
        List<PieEntry> entries = new ArrayList<>();
        if (allZero) {
            entries.add(new PieEntry(1f, "Sin datos"));
        } else {
            entries.add(new PieEntry(Math.max(0, nh3),     "NH₃"));
            entries.add(new PieEntry(Math.max(0, co2),     "CO₂"));
            entries.add(new PieEntry(Math.max(0, co),      "CO"));
            entries.add(new PieEntry(Math.max(0, benzene), "Benceno"));
            entries.add(new PieEntry(Math.max(0, alcohol), "Alcohol"));
            entries.add(new PieEntry(Math.max(0, smoke),   "Humo"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Composición Estimada de Gases");
        dataSet.setColors(new int[]{
                Color.rgb(76, 175, 80),   // NH3
                Color.rgb(255, 152, 0),   // CO2
                Color.RED,                // CO
                Color.BLUE,               // Benceno
                Color.MAGENTA,            // Alcohol
                Color.DKGRAY              // Humo
        });
        dataSet.setSliceSpace(2f);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        // Mostrar valores como enteros
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return String.valueOf(Math.round(value));
            }
        });

        chart.setData(new PieData(dataSet));
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(50f);
        chart.setTransparentCircleRadius(55f);
        chart.setEntryLabelColor(Color.BLACK);
        chart.setEntryLabelTextSize(12f);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);
        chart.setUsePercentValues(false);
        chart.setDrawEntryLabels(!allZero);   // no muestres labels si es placeholder

        // Si no hay datos, indica en el centro
        if (allZero) {
            chart.setCenterText("Sin datos");
            chart.setCenterTextSize(14f);
        }

        chart.invalidate();
    }

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

    private void setupRing(PieChart chart, float percent, int ringColor, int trackColor) {
        float value = Math.max(0f, Math.min(100f, percent));

        chart.setUsePercentValues(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawEntryLabels(false);

        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setHoleRadius(78f);
        chart.setTransparentCircleRadius(0f);

        chart.setRotationEnabled(false);
        chart.setTouchEnabled(false);
        chart.setHighlightPerTapEnabled(false);

        chart.setRotationAngle(-90f);

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(value));
        entries.add(new PieEntry(100f - value));

        PieDataSet ds = new PieDataSet(entries, "");
        ds.setDrawValues(false);
        ds.setColors(Arrays.asList(ringColor, trackColor));
        ds.setSliceSpace(3f);
        ds.setSelectionShift(0f);

        chart.setData(new PieData(ds));
        chart.invalidate();
    }

    private void bindRing(PieChart chart, TextView tvValue, float value,
                          int ringColor, int trackColor) {
        float v = Math.max(0f, Math.min(100f, value));
        float v2 = Math.max(0f, Math.min(100f, value));

        setupRing(chart, v, ringColor, trackColor);

        Float prev = (Float) tvValue.getTag(R.id.valTemp);
        if (prev == null) prev = v;
        animateNumber(tvValue, prev, v);
        tvValue.setTag(R.id.valHum, v);

        Float prev2 = (Float) tvValue.getTag(R.id.valTemp2);
        if(prev2 == null) prev2 = v2;
        animateNumber(tvValue, prev2, v2);
        tvValue.setTag(R.id.valHum2);
    }

    private void animateNumber(TextView tv, float from, float to) {
        ValueAnimator va = ValueAnimator.ofFloat(from, to);
        va.setDuration(450);
        va.addUpdateListener(anim -> {
            float v = (float) anim.getAnimatedValue();
            String txt = String.format(Locale.getDefault(), "%.0f", v);
            tv.setText(txt);
        });
        va.start();
    }

    private void animateInt(TextView tv, int from, int to, long ms) {
        ValueAnimator va = ValueAnimator.ofInt(from, to);
        va.setDuration(ms);
        va.addUpdateListener(a -> tv.setText(String.valueOf((int) a.getAnimatedValue())));
        va.start();
    }
}
