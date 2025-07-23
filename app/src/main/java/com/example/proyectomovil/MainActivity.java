package com.example.proyectomovil;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Referencias a los gráficos
        PieChart dashTemperatura = findViewById(R.id.dashTemperatura);
        PieChart dashHumedad = findViewById(R.id.dashHumedad);
        PieChart dashGas = findViewById(R.id.dashGas);
        PieChart dashTemperaturaSuelo = findViewById(R.id.dashTemperaturaSuelo);
        PieChart dashHumedadSuelo = findViewById(R.id.dashHumedadSuelo);
        BarChart dashHistorico = findViewById(R.id.dashHistorico);

        // Datos ejemplo para PieCharts
        setupPieChart(dashTemperatura, 28f, Color.parseColor("#3F51B5"), 35f, "mayor");         // Temperatura: crítica si supera 35
        setupPieChart(dashHumedad, 48f, Color.parseColor("#4CAF50"), 40f, "mayor");              // Humedad: crítica si supera 40
        setupPieChart(dashGas, 12f, Color.parseColor("#FF9800"), 15f, "mayor");                 // Gas: crítica si supera 15
        setupPieChart(dashTemperaturaSuelo, 20f, Color.parseColor("#9C27B0"), 15f, "menor");    // Temp Suelo: crítica si baja de 15
        setupPieChart(dashHumedadSuelo, 35f, Color.parseColor("#607D8B"), 30f, "menor");        // Humedad Suelo: crítica si baja de 30
        // Datos ejemplo para BarChart
        setupBarChart(dashHistorico);
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
        } else if (modoCritico.equals("menor")) {
            // Ej: si baja del valor -> color rojo
            if (value < limiteMax) {
                centerColor = Color.parseColor("#F44336"); // rojo
            } else {
                centerColor = Color.parseColor("#4CAF50"); // verde
            }
        } else {
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
        dataSet.setColors(ringColor, Color.TRANSPARENT);
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