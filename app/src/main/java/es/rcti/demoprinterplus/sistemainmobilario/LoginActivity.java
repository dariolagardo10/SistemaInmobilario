package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsuario, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvEstadoConexion;

    private ApiClient apiClient;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsuario = findViewById(R.id.editTextUsername);
        etPassword = findViewById(R.id.editTextPassword);
        btnLogin = findViewById(R.id.buttonLogin);
        progressBar = findViewById(R.id.progressBar);
        tvEstadoConexion = findViewById(R.id.tvConnectivityStatus);

        apiClient = new ApiClient(this);

        btnLogin.setOnClickListener(v -> verificarUsuario());
    }

    private void verificarUsuario() {
        String username = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingrese usuario y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        mostrarProgreso(true);
        Log.d(TAG, "Verificando usuario: " + username);

        apiClient.loginInspector(username, password, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                mostrarProgreso(false);
                try {
                    Log.d(TAG, "Respuesta login: " + response.toString());
                    boolean success = response.optBoolean("success", false);
                    String mensaje = response.optString("message", "");
                    String error = response.optString("error", "");

                    if (!error.isEmpty()) {
                        mostrarMensaje(error);
                        return;
                    }

                    if (success) {
                        String nombre = response.optString("nombre", "");
                        String apellido = response.optString("apellido", "");
                        String legajo = response.optString("legajo", "");
                        String inspectorId = response.optString("inspector_id", "");

                        mostrarMensaje("Bienvenido " + nombre + " " + apellido);

                        // Ir al MainActivity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("USERNAME", username);
                        intent.putExtra("NOMBRE_INSPECTOR", nombre);
                        intent.putExtra("APELLIDO_INSPECTOR", apellido);
                        intent.putExtra("LEGAJO_INSPECTOR", legajo);
                        intent.putExtra("INSPECTOR_ID", inspectorId);
                        startActivity(intent);
                        finish();

                    } else {
                        mostrarMensaje(mensaje.isEmpty() ? "Usuario o contraseña incorrectos" : mensaje);
                    }

                } catch (Exception e) {
                    mostrarMensaje("Error al procesar respuesta: " + e.getMessage());
                    Log.e(TAG, "Error parsing login response", e);
                }
            }

            @Override
            public void onError(String error) {
                mostrarProgreso(false);
                mostrarMensaje(error);
            }
        });
    }

    private void mostrarProgreso(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!mostrar);
    }

    private void mostrarMensaje(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        tvEstadoConexion.setText(msg);
    }
}
