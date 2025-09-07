package com.example.proyectomovil.data.repository;

import com.example.proyectomovil.data.api.ApiClient;
import com.example.proyectomovil.data.api.ApiRoutes;
import com.example.proyectomovil.domain.models.Materials;
import com.example.proyectomovil.utils.Result;

import org.json.JSONArray;

import java.util.List;

import okhttp3.*;

public class MaterialsRepository {

    public interface CallbackMaterials {
        void onComplete(Result<List<Materials>> result);
    }

    public void getAll(CallbackMaterials cb){
        Request req = new Request.Builder().url(ApiRoutes.MATERIALS).get().build();
        ApiClient.get().newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                cb.onComplete(Result.fail("Sin conexión: " + e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody rb = response.body()) {
                    if (!response.isSuccessful() || rb == null){
                        cb.onComplete(Result.fail("Error obteniendo materiales"));
                        return;
                    }
                    cb.onComplete(Result.ok(Materials.parseList(rb.string())));
                } catch(Exception ex){
                    cb.onComplete(Result.fail("Parseo materiales: " + ex.getMessage()));
                }
            }
        });
    }
}
