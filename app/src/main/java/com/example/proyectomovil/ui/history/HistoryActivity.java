package com.example.proyectomovil.ui.history;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.proyectomovil.R;
import com.example.proyectomovil.data.api.ApiClient;
import com.example.proyectomovil.data.api.ApiRoutes;
import com.example.proyectomovil.domain.models.Reading;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.example.proyectomovil.ui.base.BaseDrawerActivity;

public class HistoryActivity extends BaseDrawerActivity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private HistoryAdapter adapter;
    private final List<Reading> readings = new ArrayList<>();

    private Button btnPickDate, btnPrev, btnNext;
    private TextView txtPage, txtSelectedDate;

    private int userId;
    private int page = 1;
    private final int limit = 10;

    private String selectedDate = null; // un solo día

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentWithDrawer(R.layout.activity_history);
        setTitle("Historial");
        recyclerView = findViewById(R.id.recyclerHistory);
        swipeRefreshLayout = findViewById(R.id.swipeHistory);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        txtPage = findViewById(R.id.txtPage);
        txtSelectedDate = findViewById(R.id.txtSelectedDate);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(readings);
        recyclerView.setAdapter(adapter);

        userId = getIntent().getIntExtra("USER_ID", -1);

        swipeRefreshLayout.setOnRefreshListener(this::fetchHistory);

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPrev.setOnClickListener(v -> {
            if (page > 1) {
                page--;
                fetchHistory();
            }
        });
        btnNext.setOnClickListener(v -> {
            page++;
            fetchHistory();
        });

        swipeRefreshLayout.setRefreshing(true);
        fetchHistory(); // 🔹 Llamamos sin fecha para traer la última disponible
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int y = calendar.get(Calendar.YEAR);
        int m = calendar.get(Calendar.MONTH);
        int d = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = year + "-" +
                    String.format("%02d", (month + 1)) + "-" +
                    String.format("%02d", dayOfMonth);
            txtSelectedDate.setText("📅 Date: " + selectedDate);
            page = 1;
            fetchHistory();
        }, y, m, d);
        dp.show();
    }

    private void fetchHistory() {
        OkHttpClient client = ApiClient.get();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(ApiRoutes.READINGS_HIST).newBuilder()
                .addQueryParameter("idUser", String.valueOf(userId))
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("limit", String.valueOf(limit));

        if (selectedDate != null) {
            urlBuilder.addQueryParameter("date", selectedDate);
        }

        Request request = new Request.Builder().url(urlBuilder.build()).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(HistoryActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> swipeRefreshLayout.setRefreshing(false));

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(HistoryActivity.this, "HTTP " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    JSONObject json = new JSONObject(resp);
                    if (json.optBoolean("success", false)) {
                        JSONArray arr = json.getJSONArray("data");

                        // 🔹 Si no teníamos fecha seleccionada, usamos la devuelta por el backend
                        if (selectedDate == null && json.has("latest_date")) {
                            selectedDate = json.getString("latest_date");
                            runOnUiThread(() ->
                                    txtSelectedDate.setText("📅 Date: " + selectedDate)
                            );
                        }

                        readings.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            readings.add(Reading.fromJson(obj));
                        }

                        runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            txtPage.setText("Page " + page);
                            btnPrev.setEnabled(page > 1);
                            btnNext.setEnabled(arr.length() == limit);
                        });
                    } else {
                        runOnUiThread(() -> {
                            readings.clear();
                            adapter.notifyDataSetChanged();
                            txtPage.setText("Page " + page);
                            Toast.makeText(HistoryActivity.this, "No data found", Toast.LENGTH_SHORT).show();
                            btnNext.setEnabled(false);
                        });
                    }
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(HistoryActivity.this, "Error parsing JSON", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
