package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "http://31.97.172.185/api_inmo.php";

    private static final int TIMEOUT_MS = 30000; // 30 segundos
    private static final String LOGIN_URL = "http://31.97.172.185/oracle_inmo_api.php";
    private Context context;
    private RequestQueue requestQueue;

    public ApiClient(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    // =========================================================
    //  INSERTAR ACTA (INFRACCIÓN o INSPECCIÓN)
    // =========================================================
    public void insertarActaInfraccion(final ActaInfraccionData acta, final ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.POST, BASE_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // 🔹 Log de la respuesta cruda
                        Log.d(TAG, "RAW response insertarActa: '" + response + "'");

                        // 🔹 Si viene null o vacío, no intentamos parsear
                        if (response == null || response.trim().isEmpty()) {
                            String msg = "Respuesta vacía del servidor";
                            Log.e(TAG, msg);
                            callback.onError(msg);
                            return;
                        }

                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            callback.onSuccess(jsonResponse);
                        } catch (JSONException e) {
                            String msg = "Error al procesar la respuesta: " + e.getMessage()
                                    + " | RAW='" + response + "'";
                            Log.e(TAG, msg, e);
                            callback.onError(msg);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Error de red: " +
                                (error.getMessage() != null ? error.getMessage() : "desconocido");
                        Log.e(TAG, errorMessage, error);
                        callback.onError(errorMessage);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();

                // 👉 Tipo de acta SIN tildes (INFRACCION / INSPECCION)
                String tipoActaOriginal = acta.getTipoActa();
                String tipoActaSinTildes = quitarTildes(tipoActaOriginal != null ? tipoActaOriginal : "");
                if (tipoActaSinTildes == null || tipoActaSinTildes.isEmpty()) {
                    tipoActaSinTildes = "INFRACCION"; // por defecto
                }

                // 👉 Acción según tipo de acta
                String accion = "insertarActaInfraccion";
                if ("INSPECCION".equalsIgnoreCase(tipoActaSinTildes)) {
                    accion = "insertarActaInspeccion"; // función nueva en PHP
                }

                // Parámetros para la API
                params.put("accion", accion);
                params.put("numero", acta.getNumeroActa());
                params.put("fecha", acta.getFecha());
                params.put("hora", acta.getHora());
                params.put("propietario", acta.getPropietario());

                // ID del inspector
                params.put("tf_inspector_id", acta.getInspectorId());

                // Datos infractor (opcionales)
                if (acta.getInfractorDni() != null) {
                    params.put("infractor_dni", acta.getInfractorDni());
                }
                if (acta.getInfractorNombre() != null) {
                    params.put("infractor_nombre", acta.getInfractorNombre());
                }
                if (acta.getInfractorDomicilio() != null) {
                    params.put("infractor_domicilio", acta.getInfractorDomicilio());
                }

                // Ubicación y datos de parcela
                params.put("lugar_infraccion", acta.getLugarInfraccion());
                params.put("seccion", acta.getSeccion());
                params.put("chacra", acta.getChacra());
                params.put("manzana", acta.getManzana());
                params.put("parcela", acta.getParcela());
                params.put("lote", acta.getLote());
                params.put("partida", acta.getPartida());

                // Datos adicionales
                params.put("observaciones", acta.getObservaciones());
                params.put("boleta_inspeccion", acta.getBoletaInspeccion());

                // 👉 Enviamos el tipo de acta al backend
                params.put("tipo_acta", tipoActaSinTildes);
                params.put("incumplimiento", acta.isIncumplimiento() ? "1" : "0");
                params.put("clausura_preventiva", acta.isClausuraPreventiva() ? "1" : "0");

                // 👉 Enviamos resultado de inspección (puede venir vacío si es infracción)
                String resultadoInspeccion = acta.getResultadoInspeccion();
                if (resultadoInspeccion == null) resultadoInspeccion = "";
                params.put("resultado_inspeccion", resultadoInspeccion);

                // Faltas seleccionadas (checkboxes) -> solo tendrán "1" cuando sea INFRACCION
                params.put("cartel_obra", acta.isCartelObra() ? "1" : "0");
                params.put("dispositivos_seguridad", acta.isDispositivosSeguridad() ? "1" : "0");
                params.put("numero_permiso", acta.isNumeroPermiso() ? "1" : "0");
                params.put("materiales_vereda", acta.isMaterialesVereda() ? "1" : "0");
                params.put("cerco_obra", acta.isCercoObra() ? "1" : "0");
                params.put("planos_aprobados", acta.isPlanosAprobados() ? "1" : "0");
                params.put("director_obra", acta.isDirectorObra() ? "1" : "0");
                params.put("varios", acta.isVarios() ? "1" : "0");

                // Log opcional para debug
                Log.d(TAG, "Enviando acta. accion=" + accion +
                        ", tipo_acta=" + tipoActaSinTildes +
                        ", resultado_inspeccion=" + resultadoInspeccion);

                return params;
            }
        };

        // Timeout y reintentos
        request.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    // =========================================================
    //  LOGIN
    // =========================================================
    public void loginInspector(String username, String password, final ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.POST, LOGIN_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "RAW response login: '" + response + "'");

                        if (response == null || response.trim().isEmpty()) {
                            String msg = "Respuesta vacía del servidor (login)";
                            Log.e(TAG, msg);
                            callback.onError(msg);
                            return;
                        }

                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            callback.onSuccess(jsonResponse);
                        } catch (JSONException e) {
                            String msg = "Error al procesar la respuesta: " + e.getMessage()
                                    + " | RAW='" + response + "'";
                            Log.e(TAG, msg, e);
                            callback.onError(msg);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Error de red: " +
                                (error.getMessage() != null ? error.getMessage() : "desconocido");
                        Log.e(TAG, errorMessage, error);
                        callback.onError(errorMessage);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "verificar");
                params.put("username", username);
                params.put("password", password);
                return params;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    // =========================================================
    //  BÚSQUEDA DE PARCELA
    // =========================================================
    public void buscarParcelaPorCodigo(String seccion, String chacra, String manzana,
                                       String parcela, final ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.POST, BASE_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "RAW response buscarParcela: '" + response + "'");

                        if (response == null || response.trim().isEmpty()) {
                            String msg = "Respuesta vacía del servidor (buscarParcela)";
                            Log.e(TAG, msg);
                            callback.onError(msg);
                            return;
                        }

                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            callback.onSuccess(jsonResponse);
                        } catch (JSONException e) {
                            String msg = "Error al procesar la respuesta: " + e.getMessage()
                                    + " | RAW='" + response + "'";
                            Log.e(TAG, msg, e);
                            callback.onError(msg);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Error de red: " +
                                (error.getMessage() != null ? error.getMessage() : "desconocido");
                        Log.e(TAG, errorMessage, error);
                        callback.onError(errorMessage);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "buscarParcelaPorCodigo");

                if (seccion != null && !seccion.isEmpty()) {
                    params.put("seccion", seccion);
                }
                if (chacra != null && !chacra.isEmpty()) {
                    params.put("chacra", chacra);
                }
                if (manzana != null && !manzana.isEmpty()) {
                    params.put("manzana", manzana);
                }
                if (parcela != null && !parcela.isEmpty()) {
                    params.put("parcela", parcela);
                }

                return params;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    // =========================================================
    //  UTIL: QUITAR TILDES
    // =========================================================
    private static String quitarTildes(String texto) {
        if (texto == null) return null;
        return texto
                .replace("Á", "A").replace("á", "a")
                .replace("É", "E").replace("é", "e")
                .replace("Í", "I").replace("í", "i")
                .replace("Ó", "O").replace("ó", "o")
                .replace("Ú", "U").replace("ú", "u")
                .replace("Ñ", "N").replace("ñ", "n");
    }
}
