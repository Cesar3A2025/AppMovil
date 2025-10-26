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
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.example.proyectomovil.ui.base.BaseDrawerActivity;
import com.example.proyectomovil.data.api.ApiRoutes;

public class Reports extends BaseDrawerActivity {

    private int userId;
    private String fromDate;
    private String toDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentWithDrawer(R.layout.activity_reports);
        setTitle("Reportes");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userId = getIntent().getIntExtra("USER_ID", 1);

        // Rango por defecto (últimos 30 días)
        toDate = formatDate(new Date());
        fromDate = formatDate(daysAgo(30));

        CardView cardCompostReadings = findViewById(R.id.cardCompostReadings);
        CardView cardVentas = findViewById(R.id.cardCompostSales);

        // ✅ Reporte de lecturas con rango predefinido (hoy, 7 o 30 días)
        cardCompostReadings.setOnClickListener(v -> showReadingsRangeDialog());

        // ✅ Reporte de ventas con calendario personalizado
        cardVentas.setOnClickListener(v -> pickDateRangeThenFormat("ventas"));
    }

    // ------------------------------------------------------------
    // 🔹 Selección de rango para lecturas (Hoy / 7 / 30 días)
    // ------------------------------------------------------------
    // ------------------------------------------------------------
// 🔹 Selección de rango y formato para lecturas
// ------------------------------------------------------------
    private void showReadingsRangeDialog() {
        String[] rangeOptions = {"Hoy", "Últimos 7 días", "Últimos 30 días"};
        String[] rangeValues = {"day", "week", "month"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccione el rango de lectura");
        builder.setItems(rangeOptions, (dialog, whichRange) -> {
            String selectedRange = rangeValues[whichRange];

            // ✅ Segundo diálogo: elegir formato
            AlertDialog.Builder formatDialog = new AlertDialog.Builder(this);
            formatDialog.setTitle("Seleccione el formato del reporte");
            String[] formats = {"PDF", "Excel (XLSX)"};

            formatDialog.setItems(formats, (dialog2, whichFormat) -> {
                String format = formats[whichFormat].toLowerCase().contains("pdf") ? "pdf" : "xlsx";
                String url;
                String fileName;

                if (format.equals("pdf")) {
                    url = ApiRoutes.DOWNLOAD_READINGS_PDF
                            + "?range=" + selectedRange
                            + "&idUser=" + userId;
                    fileName = "reporte_lecturas_" + selectedRange + ".pdf";
                } else {
                    url = ApiRoutes.EXPORT_READINGS_XLSX
                            + "?range=" + selectedRange
                            + "&idUser=" + userId;
                    fileName = "reporte_lecturas_" + selectedRange + ".xlsx";
                }

                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setTitle("Descargando reporte de lecturas");
                    request.setDescription("Rango: " + rangeOptions[whichRange] + " • Formato: " + format.toUpperCase());
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(this, "Descarga iniciada...", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error en descarga: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            formatDialog.setNegativeButton("Cancelar", null);
            formatDialog.show();
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    // ------------------------------------------------------------
    // 🔹 Calendario para seleccionar fechas (solo para ventas)
    // ------------------------------------------------------------
    private void pickDateRangeThenFormat(String tipo) {
        Calendar calFrom = Calendar.getInstance();
        DatePickerDialog dpFrom = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    Calendar selFrom = Calendar.getInstance();
                    selFrom.set(y, m, d, 0, 0, 0);
                    fromDate = formatDate(selFrom.getTime());

                    Calendar calTo = Calendar.getInstance();
                    DatePickerDialog dpTo = new DatePickerDialog(
                            this,
                            (view2, y2, m2, d2) -> {
                                Calendar selTo = Calendar.getInstance();
                                selTo.set(y2, m2, d2, 23, 59, 59);
                                toDate = formatDate(selTo.getTime());
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

    private void showFormatSelectionDialog(String tipo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccione el formato del reporte");

        String[] formats = {"PDF", "Excel (XLSX)"};
        builder.setItems(formats, (dialog, which) -> {
            if (which == 0) {
                downloadSalesReport("pdf");
            } else {
                downloadSalesReport("xlsx");
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    // ------------------------------------------------------------
    // 🔹 Generación del reporte de ventas
    // ------------------------------------------------------------
    private void downloadSalesReport(String formato) {
        String url;
        String fileName;

        if ("pdf".equalsIgnoreCase(formato)) {
            url = ApiRoutes.DOWNLOAD_SALES_PDF
                    + "?from=" + fromDate
                    + "&to=" + toDate
                    + "&idUser=" + userId;
        } else {
            url = ApiRoutes.EXPORT_SALES_XLSX
                    + "?from=" + fromDate
                    + "&to=" + toDate
                    + "&idUser=" + userId;
        }

        fileName = "ventas_compostaje_" + fromDate + "_a_" + toDate + "." + formato.toLowerCase();

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle("Descargando reporte de ventas");
            request.setDescription("Formato: " + formato.toUpperCase());
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(this, "Descarga iniciada...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error en descarga: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------
    // 🔹 Utilidades
    // ------------------------------------------------------------
    private String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }

    private Date daysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        return cal.getTime();
    }
}
