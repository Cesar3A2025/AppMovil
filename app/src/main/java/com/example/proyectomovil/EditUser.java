package com.example.proyectomovil;

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

public class EditUser extends AppCompatActivity {

    private EditText etName, etFirstLastName, etSecondLastName, etUsername, etEmail, etPassword;
    private Button btnSave;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        etName = findViewById(R.id.etName);
        etFirstLastName = findViewById(R.id.etFirstLastName);
        etSecondLastName = findViewById(R.id.etSecondLastName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSave = findViewById(R.id.btnSave);

        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId != -1) {
            fetchUserData(userId);
        }

        btnSave.setOnClickListener(v -> {
            String password = etPassword.getText().toString();
            if (!password.isEmpty() && password.length() < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }
            updateUserData();
        });
    }

    private void fetchUserData(int id) {
        OkHttpClient client = new OkHttpClient();
        String url = MainActivity.Constants.BASE_URL + "get_user_by_id?id=" + id;

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EditUser.this, "Error al obtener datos", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body().string();
                try {
                    JSONObject obj = new JSONObject(resp);
                    if (obj.getBoolean("success")) {
                        JSONObject user = obj.getJSONObject("data");

                        runOnUiThread(() -> {
                            try {
                                etName.setText(user.getString("name"));
                                etFirstLastName.setText(user.getString("firstLastName"));
                                etSecondLastName.setText(user.getString("secondLastName"));
                                etUsername.setText(user.getString("username"));
                                etEmail.setText(user.getString("email"));
                            } catch (JSONException e) {
                                Toast.makeText(EditUser.this, "Error al cargar campos", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(EditUser.this, "Error al parsear datos", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void updateUserData() {
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("id", String.valueOf(userId))
                .add("name", etName.getText().toString())
                .add("firstLastName", etFirstLastName.getText().toString())
                .add("secondLastName", etSecondLastName.getText().toString())
                .add("username", etUsername.getText().toString())
                .add("email", etEmail.getText().toString())
                .add("password", etPassword.getText().toString()) // puede ir vacío si no cambia
                .build();

        Request request = new Request.Builder()
                .url(MainActivity.Constants.BASE_URL +"update_user")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EditUser.this, "Error al actualizar", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> Toast.makeText(EditUser.this, "Actualización exitosa", Toast.LENGTH_SHORT).show());
            }
        });

        Intent intent = new Intent(EditUser.this, MainActivity.class);
        intent.putExtra("USER_ID", userId); // El mismo ID del usuario
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

    }
}
