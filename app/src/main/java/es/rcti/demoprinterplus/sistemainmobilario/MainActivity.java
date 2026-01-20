package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private Button btnActasPendientes;

    private static final String TAG = "MainActivity";

    // 🔹 Inspector
    private String nombreInspector;
    private String apellidoInspector;
    private String legajoInspector;
    private String inspectorId;

    // 🔹 Room
    private AppDb db;
    private ActaDao actaDao;

    // 🔹 Volley (misma base que tu ApiClient)
    private static final String BASE_URL = "http://31.97.172.185/api_inmo.php";
    private static final int TIMEOUT_MS = 30000;
    private RequestQueue requestQueue;

    // Para evitar doble click mientras sincroniza
    private boolean syncing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(this);

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
        btnActasPendientes = findViewById(R.id.btnActasPendientes);

        btnActasPendientes.setOnClickListener(v -> {
            if (syncing) return;
            mostrarPendientes();
        });

        setupWebView();
        actualizarBotonPendientes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarBotonPendientes();
    }

    // =========================
    // CONTADOR
    // =========================
    private void actualizarBotonPendientes() {
        new Thread(() -> {
            int count = actaDao.countPending();
            runOnUiThread(() ->
                    btnActasPendientes.setText("Actas pendientes (" + count + ")")
            );
        }).start();
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
                        .setPositiveButton("Sincronizar todas", (d, w) -> confirmarSincronizarTodas(list))
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
                .setPositiveButton("Sincronizar esta acta", (d, w) -> sincronizarUna(a))
                .show();
    }

    private void confirmarSincronizarTodas(List<ActaEntity> list) {
        new AlertDialog.Builder(this)
                .setTitle("Sincronizar todas")
                .setMessage("Se van a sincronizar " + list.size() + " actas. ¿Continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sí, sincronizar", (d, w) -> {
                    syncing = true;
                    btnActasPendientes.setEnabled(false);
                    sincronizarSiguiente(list, 0, 0, 0);
                })
                .show();
    }

    // =========================
    // SINCRONIZACION (UNA)
    // =========================
    private void sincronizarUna(ActaEntity a) {
        syncing = true;
        btnActasPendientes.setEnabled(false);

        subirActaAlServidor(a, new SyncCallback() {
            @Override public void onSuccess(int serverId) {
                new Thread(() -> {
                    actaDao.marcarSynced(a.localId, serverId);
                    runOnUiThread(() -> {
                        syncing = false;
                        btnActasPendientes.setEnabled(true);
                        actualizarBotonPendientes();
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Sincronizada ✅")
                                .setMessage("Acta subida correctamente.\nServerId: " + serverId)
                                .setPositiveButton("OK", null)
                                .show();
                    });
                }).start();
            }

            @Override public void onError(String msg) {
                runOnUiThread(() -> {
                    syncing = false;
                    btnActasPendientes.setEnabled(true);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Error al sincronizar")
                            .setMessage(msg)
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        });
    }

    // =========================
    // SINCRONIZACION (TODAS)
    // =========================
    private void sincronizarSiguiente(List<ActaEntity> list, int index, int ok, int fail) {
        if (index >= list.size()) {
            syncing = false;
            btnActasPendientes.setEnabled(true);
            actualizarBotonPendientes();

            new AlertDialog.Builder(this)
                    .setTitle("Sincronización finalizada")
                    .setMessage("OK: " + ok + "\nError: " + fail)
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        ActaEntity a = list.get(index);

        subirActaAlServidor(a, new SyncCallback() {
            @Override public void onSuccess(int serverId) {
                new Thread(() -> actaDao.marcarSynced(a.localId, serverId)).start();
                runOnUiThread(() -> actualizarBotonPendientes());
                sincronizarSiguiente(list, index + 1, ok + 1, fail);
            }

            @Override public void onError(String msg) {
                Log.e(TAG, "Error sincronizando localId=" + a.localId + " -> " + msg);
                sincronizarSiguiente(list, index + 1, ok, fail + 1);
            }
        });
    }

    // =========================
    // POST AL SERVIDOR (VOLLEY)
    // =========================
    private interface SyncCallback {
        void onSuccess(int serverId);
        void onError(String msg);
    }

    private void subirActaAlServidor(final ActaEntity a, final SyncCallback cb) {
        StringRequest request = new StringRequest(Request.Method.POST, BASE_URL,
                response -> {
                    Log.d(TAG, "RAW response syncActa: '" + response + "'");

                    if (response == null || response.trim().isEmpty()) {
                        cb.onError("Respuesta vacía del servidor");
                        return;
                    }

                    try {
                        JSONObject json = new JSONObject(response);

                        if (json.has("error") && !json.isNull("error")) {
                            cb.onError(json.optString("error", "Error desconocido"));
                            return;
                        }

                        int serverId =
                                json.optInt("actaId",
                                        json.optInt("serverId",
                                                json.optInt("id",
                                                        json.optInt("acta_id", 0))));

                        if (serverId <= 0) serverId = 1;
                        cb.onSuccess(serverId);

                    } catch (JSONException e) {
                        cb.onError("Error parseando respuesta: " + e.getMessage() + "\nRAW=" + response);
                    }
                },
                (VolleyError error) -> {
                    String errorMessage = "Error de red: " +
                            (error.getMessage() != null ? error.getMessage() : "desconocido");
                    cb.onError(errorMessage);
                }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();

                // ✅ tipo_acta normalizado
                String tipoActa = quitarTildes(safe(a.tipoActa));
                if (tipoActa.isEmpty()) tipoActa = "INFRACCION";

                // ✅ accion coherente con tipoActa
                String accion = safe(a.accion);
                if (accion.isEmpty()) {
                    accion = "INSPECCION".equalsIgnoreCase(tipoActa)
                            ? "insertarActaInspeccion"
                            : "insertarActaInfraccion";
                }
                // Forzar coherencia sí o sí
                if ("INSPECCION".equalsIgnoreCase(tipoActa)) {
                    accion = "insertarActaInspeccion";
                } else {
                    accion = "insertarActaInfraccion";
                }

                params.put("accion", accion);

                // Campos base
                params.put("numero", safe(a.numero));
                params.put("fecha", safe(a.fecha));
                params.put("hora", safe(a.hora));
                params.put("propietario", safe(a.propietario));

                // Inspector
                params.put("tf_inspector_id", safe(a.tfInspectorId));

                // Infractor (opcionales)
                if (!safe(a.infractorDni).isEmpty()) params.put("infractor_dni", safe(a.infractorDni));
                if (!safe(a.infractorNombre).isEmpty()) params.put("infractor_nombre", safe(a.infractorNombre));
                if (!safe(a.infractorDomicilio).isEmpty()) params.put("infractor_domicilio", safe(a.infractorDomicilio));

                // Ubicación / parcela
                params.put("lugar_infraccion", safe(a.lugarInfraccion));
                params.put("seccion", safe(a.seccion));
                params.put("chacra", safe(a.chacra));
                params.put("manzana", safe(a.manzana));
                params.put("parcela", safe(a.parcela));
                params.put("lote", safe(a.lote));
                params.put("partida", safe(a.partida));

                // Otros
                params.put("observaciones", safe(a.observaciones));
                params.put("boleta_inspeccion", safe(a.boletaInspeccion));
                params.put("tipo_acta", tipoActa);

                // ✅ Resultado inspección (limpio)
                String res = safe(a.resultadoInspeccion);
                if (res.equalsIgnoreCase("Ningún resultado seleccionado")) res = "";
                params.put("resultado_inspeccion", res);

                // ✅ FALTAS REALES DESDE ROOM (0/1)
                // (si tu Entity usa boolean, cambiá a: (a.campo ? "1" : "0"))
                params.put("cartel_obra", String.valueOf(a.cartelObra));
                params.put("dispositivos_seguridad", String.valueOf(a.dispositivosSeguridad));
                params.put("numero_permiso", String.valueOf(a.numeroPermiso));
                params.put("materiales_vereda", String.valueOf(a.materialesVereda));
                params.put("cerco_obra", String.valueOf(a.cercoObra));
                params.put("planos_aprobados", String.valueOf(a.planosAprobados));
                params.put("director_obra", String.valueOf(a.directorObra));
                params.put("varios", String.valueOf(a.varios));

                // ✅ NUEVOS
                params.put("incumplimiento", String.valueOf(a.incumplimiento));
                params.put("clausura_preventiva", String.valueOf(a.clausuraPreventiva));

                // Log para ver qué manda
                Log.d(TAG, "Sync acta localId=" + a.localId
                        + " accion=" + accion
                        + " tipo_acta=" + tipoActa
                        + " faltas=[cartel=" + a.cartelObra
                        + ", disp=" + a.dispositivosSeguridad
                        + ", permiso=" + a.numeroPermiso
                        + ", matVereda=" + a.materialesVereda
                        + ", cerco=" + a.cercoObra
                        + ", planos=" + a.planosAprobados
                        + ", director=" + a.directorObra
                        + ", varios=" + a.varios
                        + ", incumpl=" + a.incumplimiento
                        + ", clausura=" + a.clausuraPreventiva + "]"
                        + " resInspeccion=" + res);

                return params;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
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

    private static String quitarTildes(String texto) {
        if (texto == null) return "";
        return texto
                .replace("Á", "A").replace("á", "a")
                .replace("É", "E").replace("é", "e")
                .replace("Í", "I").replace("í", "i")
                .replace("Ó", "O").replace("ó", "o")
                .replace("Ú", "U").replace("ú", "u")
                .replace("Ñ", "N").replace("ñ", "n");
    }
}
