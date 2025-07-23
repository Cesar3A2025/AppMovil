package com.example.proyectomovil;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private ProgressDialog progressDialog;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        client = new OkHttpClient();

        btnLogin.setOnClickListener(v -> login());
    }

    private void login() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Iniciando sesión...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .build();

        // Cambia esta URL por la ruta real a tu archivo PHP
        String url = "http://192.168.0.4/ProyectoGrado/login.php"; // O tu IP real en red local

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Error de red: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body().string();
                runOnUiThread(() -> progressDialog.dismiss());
                try {
                    JSONObject json = new JSONObject(resp);
                    boolean success = json.getBoolean("success");
                    if (success) {
                        int userId = json.getInt("id");
                        String name = json.getString("name");

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("USER_ID", userId);
                        intent.putExtra("USER_NAME", name);
                        startActivity(intent);
                        finish();
                    } else {
                        String message = json.optString("message", "Error desconocido");
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show());
                    }
                } catch (JSONException e) {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "Respuesta inválida del servidor", Toast.LENGTH_LONG).show());
                }
            }
        });
    }
}
