package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private Snackbar snackbarActasPendientes;
    private View rootView;
    private boolean mostrandoExito = false; // Flag para evitar ocultar mensaje de éxito

    private static final String TAG = "MainActivity";
    private static final String UNIQUE_SYNC_NAME = "SYNC_INMO_UNIQUE";

    // 🔹 Inspector
    private String nombreInspector;
    private String apellidoInspector;
    private String legajoInspector;
    private String inspectorId;

    // 🔹 Room
    private AppDb db;
    private ActaDao actaDao;

    // Para evitar doble click mientras sincroniza
    private boolean syncing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // =========================
        // DATOS LOGIN
        // =========================
        Intent loginIntent = getIntent();
        nombreInspector = loginIntent.getStringExtra("NOMBRE_INSPECTOR");
        apellidoInspector = loginIntent.getStringExtra("APELLIDO_INSPECTOR");
        legajoInspector = loginIntent.getStringExtra("LEGAJO_INSPECTOR");
        inspectorId = loginIntent.getStringExtra("INSPECTOR_ID");

        Log.d(TAG, "Inspector: " + nombreInspector + " " + apellidoInspector +
                " | Legajo: " + legajoInspector + " | ID: " + inspectorId);

        // =========================
        // DB
        // =========================
        db = AppDb.get(getApplicationContext());
        actaDao = db.actaDao();

        // =========================
        // UI
        // =========================
        webView = findViewById(R.id.webView);
        rootView = findViewById(R.id.coordinatorLayout);

        setupWebView();

        // ✅ Observa el estado del worker y actualiza el contador al finalizar
        setupSyncObserver();

        actualizarSnackbarPendientes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarSnackbarPendientes();
    }

    // =========================
    // OBSERVAR WORKMANAGER
    // =========================
    private void setupSyncObserver() {
        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(UNIQUE_SYNC_NAME)
                .observe(this, workInfos -> {
                    if (workInfos == null || workInfos.isEmpty()) return;

                    androidx.work.WorkInfo info = workInfos.get(0);
                    androidx.work.WorkInfo.State st = info.getState();

                    Log.d(TAG, "🔄 SyncWorker state=" + st);

                    if (st == androidx.work.WorkInfo.State.RUNNING) {
                        syncing = true;
                        mostrarSnackbarSincronizando();
                        return;
                    }

                    if (st.isFinished()) {
                        syncing = false;
                        
                        // ✅ Mostrar mensaje de éxito verde
                        if (st == androidx.work.WorkInfo.State.SUCCEEDED) {
                            mostrarSnackbarExito();
                        } else {
                            // Si falló, actualizar directamente
                            actualizarSnackbarPendientes();
                        }
                    }
                });
    }

    // =========================
    // SNACKBAR - ACTAS PENDIENTES
    // =========================
    private void actualizarSnackbarPendientes() {
        // No actualizar si estamos mostrando el mensaje de éxito
        if (mostrandoExito) {
            return;
        }
        
        new Thread(() -> {
            int count = actaDao.countPending();
            runOnUiThread(() -> {
                if (count > 0) {
                    mostrarSnackbarConContador(count);
                } else {
                    ocultarSnackbar();
                }
            });
        }).start();
    }

    private void mostrarSnackbarConContador(int count) {
        String mensaje = count == 1
                ? "1 acta sin sincronizar"
                : count + " actas sin sincronizar";

        // Si ya existe el Snackbar, actualizar texto
        if (snackbarActasPendientes != null && snackbarActasPendientes.isShown()) {
            snackbarActasPendientes.setText(mensaje);
            return;
        }

        // Crear nuevo Snackbar
        snackbarActasPendientes = Snackbar.make(rootView, mensaje, Snackbar.LENGTH_INDEFINITE)
                .setAction("SINCRONIZAR", v -> {
                    if (!syncing) {
                        confirmarSincronizarTodas(count);
                    }
                })
                .setActionTextColor(getResources().getColor(android.R.color.holo_green_light));

        snackbarActasPendientes.show();
    }

    private void mostrarSnackbarSincronizando() {
        if (snackbarActasPendientes != null && snackbarActasPendientes.isShown()) {
            snackbarActasPendientes.dismiss();
        }

        snackbarActasPendientes = Snackbar.make(
                rootView,
                "Sincronizando actas...",
                Snackbar.LENGTH_INDEFINITE
        );
        snackbarActasPendientes.show();
    }

    private void ocultarSnackbar() {
        if (snackbarActasPendientes != null && snackbarActasPendientes.isShown()) {
            snackbarActasPendientes.dismiss();
        }
    }

    private void mostrarSnackbarExito() {
        mostrandoExito = true; // Activar flag
        
        // Ocultar el snackbar anterior
        if (snackbarActasPendientes != null && snackbarActasPendientes.isShown()) {
            snackbarActasPendientes.dismiss();
        }

        // Mostrar mensaje de éxito verde por 3.5 segundos (más visible)
        snackbarActasPendientes = Snackbar.make(
                rootView,
                "✓ Actas sincronizadas correctamente",
                Snackbar.LENGTH_LONG // 3.5 segundos (antes era SHORT = 2s)
        );
        
        // Fondo verde para éxito
        snackbarActasPendientes.setBackgroundTint(getResources().getColor(android.R.color.holo_green_dark));
        snackbarActasPendientes.setTextColor(getResources().getColor(android.R.color.white));
        
        snackbarActasPendientes.show();

        // Después de 3.5 segundos, actualizar el contador (que ocultará el snackbar si no hay pendientes)
        new android.os.Handler().postDelayed(() -> {
            mostrandoExito = false; // Desactivar flag
            actualizarSnackbarPendientes();
        }, 3500); // Aumentado de 2000ms a 3500ms
    }

    // =========================
    // DIALOGO PENDIENTES
    // =========================
    private void mostrarPendientes() {
        new Thread(() -> {
            List<ActaEntity> list = actaDao.pendientes();

            runOnUiThread(() -> {
                if (list == null || list.isEmpty()) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Actas pendientes")
                            .setMessage("No hay actas pendientes de sincronizar ✅")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                String[] items = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ActaEntity a = list.get(i);

                    String fechaHora = (a.fecha != null ? a.fecha : "") +
                            (a.hora != null ? " " + a.hora : "");
                    String tipo = a.tipoActa != null ? a.tipoActa : "ACTA";
                    String lugar = a.lugarInfraccion != null ? a.lugarInfraccion : "";
                    String prop = a.propietario != null ? a.propietario : "";

                    items[i] = "• " + fechaHora + " | " + tipo +
                            "\n" + lugar + (prop.isEmpty() ? "" : " | " + prop);
                }

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Actas pendientes (" + list.size() + ")")
                        .setItems(items, (dialog, which) -> {
                            ActaEntity seleccionada = list.get(which);
                            mostrarOpcionesActa(seleccionada);
                        })
                        .setNegativeButton("Cerrar", null)
                        .setPositiveButton("Sincronizar todas", (d, w) -> confirmarSincronizarTodas(list.size()))
                        .show();
            });
        }).start();
    }

    private void mostrarOpcionesActa(ActaEntity a) {
        String titulo = (a.tipoActa != null ? a.tipoActa : "ACTA") +
                " - " + (a.fecha != null ? a.fecha : "") +
                (a.hora != null ? " " + a.hora : "");

        String detalle = ""
                + "Propietario: " + safe(a.propietario) + "\n"
                + "Lugar: " + safe(a.lugarInfraccion) + "\n"
                + "Acción: " + safe(a.accion) + "\n"
                + "LocalId: " + safe(a.localId);

        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(detalle)
                .setNegativeButton("Cerrar", null)
                .setPositiveButton("Sincronizar esta acta", (d, w) -> {
                    // ✅ Disparamos el worker
                    ejecutarSyncWorker();

                    new AlertDialog.Builder(this)
                            .setTitle("Sincronización")
                            .setMessage("Se inició la sincronización de la acta seleccionada.")
                            .setPositiveButton("OK", null)
                            .show();
                })
                .show();
    }

    private void confirmarSincronizarTodas(int total) {
        new AlertDialog.Builder(this)
                .setTitle("Sincronizar todas")
                .setMessage("Se van a sincronizar " + total + " actas. ¿Continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sí, sincronizar", (d, w) -> {

                    syncing = true;
                    mostrarSnackbarSincronizando();

                    // ✅ SOLO 1 CAMINO DE SYNC: SyncWorker
                    ejecutarSyncWorker();

                    new AlertDialog.Builder(this)
                            .setTitle("Sincronización")
                            .setMessage("Se inició la sincronización. Al finalizar se actualizará el contador automáticamente.")
                            .setPositiveButton("OK", null)
                            .show();
                })
                .show();
    }

    // =========================
    // WORKMANAGER (UNICO)
    // =========================
    private void ejecutarSyncWorker() {

        Constraints c = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(c)
                .addTag("SYNC_INMO")
                .build();

        // ✅ Único para que NO se ejecute doble
        WorkManager.getInstance(this).enqueueUniqueWork(
                UNIQUE_SYNC_NAME,
                ExistingWorkPolicy.KEEP,
                req
        );

        Log.d(TAG, "✅ SyncWorker encolado (UNIQUE) -> " + UNIQUE_SYNC_NAME);
    }

    // =========================
    // WEBVIEW
    // =========================
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        WebView.setWebContentsDebuggingEnabled(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Página cargada exitosamente");
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    // =========================
    // JS INTERFACE
    // =========================
    public class WebAppInterface {

        @JavascriptInterface
        public String getGeoJsonData(String section) {
            try {
                return loadGeoJsonFromAssets("SEC_" + section + ".geojson");
            } catch (Exception e) {
                Log.e(TAG, "Error GeoJSON", e);
                return "{}";
            }
        }

        @JavascriptInterface
        public void sendParcelData(String parcelData) {
            runOnUiThread(() -> {
                Intent intent = new Intent(MainActivity.this, ParcelDetailActivity.class);
                intent.putExtra("PARCEL_DATA", parcelData);
                intent.putExtra("NOMBRE_INSPECTOR", nombreInspector);
                intent.putExtra("APELLIDO_INSPECTOR", apellidoInspector);
                intent.putExtra("LEGAJO_INSPECTOR", legajoInspector);
                intent.putExtra("INSPECTOR_ID", inspectorId);
                startActivity(intent);
            });
        }
    }

    private String loadGeoJsonFromAssets(String fileName) {
        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open(fileName)))) {

            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line).append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error leyendo " + fileName, e);
        }
        return json.toString();
    }

    // =========================
    // UTILS
    // =========================
    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
