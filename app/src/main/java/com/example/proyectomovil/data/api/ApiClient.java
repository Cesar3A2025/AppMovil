package com.example.proyectomovil.data.api;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
public final class ApiClient {
    private static OkHttpClient instance;
    private ApiClient() {}
    public static OkHttpClient get() {
        if (instance == null) {
            instance = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .build();
        }
        return instance;
    }
}

