package com.example.proyectomovil;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.*;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etFirstLastName, etSecondLastName, etUsername, etEmail, etPassword;
    private Button btnSubmitRegister;
    private ProgressDialog progressDialog;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etFirstLastName = findViewById(R.id.etFirstLastName);
        etSecondLastName = findViewById(R.id.etSecondLastName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSubmitRegister = findViewById(R.id.btnSubmitRegister);

        client = new OkHttpClient();

        btnSubmitRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String firstLastName = etFirstLastName.getText().toString().trim();
        String secondLastName = etSecondLastName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || firstLastName.isEmpty() || secondLastName.isEmpty()
                || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registrando...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        RequestBody formBody = new FormBody.Builder()
                .add("name", name)
                .add("firstLastName", firstLastName)
                .add("secondLastName", secondLastName)
                .add("username", username)
                .add("email", email)
                .add("password", password)
                .build();

        String url = MainActivity.Constants.BASE_URL+"register_user.php";


        Request request = new Request.Builder().url(url).post(formBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body().string();
                runOnUiThread(() -> progressDialog.dismiss());
                try {
                    JSONObject json = new JSONObject(resp);
                    boolean success = json.getBoolean("success");
                    String message = json.getString("message");

                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                        if (success) {
                            finish(); // volver al login
                        }
                    });

                } catch (JSONException e) {
                    runOnUiThread(() ->
                            Toast.makeText(RegisterActivity.this, "Respuesta inválida", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
