package com.example.proyectomovil.data.repository;

import com.example.proyectomovil.data.api.ApiClient;
import com.example.proyectomovil.data.api.ApiRoutes;
import com.example.proyectomovil.domain.models.Materials;
import com.example.proyectomovil.utils.Result;

import org.json.JSONException;

import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MaterialsRepository {

    public interface CallbackMaterials {
        void onComplete(Result<List<Materials>> result);
    }

    public void getAll(CallbackMaterials cb){
        Request req = new Request.Builder()
                .url(ApiRoutes.MATERIALS)   // GET /materials
                .get()
                .build();

        ApiClient.get().newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                cb.onComplete(Result.fail("Sin conexión: " + e.getMessage()));
            }

            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody rb = response.body()) {
                    final String raw = (rb == null) ? "" : rb.string();

                    if (!response.isSuccessful()) {
                        cb.onComplete(Result.fail("HTTP " + response.code() + ": " + preview(raw)));
                        return;
                    }

                    try {
                        List<Materials> list = Materials.parseList(raw);
                        cb.onComplete(Result.ok(list));
                    } catch (JSONException jex) {
                        cb.onComplete(Result.fail("Parseo materiales: " + preview(raw)));
                    }

                } catch (Exception ex) {
                    cb.onComplete(Result.fail("Error obteniendo materiales: " + ex.getMessage()));
                }
            }
        });
    }

    // --- Helpers ---
    private String preview(String s) {
        if (s == null) return "";
        String t = s.replace("\n", " ").replace("\r", " ");
        return t.length() > 200 ? t.substring(0, 200) + "..." : t;
    }

    public void getAllFiltered(
            String search,
            String clasif,
            String aptitude,
            String typeCategory,
            int perPage,
            String sort,
            String dir,
            CallbackMaterials cb
    ) {
        okhttp3.HttpUrl.Builder b = okhttp3.HttpUrl.parse(ApiRoutes.MATERIALS).newBuilder()
                .addQueryParameter("per_page", String.valueOf(perPage))
                .addQueryParameter("sort", (sort == null || sort.isEmpty()) ? "created_at" : sort)
                .addQueryParameter("dir",  (dir  == null || dir.isEmpty())  ? "desc"       : dir);

        if (search != null && !search.trim().isEmpty()) b.addQueryParameter("search", search.trim());
        if (clasif != null && !clasif.trim().isEmpty()) b.addQueryParameter("clasification", clasif.trim());
        if (aptitude != null && !aptitude.trim().isEmpty()) b.addQueryParameter("aptitude", aptitude.trim());
        if (typeCategory != null && !typeCategory.trim().isEmpty()) b.addQueryParameter("type_category", typeCategory.trim());

        okhttp3.Request req = new okhttp3.Request.Builder()
                .url(b.build())
                .get()
                .addHeader("Accept", "application/json")
                .build();

        ApiClient.get().newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                cb.onComplete(Result.fail("Sin conexión: " + e.getMessage()));
            }
            @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                try (okhttp3.ResponseBody rb = response.body()) {
                    final String raw = (rb == null) ? "" : rb.string();
                    if (!response.isSuccessful()) {
                        cb.onComplete(Result.fail("HTTP " + response.code() + ": " + preview(raw)));
                        return;
                    }
                    try {
                        List<Materials> list = Materials.parseList(raw); // soporta {data:[...]} o [...]
                        cb.onComplete(Result.ok(list));
                    } catch (org.json.JSONException jex) {
                        cb.onComplete(Result.fail("Parseo materiales: " + preview(raw)));
                    }
                } catch (Exception ex) {
                    cb.onComplete(Result.fail("Error obteniendo materiales: " + ex.getMessage()));
                }
            }
            private String preview(String s) {
                if (s == null) return "";
                String t = s.replace("\n", " ").replace("\r", " ");
                return t.length() > 200 ? t.substring(0, 200) + "..." : t;
            }
        });
    }
}
