package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsuario, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;

    // ✅ Badge offline + mensajes de login separados
    private TextView tvConnectivityStatus; // tvConnectivityStatus (badge)
    private TextView tvLoginMessage;       // tvLoginMessage (mensajes)

    private boolean toastOfflineMostrado = false;

    private ApiClient apiClient;
    private static final String TAG = "LoginActivity";

    // Room
    private AppDb db;
    private InspectorDao inspectorDao;

    // ✅ Callback para cambios de red
    private android.net.ConnectivityManager connectivityManager;
    private android.net.ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsuario = findViewById(R.id.editTextUsername);
        etPassword = findViewById(R.id.editTextPassword);
        btnLogin = findViewById(R.id.buttonLogin);
        progressBar = findViewById(R.id.progressBar);

        // ✅ NUEVOS IDs del XML modificado
        tvConnectivityStatus = findViewById(R.id.tvConnectivityStatus);
        tvLoginMessage = findViewById(R.id.tvLoginMessage);

        apiClient = new ApiClient(this);

        db = AppDb.get(getApplicationContext());
        inspectorDao = db.inspectorDao();

        // ✅ Estado inicial del badge
        updateConnectivityUI(NetworkUtils.isOnline(this));

        // ✅ Escuchar cambios de red (Android N+)
        setupNetworkCallback();

        btnLogin.setOnClickListener(v -> verificarUsuario());
    }

    private void setupNetworkCallback() {
        connectivityManager = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && connectivityManager != null) {
            networkCallback = new android.net.ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(android.net.Network network) {
                    runOnUiThread(() -> updateConnectivityUI(NetworkUtils.isOnline(LoginActivity.this)));
                }

                @Override
                public void onLost(android.net.Network network) {
                    runOnUiThread(() -> updateConnectivityUI(NetworkUtils.isOnline(LoginActivity.this)));
                }

                @Override
                public void onCapabilitiesChanged(android.net.Network network,
                                                  android.net.NetworkCapabilities networkCapabilities) {
                    boolean online = networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            && networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    runOnUiThread(() -> updateConnectivityUI(online));
                }
            };

            try {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "No se pudo registrar NetworkCallback: " + e.getMessage(), e);
            }
        }
    }

    // ✅ Online: badge oculto. Offline: badge visible + toast 1 vez
    private void updateConnectivityUI(boolean online) {
        if (tvConnectivityStatus == null) return;

        if (online) {
            tvConnectivityStatus.setVisibility(View.GONE);
            toastOfflineMostrado = false;
        } else {
            tvConnectivityStatus.setText("Modo Offline");
            tvConnectivityStatus.setVisibility(View.VISIBLE);

            if (!toastOfflineMostrado) {
                Toast.makeText(this, "Trabajando sin conexión", Toast.LENGTH_SHORT).show();
                toastOfflineMostrado = true;
            }
        }
    }

    private void verificarUsuario() {
        String username = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingrese usuario y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Si NO hay internet => login offline con cache
        if (!NetworkUtils.isOnline(this)) {
            loginOffline(username, password);
            return;
        }

        // ✅ Si hay internet => login normal
        loginOnline(username, password);
    }

    // =========================
    // LOGIN ONLINE
    // =========================
    private void loginOnline(String username, String password) {
        mostrarProgreso(true);
        Log.d(TAG, "Login ONLINE. Verificando usuario: " + username);

        apiClient.loginInspector(username, password, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                mostrarProgreso(false);

                try {
                    Log.d(TAG, "Respuesta login: " + response);

                    boolean success = response.optBoolean("success", false);
                    String mensaje = response.optString("message", "");
                    String error = response.optString("error", "");

                    if (!error.isEmpty()) {
                        mostrarMensaje(error);
                        return;
                    }

                    if (!success) {
                        mostrarMensaje(mensaje.isEmpty() ? "Usuario o contraseña incorrectos" : mensaje);
                        return;
                    }

                    String nombre = response.optString("nombre", "");
                    String apellido = response.optString("apellido", "");
                    String legajo = response.optString("legajo", "");
                    String inspectorId = response.optString("inspector_id", "");

                    // ✅ Guardar inspector en Room para login offline
                    String passHash = sha256(password);

                    new Thread(() -> {
                        try {
                            InspectorEntity i = new InspectorEntity();
                            i.inspectorId = safe(inspectorId).isEmpty() ? username : inspectorId; // fallback
                            i.username = username; // ✅ guardar lo que escribe el usuario
                            i.legajo = safe(legajo);
                            i.nombre = nombre;
                            i.apellido = apellido;
                            i.passHash = passHash;
                            i.lastLoginAt = System.currentTimeMillis();

                            inspectorDao.upsert(i);
                            Log.d(TAG, "Inspector guardado en cache offline. id=" + i.inspectorId +
                                    " user=" + i.username + " legajo=" + i.legajo);
                        } catch (Exception e) {
                            Log.e(TAG, "Error guardando inspector cache", e);
                        }
                    }).start();

                    mostrarMensaje("Bienvenido " + nombre + " " + apellido);

                    irAMain(username, nombre, apellido, legajo, inspectorId);

                } catch (Exception e) {
                    mostrarMensaje("Error al procesar respuesta: " + e.getMessage());
                    Log.e(TAG, "Error parsing login response", e);
                }
            }

            @Override
            public void onError(String error) {
                mostrarProgreso(false);
                Log.e(TAG, "Login online falló: " + error);

                // Si se cayó internet justo, probamos offline
                if (!NetworkUtils.isOnline(LoginActivity.this)) {
                    loginOffline(username, password);
                } else {
                    mostrarMensaje(error);
                }
            }
        });
    }

    // =========================
    // LOGIN OFFLINE
    // =========================
    private void loginOffline(String username, String password) {
        mostrarProgreso(true);
        Log.d(TAG, "Login OFFLINE. Usuario: " + username);

        new Thread(() -> {
            try {
                // ✅ Buscar primero por username
                InspectorEntity local = inspectorDao.findByUsername(username);

                // ✅ Si no existe, probar por legajo (por si el usuario escribe legajo)
                if (local == null) {
                    local = inspectorDao.findByLegajo(username);
                }

                // ✅ Último recurso: último logueado
                if (local == null) {
                    local = inspectorDao.last();
                }

                InspectorEntity finalLocal = local;

                runOnUiThread(() -> {
                    mostrarProgreso(false);

                    if (finalLocal == null) {
                        mostrarMensaje("Sin conexión. No hay ningún inspector guardado en este dispositivo (primero logueá una vez con internet).");
                        return;
                    }

                    // Validar contraseña (hash)
                    String passHashIngresada = sha256(password);

                    if (finalLocal.passHash != null && !finalLocal.passHash.isEmpty()) {
                        if (!passHashIngresada.equals(finalLocal.passHash)) {
                            mostrarMensaje("Sin conexión. Contraseña incorrecta (modo offline).");
                            return;
                        }
                    }

                    mostrarMensaje("Modo offline ✅ Bienvenido " + safe(finalLocal.nombre) + " " + safe(finalLocal.apellido));

                    irAMain(
                            username,
                            safe(finalLocal.nombre),
                            safe(finalLocal.apellido),
                            safe(finalLocal.legajo),
                            safe(finalLocal.inspectorId)
                    );
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loginOffline", e);
                runOnUiThread(() -> {
                    mostrarProgreso(false);
                    mostrarMensaje("Error en login offline: " + e.getMessage());
                });
            }
        }).start();
    }

    private void irAMain(String username, String nombre, String apellido, String legajo, String inspectorId) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("USERNAME", username);
        intent.putExtra("NOMBRE_INSPECTOR", nombre);
        intent.putExtra("APELLIDO_INSPECTOR", apellido);
        intent.putExtra("LEGAJO_INSPECTOR", legajo);
        intent.putExtra("INSPECTOR_ID", inspectorId);
        startActivity(intent);
        finish();
    }

    private void mostrarProgreso(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!mostrar);
    }

    // ✅ Mensajes: van al TextView nuevo (no al badge)
    private void mostrarMensaje(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        if (tvLoginMessage != null) {
            tvLoginMessage.setText(msg);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ✅ Unregister callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && connectivityManager != null
                && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregisterNetworkCallback: " + e.getMessage(), e);
            }
        }
    }
}
