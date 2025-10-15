package com.example.proyectomovil.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.proyectomovil.R;
import com.example.proyectomovil.data.api.ApiRoutes;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AlertsWorker extends Worker {

    private static final String CHANNEL_ID = "alerts_channel";

    public AlertsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        OkHttpClient client = new OkHttpClient();
        int userId = getInputData().getInt("userId", -1);
        HttpUrl url = HttpUrl.parse(ApiRoutes.ALERTS)
                .newBuilder()
                .addQueryParameter("idUser", String.valueOf(userId))
                .build();

        Request request = new Request.Builder().url(url).get().build();

        try {
            // Usamos execute() para que sea síncrono y WorkManager espere la respuesta
            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) return Result.retry();
            if (response.body() == null) return Result.retry();

            String resp = response.body().string();
            JSONObject json = new JSONObject(resp);
            JSONArray alerts = json.optJSONArray("alerts");

            if (alerts != null && alerts.length() > 0) {
                StringBuilder msg = new StringBuilder();
                for (int i = 0; i < alerts.length(); i++) {
                    msg.append("• ").append(alerts.getString(i)).append("\n");
                }
                showNotification(msg.toString());
            }

            return Result.success();

        } catch (IOException e) {
            e.printStackTrace();
            return Result.retry(); // Reintentar en caso de error de red
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure(); // Error inesperado
        }
    }

    private void showNotification(String message) {
        NotificationManager manager = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager == null) return;

        // Crear canal si es Android >= Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alertas de compostaje",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Canal para notificaciones de alertas de compostaje");
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("🚨 Alertas de Compostaje")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setSmallIcon(R.drawable.ic_alert) // reemplaza con tu icono
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // ID único para cada notificación
        int notificationId = (int) System.currentTimeMillis();
        manager.notify(notificationId, builder.build());
    }
}
