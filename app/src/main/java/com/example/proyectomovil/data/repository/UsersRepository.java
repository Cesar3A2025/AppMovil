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

    // 🔹 LOGIN
    public void login(String email, String password, CallbackUser cb) {
        RequestBody body = new FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .build();

        Request req = new Request.Builder()
                .url(ApiRoutes.LOGIN)
                .post(body)
                .build();

        ApiClient.get().newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                cb.onComplete(Result.fail("Sin conexión: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody rb = response.body()) {
                    String raw = (rb == null) ? "" : rb.string();
                    android.util.Log.d("LOGIN_RAW", raw);

                    if (!response.isSuccessful()) {
                        cb.onComplete(Result.fail("HTTP " + response.code() + ": " + preview(raw)));
                        return;
                    }

                    if (!looksJson(raw)) {
                        cb.onComplete(Result.fail("Respuesta no JSON: " + preview(raw)));
                        return;
                    }

                    JSONObject json = new JSONObject(raw);

                    // ✅ 1️⃣ { "success": true, "user": { ... } }
                    if (json.optBoolean("success", false) && json.has("user")) {
                        User u = User.fromJson(json.getJSONObject("user"));
                        ensureUsername(u);
                        cb.onComplete(Result.ok(u));
                        return;
                    }

                    // ✅ 2️⃣ { "status": true, "data": { "user": { ... } } }
                    if (json.optBoolean("status", false)) {
                        JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            if (data.has("user")) {
                                User u = User.fromJson(data.getJSONObject("user"));
                                ensureUsername(u);
                                cb.onComplete(Result.ok(u));
                                return;
                            } else {
                                User u = User.fromJson(data);
                                ensureUsername(u);
                                cb.onComplete(Result.ok(u));
                                return;
                            }
                        }
                    }

                    // ✅ 3️⃣ { "data": { ... } }
                    if (json.has("data")) {
                        User u = User.fromJson(json.getJSONObject("data"));
                        ensureUsername(u);
                        cb.onComplete(Result.ok(u));
                        return;
                    }

                    // ✅ 4️⃣ { "user": { ... } }
                    if (json.has("user")) {
                        User u = User.fromJson(json.getJSONObject("user"));
                        ensureUsername(u);
                        cb.onComplete(Result.ok(u));
                        return;
                    }

                    // ✅ 5️⃣ Campos planos (sin contenedor)
                    if (json.has("email")) {
                        User u = User.fromJson(json);
                        ensureUsername(u);
                        cb.onComplete(Result.ok(u));
                        return;
                    }

                    // ⚠️ No coincide ningún formato
                    cb.onComplete(Result.fail("Formato desconocido: " + preview(raw)));

                } catch (Exception ex) {
                    cb.onComplete(Result.fail("Error procesando login: " + ex.getMessage()));
                }
            }

            private void ensureUsername(User u) {
                if (u != null) {
                    if (u.username == null || u.username.trim().isEmpty()) {
                        // fallback
                        u.username = (u.name != null && !u.name.isEmpty())
                                ? u.name
                                : (u.email != null ? u.email.split("@")[0] : "Usuario");
                    }
                }
            }

            private boolean looksJson(String s) {
                if (s == null) return false;
                String t = s.trim();
                return t.startsWith("{") || t.startsWith("[");
            }

            private String preview(String s) {
                if (s == null) return "";
                s = s.replace("\n", " ").replace("\r", " ");
                return s.length() > 200 ? s.substring(0, 200) + "..." : s;
            }
        });
    }

    // 🔹 UPDATE USER
    public void updateUser(int id, User payload, CallbackUser cb) {
        FormBody.Builder fb = new FormBody.Builder()
                .add("id", String.valueOf(id))
                .add("name", payload.name)
                .add("firstLastName", payload.firstLastName)
                .add("secondLastName", payload.secondLastName)
                .add("username", payload.username)
                .add("email", payload.email);

        RequestBody body = fb.build();

        Request req = new Request.Builder()
                .url(ApiRoutes.USER_UPDATE)
                .post(body)
                .build();

        ApiClient.get().newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                cb.onComplete(Result.fail("Sin conexión: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody rb = response.body()) {
                    String raw = (rb == null) ? "" : rb.string();

                    if (!response.isSuccessful()) {
                        cb.onComplete(Result.fail("HTTP " + response.code() + ": " + preview(raw)));
                        return;
                    }

                    JSONObject json = new JSONObject(raw);
                    if (json.optBoolean("success", false)) {
                        JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            User u = User.fromJson(data);
                            cb.onComplete(Result.ok(u));
                        } else {
                            cb.onComplete(Result.ok(payload));
                        }
                    } else {
                        cb.onComplete(Result.fail(json.optString("message", "Error actualizando usuario")));
                    }

                } catch (Exception ex) {
                    cb.onComplete(Result.fail("Error procesando update: " + ex.getMessage()));
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
