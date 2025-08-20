package com.example.proyectomovil;

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

public class Reports extends AppCompatActivity {

    private int userId;

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

        CardView cardCompostReadings = findViewById(R.id.cardCompostReadings);
        CardView cardVentas = findViewById(R.id.cardCompostSales);

        // Lecturas de compostaje
        cardCompostReadings.setOnClickListener(v -> showFormatSelectionDialog("lecturas"));

        // Ventas de compostaje
        cardVentas.setOnClickListener(v -> showFormatSelectionDialog("ventas"));
    }

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

    private void downloadFile(String tipo, String formato) {
        String baseUrl = "http://192.168.0.7/composta_esp33/public/api/";
        String url = "";
        String fileName = "";

        // Armado de la URL y nombre del archivo
        if (tipo.equals("lecturas")) {
            if (formato.equals("pdf")) {
                url = baseUrl + "export_readings_pdf?idUser=" + userId;
                fileName = "lecturas_compostaje.pdf";
            } else {
                url = baseUrl + "export_readings_xlsx?idUser=" + userId;
                fileName = "lecturas_compostaje.xlsx";
            }
        } else if (tipo.equals("ventas")) {
            if (formato.equals("pdf")) {
                url = baseUrl + "export_sales_pdf?idUser=" + userId;
                fileName = "ventas_compostaje.pdf";
            } else {
                url = baseUrl + "export_sales_xlsx?idUser=" + userId;
                fileName = "ventas_compostaje.xlsx";
            }
        }

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle("Descargando reporte de " + tipo);
            request.setDescription("Formato: " + formato.toUpperCase());
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);

            Toast.makeText(this, "Descarga iniciada...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error en descarga: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
