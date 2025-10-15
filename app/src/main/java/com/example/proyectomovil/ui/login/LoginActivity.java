package com.example.proyectomovil.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectomovil.R;
import com.example.proyectomovil.data.repository.UsersRepository;
import com.example.proyectomovil.domain.models.User;
import com.example.proyectomovil.ui.main.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private View btnLogin, progress;
    private ImageView btnTogglePassword;
    private final UsersRepository usersRepo = new UsersRepository();
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupPasswordToggle();
        setupKeyboardListeners();
    }

    private void initializeViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progress = findViewById(R.id.progressBar);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);

        btnLogin.setOnClickListener(v -> tryLogin());

        // Configurar hint para email
        etUsername.setHint("ejemplo@correo.com");
    }

    private void setupPasswordToggle() {
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
    }

    private void togglePasswordVisibility() {
        if (passwordVisible) {
            // Ocultar contraseña
            etPassword.setTransformationMethod(new PasswordTransformationMethod());
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            etPassword.setSelection(etPassword.getText().length());
        } else {
            // Mostrar contraseña
            etPassword.setTransformationMethod(null);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
            etPassword.setSelection(etPassword.getText().length());
        }
        passwordVisible = !passwordVisible;
    }

    private void setupKeyboardListeners() {
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                tryLogin();
                return true;
            }
            return false;
        });
    }

    private void tryLogin() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!isValidForm(email, password)) {
            return;
        }

        setLoading(true);
        usersRepo.login(email, password, result -> runOnUiThread(() -> {
            setLoading(false);
            if (!result.isOk()) {
                showError(result.error);
                return;
            }

            User u = result.data;
            if (u == null) {
                showError("Respuesta inválida del servidor");
                return;
            }

            navigateToMain(u);
        }));
    }

    private boolean isValidForm(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etUsername.setError("Formato de email inválido");
            etUsername.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            etPassword.requestFocus();
            return false;
        }

        // Limpiar errores si todo está bien
        etUsername.setError(null);
        etPassword.setError(null);
        return true;
    }

    private void showError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();

        // Mostrar error en campos específicos
        if (error.toLowerCase().contains("contraseña") || error.toLowerCase().contains("password")) {
            etPassword.setError(error);
            etPassword.requestFocus();
        } else if (error.toLowerCase().contains("email") || error.toLowerCase().contains("usuario")) {
            etUsername.setError(error);
            etUsername.requestFocus();
        }
    }

    private void navigateToMain(User user) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("USER_ID", user.id);
        intent.putExtra("USER_NAME", user.name);
        intent.putExtra("USER_EMAIL", user.email);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnTogglePassword.setEnabled(!loading);
        if (progress != null) {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        }

        // Cambiar texto del botón durante carga
        if (btnLogin instanceof android.widget.Button) {
            android.widget.Button button = (android.widget.Button) btnLogin;
            button.setText(loading ? "Iniciando sesión..." : "Iniciar Sesión");
        }
    }
}