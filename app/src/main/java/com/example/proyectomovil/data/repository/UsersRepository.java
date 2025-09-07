package com.example.proyectomovil.data.repository;

import com.example.proyectomovil.data.api.ApiClient;
import com.example.proyectomovil.data.api.ApiRoutes;
import com.example.proyectomovil.domain.models.User;
import com.example.proyectomovil.utils.Result;

import org.json.JSONObject;

import okhttp3.*;

public class UsersRepository {

    public interface CallbackUser {
        void onComplete(Result<User> result);
    }

    // Login
    public void login(String email, String password, CallbackUser cb){
        RequestBody body = new FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .build();

        Request req = new Request.Builder()
                .url(ApiRoutes.LOGIN)
                .post(body)
                .build();

        ApiClient.get().newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                cb.onComplete(Result.fail("Sin conexión: " + e.getMessage()));
            }

            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody rb = response.body()) {
                    int code = response.code();
                    String ctype = response.header("Content-Type", "");
                    String raw = (rb == null) ? "" : rb.string();

                    android.util.Log.d("LOGIN","code=" + code + " ctype=" + ctype + " body=" + preview(raw));

                    if (!response.isSuccessful()) {
                        cb.onComplete(Result.fail("HTTP " + code + ". " + hintFromBody(raw)));
                        return;
                    }
                    if (!looksJson(raw)) {
                        cb.onComplete(Result.fail("Respuesta no-JSON del servidor: " + preview(raw)));
                        return;
                    }

                    String clean = raw.trim();

                    // Intento 1: { "user": { ... } }
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(clean);
                        if (json.has("user")) {
                            org.json.JSONObject ju = json.optJSONObject("user");
                            User u = (ju != null) ? User.fromJson(ju) : null;
                            if (u != null) { cb.onComplete(Result.ok(u)); return; }
                        }
                    } catch (Exception ignore){}

                    // Intento 2: { "success": true, "id":..., "name":..., "email":... }
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(clean);
                        if (json.optBoolean("success", false)) {
                            User u = new User();
                            u.id    = json.optInt("id");
                            u.name  = json.optString("name");
                            u.email = json.optString("email");
                            u.username = json.optString("username", u.email);
                            u.role     = json.optString("role", "");
                            u.firstLastName  = json.optString("firstLastName", "");
                            u.secondLastName = json.optString("secondLastName", "");
                            u.state    = json.optString("state", "");
                            cb.onComplete(Result.ok(u));
                            return;
                        }
                    } catch (Exception ignore){}

                    // Intento 3: { "status": true, "data": { "user": { ... } } }
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(clean);
                        if (json.optBoolean("status", false)) {
                            org.json.JSONObject data = json.optJSONObject("data");
                            if (data != null && data.has("user")) {
                                User u = User.fromJson(data.optJSONObject("user"));
                                if (u != null) { cb.onComplete(Result.ok(u)); return; }
                            }
                        }
                    } catch (Exception ignore){}

                    // Intento 4: { "message": "..." }
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(clean);
                        String msg = json.optString("message");
                        if (!msg.isEmpty()) { cb.onComplete(Result.fail(msg)); return; }
                    } catch (Exception ignore){}

                    cb.onComplete(Result.fail("Error parseando login: " + preview(clean)));
                } catch (Exception ex) {
                    cb.onComplete(Result.fail("Error parseando login: " + ex.getMessage()));
                }
            }

            // Helpers:
            private boolean looksJson(String s) {
                String t = (s == null) ? "" : s.trim();
                return t.startsWith("{") || t.startsWith("[");
            }
            private String preview(String s) {
                if (s == null) return "";
                s = s.replace("\n", " ").replace("\r", " ");
                return s.length() > 200 ? s.substring(0, 200) + "..." : s;
            }
            private String hintFromBody(String s) {
                if (s == null) return "";
                if (s.contains("CSRF") || s.contains("_token")) return "¿Ruta web en lugar de API? Revisa CSRF.";
                if (s.contains("Not Found") || s.contains("<title>404")) return "Ruta /login no encontrada (404).";
                return preview(s);
            }
        });
    }

    // Update user (POST /update_user con id en el body)
    public void updateUser(int id, User payload, CallbackUser cb){
        FormBody.Builder fb = new FormBody.Builder()
                .add("id", String.valueOf(id))
                .add("name", payload.name)
                .add("firstLastName", payload.firstLastName)
                .add("secondLastName", payload.secondLastName)
                .add("username", payload.username)
                .add("email", payload.email);

        // contraseña opcional si tu backend lo acepta
        // if (payload.password != null && !payload.password.isEmpty()) {
        //     fb.add("password", payload.password);
        // }

        RequestBody body = fb.build();

        Request req = new Request.Builder()
                .url(ApiRoutes.USER_UPDATE)   // <-- POST /update_user
                .post(body)                   // <-- IMPORTANTE: POST, no PUT
                .build();

        ApiClient.get().newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                cb.onComplete(Result.fail("Sin conexión: " + e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody rb = response.body()) {
                    String raw = (rb == null) ? "" : rb.string();

                    if (!response.isSuccessful()) {
                        cb.onComplete(Result.fail("HTTP " + response.code() + ". " + preview(raw)));
                        return;
                    }

                    // Algunos backends devuelven solo {success:true}; otros devuelven {success:true, data:{...}}
                    try {
                        JSONObject json = new JSONObject(raw);
                        boolean ok = json.optBoolean("success", false);

                        if (ok) {
                            JSONObject data = json.optJSONObject("data");
                            if (data != null) {
                                User u = User.fromJson(data);
                                cb.onComplete(Result.ok(u));
                            } else {
                                // Si no hay data, devolvemos el payload actualizado (mejor que null)
                                cb.onComplete(Result.ok(payload));
                            }
                            return;
                        }

                        String msg = json.optString("message", "Error actualizando usuario");
                        cb.onComplete(Result.fail(msg));
                    } catch (Exception ex) {
                        // Si no es JSON o falla parseo:
                        cb.onComplete(Result.fail("Error parseando update: " + preview(raw)));
                    }
                } catch (Exception ex) {
                    cb.onComplete(Result.fail("Error parseando update: " + ex.getMessage()));
                }
            }

            private String preview(String s) {
                if (s == null) return "";
                s = s.replace("\n", " ").replace("\r", " ");
                return s.length() > 200 ? s.substring(0, 200) + "..." : s;
            }
        });
    }
}
