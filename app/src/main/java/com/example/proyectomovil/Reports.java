package com.example.proyectomovil;

import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.HttpUrl;

import com.example.proyectomovil.data.api.ApiRoutes;

public class Reports extends AppCompatActivity {

    private int userId;

    // rango de fechas (yyyy-MM-dd). Se inicializa a últimos 30 días
    private String fromDate;
    private String toDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reports);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userId = getIntent().getIntExtra("USER_ID", 1);

        // rango por defecto: últimos 30 días
        toDate = formatDate(new Date());
        fromDate = formatDate(daysAgo(30));

        CardView cardCompostReadings = findViewById(R.id.cardCompostReadings);
        CardView cardVentas = findViewById(R.id.cardCompostSales);

        // Lecturas de compostaje
        cardCompostReadings.setOnClickListener(v -> pickDateRangeThenFormat("lecturas"));

        // Ventas de compostaje
        cardVentas.setOnClickListener(v -> pickDateRangeThenFormat("ventas"));
    }

    /** Paso 1: pedir rango de fechas (from/to). Luego muestra selección de formato. */
    private void pickDateRangeThenFormat(String tipo) {
        // 1) pick FROM
        Calendar calFrom = Calendar.getInstance();
        DatePickerDialog dpFrom = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    Calendar selFrom = Calendar.getInstance();
                    selFrom.set(y, m, d, 0, 0, 0);
                    fromDate = formatDate(selFrom.getTime());

                    // 2) pick TO
                    Calendar calTo = Calendar.getInstance();
                    DatePickerDialog dpTo = new DatePickerDialog(
                            this,
                            (view2, y2, m2, d2) -> {
                                Calendar selTo = Calendar.getInstance();
                                selTo.set(y2, m2, d2, 23, 59, 59);
                                toDate = formatDate(selTo.getTime());
                                // 3) elegir formato
                                showFormatSelectionDialog(tipo);
                            },
                            calTo.get(Calendar.YEAR),
                            calTo.get(Calendar.MONTH),
                            calTo.get(Calendar.DAY_OF_MONTH)
                    );
                    dpTo.setTitle("Fecha Fin");
                    dpTo.show();
                },
                calFrom.get(Calendar.YEAR),
                calFrom.get(Calendar.MONTH),
                calFrom.get(Calendar.DAY_OF_MONTH)
        );
        dpFrom.setTitle("Fecha Inicio");
        dpFrom.show();
    }

    /** Paso 2: elegir formato PDF/XLSX */
    private void showFormatSelectionDialog(String tipo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccione el formato del reporte");

        String[] formats = {"PDF", "Excel (XLSX)"};
        builder.setItems(formats, (dialog, which) -> {
            if (which == 0) {
                downloadFile(tipo, "pdf");
            } else {
                downloadFile(tipo, "xlsx");
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    /** Paso 3: armar URL según tipo/formato + rango de fechas y lanzar DownloadManager */
    private void downloadFile(String tipo, String formato) {
        String endpoint;
        String fileName;

        if ("lecturas".equals(tipo)) {
            if ("pdf".equalsIgnoreCase(formato)) {
                endpoint = ApiRoutes.EXPORT_READINGS_PDF;
                fileName = "lecturas_compostaje.pdf";
            } else {
                endpoint = ApiRoutes.EXPORT_READINGS_XLSX;
                fileName = "lecturas_compostaje.xlsx";
            }
        } else { // "ventas"
            if ("pdf".equalsIgnoreCase(formato)) {
                endpoint = ApiRoutes.EXPORT_SALES_PDF;
                fileName = "ventas_compostaje.pdf";
            } else {
                endpoint = ApiRoutes.EXPORT_SALES_XLSX;
                fileName = "ventas_compostaje.xlsx";
            }
        }

        try {
            // Construimos la URL con query params seguros
            HttpUrl url = HttpUrl.parse(endpoint).newBuilder()
                    .addQueryParameter("idUser", String.valueOf(userId))
                    .addQueryParameter("from", fromDate)
                    .addQueryParameter("to", toDate)
                    .build();

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url.toString()));
            request.setTitle("Descargando reporte de " + tipo);
            request.setDescription("Formato: " + formato.toUpperCase() + " | Rango: " + fromDate + " a " + toDate);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);

            Toast.makeText(this, "Descarga iniciada...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error en descarga: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // --------- helpers ---------
    private String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }

    private Date daysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        return cal.getTime();
    }
}
