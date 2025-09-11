package com.example.proyectomovil.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectomovil.R;
import com.example.proyectomovil.data.repository.UsersRepository;
import com.example.proyectomovil.domain.models.User;
import com.example.proyectomovil.ui.main.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private View btnLogin, progress; // ProgressBar
    private final UsersRepository usersRepo = new UsersRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        progress   = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> tryLogin());
    }

    private void tryLogin() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        usersRepo.login(email, password, result -> runOnUiThread(() -> {
            setLoading(false);
            if (!result.isOk()) {
                Toast.makeText(this, result.error, Toast.LENGTH_LONG).show();
                return;
            }

            User u = result.data;
            if (u == null) {
                Toast.makeText(this, "Respuesta inválida del servidor", Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("USER_ID",    u.id);
            intent.putExtra("USER_NAME",  u.name);
            intent.putExtra("USER_EMAIL", u.email);
            startActivity(intent);
            finish();
        }));
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        if (progress != null) {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }
}
