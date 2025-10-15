package com.example.proyectomovil.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectomovil.R;
import com.example.proyectomovil.data.api.ApiClient;
import com.example.proyectomovil.data.api.ApiRoutes;
import com.example.proyectomovil.ui.main.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.example.proyectomovil.ui.base.BaseDrawerActivity;

public class EditUser extends BaseDrawerActivity {

    private EditText etName, etFirstLastName, etSecondLastName, etUsername, etEmail, etPassword, etCurrentPassword;
    private Button btnSave;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentWithDrawer(R.layout.activity_edit_user);
        setTitle("EdicionUsuario");
        etName = findViewById(R.id.etName);
        etFirstLastName = findViewById(R.id.etFirstLastName);
        etSecondLastName = findViewById(R.id.etSecondLastName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        btnSave = findViewById(R.id.btnSave);

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId != -1) {
            fetchUserData(userId);
        }

        btnSave.setOnClickListener(v -> {
            String newPassword = etPassword.getText().toString();
            String currentPassword = etCurrentPassword.getText().toString();

            if (!newPassword.isEmpty() && newPassword.length() < 6) {
                Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.isEmpty() && currentPassword.isEmpty()) {
                Toast.makeText(this, "Debe ingresar la contraseña actual para cambiarla", Toast.LENGTH_SHORT).show();
                return;
            }

            updateUserData();
        });
    }

    private void fetchUserData(int id) {
        OkHttpClient client = ApiClient.get();
        okhttp3.HttpUrl url = okhttp3.HttpUrl.parse(ApiRoutes.USER_GET_BY_ID)
                .newBuilder()
                .addQueryParameter("id", String.valueOf(id))
                .build();

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EditUser.this, "Error al obtener datos: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(EditUser.this, "HTTP " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }
                try {
                    JSONObject obj = new JSONObject(resp);
                    if (obj.optBoolean("success", false)) {
                        JSONObject user = obj.getJSONObject("data");
                        runOnUiThread(() -> {
                            etName.setText(user.optString("name", ""));
                            etFirstLastName.setText(user.optString("firstLastName", ""));
                            etSecondLastName.setText(user.optString("secondLastName", ""));
                            etUsername.setText(user.optString("username", ""));
                            etEmail.setText(user.optString("email", ""));
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(EditUser.this, "Usuario no encontrado", Toast.LENGTH_SHORT).show());
                    }
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(EditUser.this, "Error al parsear datos", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void updateUserData() {
        OkHttpClient client = ApiClient.get();

        FormBody.Builder fb = new FormBody.Builder()
                .add("id", String.valueOf(userId))
                .add("name", etName.getText().toString())
                .add("firstLastName", etFirstLastName.getText().toString())
                .add("secondLastName", etSecondLastName.getText().toString())
                .add("username", etUsername.getText().toString())
                .add("email", etEmail.getText().toString());

        String pwd = etPassword.getText().toString();
        String currentPwd = etCurrentPassword.getText().toString();

        if (!pwd.isEmpty()) {
            fb.add("password", pwd);
            fb.add("current_password", currentPwd);
        }

        RequestBody formBody = fb.build();

        Request request = new Request.Builder()
                .url(ApiRoutes.USER_UPDATE)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EditUser.this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject json = new JSONObject(resp);
                    boolean success = json.optBoolean("success", false);
                    String message = json.optString("message", "Error desconocido");

                    runOnUiThread(() -> {
                        Toast.makeText(EditUser.this, message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            Intent intent = new Intent(EditUser.this, MainActivity.class);
                            intent.putExtra("USER_ID", userId);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                    });
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(EditUser.this, "Respuesta inválida del servidor", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
