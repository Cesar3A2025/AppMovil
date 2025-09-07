package com.example.proyectomovil.data.repository;

import com.example.proyectomovil.data.api.ApiClient;
import com.example.proyectomovil.data.api.ApiRoutes;
import com.example.proyectomovil.domain.models.Reading;
import com.example.proyectomovil.utils.Result;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.*;

public class ReadingsRepository {

    public interface CallbackReadings {
        void onComplete(Result<List<Reading>> result);
    }

    public void getLatest(CallbackReadings cb){
        Request req = new Request.Builder().url(ApiRoutes.READINGS_LATEST).get().build();
        ApiClient.get().newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                cb.onComplete(Result.fail("Sin conexión: " + e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody rb = response.body()) {
                    if (!response.isSuccessful() || rb == null){
                        cb.onComplete(Result.fail("Error obteniendo lecturas"));
                        return;
                    }
                    JSONArray arr = new JSONArray(rb.string());
                    List<Reading> list = new ArrayList<>();
                    for (int i=0;i<arr.length();i++){
                        JSONObject o = arr.getJSONObject(i);
                        list.add(Reading.fromJson(o));
                    }
                    cb.onComplete(Result.ok(list));
                } catch(Exception ex){
                    cb.onComplete(Result.fail("Parseo lecturas: " + ex.getMessage()));
                }
            }
        });
    }
}
