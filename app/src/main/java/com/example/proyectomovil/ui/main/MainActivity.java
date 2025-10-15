package com.example.proyectomovil.ui.main;

import android.animation.ValueAnimator;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
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
import com.example.proyectomovil.ui.history.HistoryActivity;
import com.example.proyectomovil.ui.materials.MaterialsActivity;
import com.example.proyectomovil.ui.user.EditUser;
import com.example.proyectomovil.workers.AlertsWorker;
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
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.example.proyectomovil.ui.base.BaseDrawerActivity;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class MainActivity extends BaseDrawerActivity {

    private final Handler handler = new Handler();
    private Runnable updateTask;
    private DrawerLayout drawerLayout;

    private PieChart chartGases;
    private TextView tvValGas;

    private boolean isAlertShowing = false;        // Para controlar si hay alerta activa
    private boolean isAlertMinimized = false;      // Para controlar si está minimizada
    private ImageView alertMinimizedIndicator;     // Referencia al icono
    private Handler blinkHandler = new Handler();  // Para parpadeo
    private Runnable blinkRunnable;
    private androidx.appcompat.app.AlertDialog activeAlertDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentWithDrawer(R.layout.activity_main);
        setTitle("Dashboard");

        alertMinimizedIndicator = findViewById(R.id.alertMinimizedIndicator);
        alertMinimizedIndicator.setOnClickListener(v -> {
            if (isAlertMinimized && activeAlertDialog != null) {
                activeAlertDialog.show(); // Vuelve a mostrar la alerta
                isAlertMinimized = false;
                stopBlinking();
            }
        });

        tvValGas = findViewById(R.id.valGas);
        LineChart dashHistorico = findViewById(R.id.dashHistorico);
        chartGases = findViewById(R.id.chartGases);

        int userId = getIntent().getIntExtra("USER_ID", -1);

        // ----------------- Handler para alertas inmediatas -----------------
        if (userId != -1) {
            updateTask = () -> {
                fetchSensorData(userId);
                fetchHistoricalData(userId, dashHistorico);
                fetchAlertsFromApi(); // 🚨 Fetch de alertas en primer plano
                handler.postDelayed(updateTask, 5000);
            };
            handler.post(updateTask);
        }

        setupLineChart(dashHistorico);

        // ----------------- Menú lateral -----------------
        drawerLayout = findViewById(R.id.drawer_layout);
        ImageView btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        ((TextView) headerView.findViewById(R.id.tvHeaderName))
                .setText(getIntent().getStringExtra("USER_NAME"));
        ((TextView) headerView.findViewById(R.id.tvHeaderEmail))
                .setText(getIntent().getStringExtra("USER_EMAIL"));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Toast.makeText(this, "Ya está en Inicio", Toast.LENGTH_SHORT).show();
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
            } else if (id == R.id.nav_historial) {
                Intent intent = new Intent(this, HistoryActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // ----------------- WorkManager para alertas en segundo plano -----------------
        createNotificationChannel();
        scheduleAlertsWorker();
        /*
        Data inputData = new Data.Builder()
                .putInt("userId", userId)
                .build();

        OneTimeWorkRequest testWork =
                new OneTimeWorkRequest.Builder(AlertsWorker.class)
                        .setInputData(inputData)
                        .build();

        WorkManager.getInstance(this).enqueue(testWork);*/
    }


    private void scheduleAlertsWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(AlertsWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "alerts_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
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
                    handler.postDelayed(updateTask, 2000);
                };
            }
            handler.post(updateTask);
        }
        fetchAlertsFromApi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateTask != null) handler.removeCallbacks(updateTask);
    }

    // ----------- Métodos fetchSensorData y fetchHistoricalData se quedan igual -----------

    private void fetchSensorData(int userId) {
        OkHttpClient client = ApiClient.get();
        HttpUrl url = HttpUrl.parse(ApiRoutes.READINGS_LATEST)
                .newBuilder()
                .addQueryParameter("idUser", String.valueOf(userId))
                .build();

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Error de red: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Ignorar 429 Too Many Requests
                if (response.code() == 429) {
                    // Opcional: log para debug
                    // Log.w("SensorData", "Recibido 429, ignorando esta respuesta");
                    return;
                }

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

                        float nh3     = sanitize((float) safeDouble(data, "ammonia"));
                        float co2     = sanitize((float) safeDouble(data, "co2"));
                        float co      = sanitize((float) safeDouble(data, "co"));
                        float benzene = sanitize((float) safeDouble(data, "benzene"));
                        float alcohol = sanitize((float) safeDouble(data, "alcohol"));
                        float smoke   = sanitize((float) safeDouble(data, "smoke"));
                        float gas     = sanitize((float) safeDouble(data, "mq135"));

                        runOnUiThread(() -> {
                            int rojo     = Color.parseColor("#E84D4D");
                            int morado   = Color.parseColor("#7B43C5");
                            int amarillo = Color.parseColor("#F69621");
                            int naranja  = Color.parseColor("#FCC813");
                            int track    = Color.parseColor("#F1F2F6");

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

                            int gasInt = Math.round(gas);
                            tvValGas.setText(String.valueOf(gasInt));

                            if (chartGases != null) {
                                boolean isAllZero = (nh3 == 0f && co2 == 0f && co == 0f
                                        && benzene == 0f && alcohol == 0f && smoke == 0f);

                                setupGasChart(chartGases, nh3, co2, co, benzene, alcohol, smoke);

                                if (!isAllZero) {
                                    chartGases.setCenterText("Elementos del Gas");
                                    chartGases.setCenterTextSize(14f);
                                }
                            }
                        });
                    }
                } catch (JSONException e) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Error al parsear respuesta", Toast.LENGTH_SHORT).show()
                    );
                }
            }

        });
    }

    private void fetchHistoricalData(int userId, LineChart chart) {
        OkHttpClient client = ApiClient.get();
        HttpUrl url = HttpUrl.parse(ApiRoutes.READINGS_HIST)
                .newBuilder()
                .addQueryParameter("idUser", String.valueOf(userId))
                .build();

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al obtener histórico", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String json = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) return;

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
                        }

                        runOnUiThread(() -> updateLineChart(chart, labels, tempEntries, humidityEntries, dsEntries, soilEntries, gasEntries));
                    }
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error parseando histórico", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void fetchAlertsFromApi() {
        int userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId != -1) fetchAlerts(userId);
    }

    private void fetchAlerts(int userId) {
        OkHttpClient client = ApiClient.get();
        HttpUrl url = HttpUrl.parse(ApiRoutes.ALERTS)
                .newBuilder()
                .addQueryParameter("idUser", String.valueOf(userId))
                .build();

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Error al obtener alertas", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Si el servidor devuelve 429, lo ignoramos y no mostramos Toast ni alertas
                if (response.code() == 429) {
                    // Opcional: podrías hacer un log en consola si quieres
                    // Log.w("Alerts", "Recibido 429 Too Many Requests, ignorando...");
                    return;
                }

                String resp = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) return;

                try {
                    JSONObject json = new JSONObject(resp);
                    if (json.getBoolean("success")) {
                        JSONArray alerts = json.getJSONArray("alerts");
                        if (alerts.length() > 0) {
                            StringBuilder alertMsg = new StringBuilder();
                            for (int i = 0; i < alerts.length(); i++) {
                                alertMsg.append("• ").append(alerts.getString(i)).append("\n");
                            }

                            runOnUiThread(() -> {
                                // Solo mostrar alerta si no hay activa ni minimizada
                                if (!isAlertShowing && !isAlertMinimized) {
                                    showAlertDialog(alertMsg.toString());
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Error al parsear alertas", Toast.LENGTH_SHORT).show()
                    );
                }
            }

        });
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alert_sound);

            NotificationChannel channel = new NotificationChannel(
                    "alerts_channel",
                    "Alertas Compostaje",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Canal para notificaciones de alertas");

            // Asignar sonido
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(soundUri, audioAttributes);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private String lastAlertMessage = ""; // Guardará la alerta actual

    private MediaPlayer mediaPlayer; // Colócalo como atributo de la clase MainActivity

    private void showAlertDialog(String message) {
        if (isAlertShowing || isAlertMinimized) return; // No mostrar nueva alerta si ya hay o está minimizada

        lastAlertMessage = message;
        isAlertShowing = true;

        // Iniciar sonido de alerta
        playAlertSound();

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🚨 Alertas del Compostaje")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    isAlertShowing = false;
                    isAlertMinimized = false;
                    stopBlinking();
                    alertMinimizedIndicator.setVisibility(View.GONE);

                    // Detener sonido
                    stopAlertSound();
                })
                .setNegativeButton("Minimizar", (dialog, which) -> {
                    isAlertShowing = false;
                    isAlertMinimized = true;
                    startBlinking();

                    // Detener sonido mientras está minimizada
                    stopAlertSound();
                })
                .setCancelable(false);

        activeAlertDialog = builder.create();
        activeAlertDialog.show();
    }

    // -------------------- Métodos de sonido --------------------
    private void playAlertSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound); // Reemplaza con tu archivo en res/raw
            mediaPlayer.setLooping(true); // Repetir mientras la alerta esté activa
        }
        mediaPlayer.start();
    }

    private void stopAlertSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    private void startBlinking() {
        alertMinimizedIndicator.setVisibility(View.VISIBLE);
        blinkRunnable = new Runnable() {
            boolean visible = true;
            @Override
            public void run() {
                alertMinimizedIndicator.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
                visible = !visible;
                blinkHandler.postDelayed(this, 500);
            }
        };
        blinkHandler.post(blinkRunnable);
    }

    private void stopBlinking() {
        if (blinkRunnable != null) blinkHandler.removeCallbacks(blinkRunnable);
        alertMinimizedIndicator.setVisibility(View.GONE);
    }


    private void showMinimizedIndicator() {
        if (alertMinimizedIndicator == null) {
            alertMinimizedIndicator = findViewById(R.id.alertMinimizedIndicator);
        }
        alertMinimizedIndicator.setVisibility(View.VISIBLE);

        // Parpadeo
        ValueAnimator blink = ValueAnimator.ofFloat(0f, 1f);
        blink.setDuration(500);
        blink.setRepeatMode(ValueAnimator.REVERSE);
        blink.setRepeatCount(ValueAnimator.INFINITE);
        blink.addUpdateListener(anim -> alertMinimizedIndicator.setAlpha((float) anim.getAnimatedValue()));
        blink.start();

        alertMinimizedIndicator.setOnClickListener(v -> {
            // Restaurar alerta
            if (activeAlertDialog != null && isAlertMinimized) {
                activeAlertDialog.show();
                isAlertMinimized = false;
                hideMinimizedIndicator();
            }
        });
    }

    private void hideMinimizedIndicator() {
        if (alertMinimizedIndicator != null) {
            alertMinimizedIndicator.setVisibility(View.GONE);
            alertMinimizedIndicator.animate().cancel();
        }
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


    private void setupGasChart(PieChart chart, float nh3, float co2, float co,
                               float benzene, float alcohol, float smoke) {
        // Si el grafico no existe terminara el metodo para evitar errores
        if (chart == null) return;
        // Devuelve true o false si todos los datos de gases son 0
        boolean allZero = (nh3 == 0 && co2 == 0 && co == 0 && benzene == 0 && alcohol == 0 && smoke == 0);
        // Crea una lista PieEntry para guardar los datos de los gases
        List<PieEntry> entries = new ArrayList<>();
        // Si el booleano que se creo devuelve verdadero entoces se agrega solo una particion de datos y un label sin datos
        if (allZero) {
            entries.add(new PieEntry(1f, "Sin datos"));
        } else {
            // En caso contrario se rellena los atributos con los valores de los compuestos del gas
            entries.add(new PieEntry(Math.max(0, nh3),     "NH₃"));
            entries.add(new PieEntry(Math.max(0, co2),     "CO₂"));
            entries.add(new PieEntry(Math.max(0, co),      "CO"));
            entries.add(new PieEntry(Math.max(0, benzene), "Benceno"));
            entries.add(new PieEntry(Math.max(0, alcohol), "Alcohol"));
            entries.add(new PieEntry(Math.max(0, smoke),   "Humo"));
        }
        // Crea y configura el conjunto de datos que se van a pintar dentro el PieChart
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{
                Color.rgb(76, 175, 80),
                Color.rgb(255, 152, 0),
                Color.RED,
                Color.BLUE,
                Color.MAGENTA,
                Color.DKGRAY
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

    private float sanitize(float v) {
        if (Float.isNaN(v) || Float.isInfinite(v) || v < 0f) return 0f;
        return v;
    }



}
