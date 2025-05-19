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

import es.rcti.demoprinterplus.sistemainmobilario.ActaInfraccionData;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "https://systemposadas.com/api_inmo.php";
    private static final int TIMEOUT_MS = 30000; // 30 segundos
    private static final String LOGIN_URL = "https://systemposadas.com/oracle_api.php";
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

    public void insertarActaInfraccion(final ActaInfraccionData acta, final ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.POST, BASE_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            callback.onSuccess(jsonResponse);
                        } catch (JSONException e) {
                            callback.onError("Error al procesar la respuesta: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Error de red: " + (error.getMessage() != null ? error.getMessage() : "desconocido");
                        Log.e(TAG, errorMessage, error);
                        callback.onError(errorMessage);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();

                // Parámetros para la API
                params.put("accion", "insertarActaInfraccion");
                params.put("numero", acta.getNumeroActa());
                params.put("fecha", acta.getFecha());
                params.put("hora", acta.getHora());
                params.put("propietario", acta.getPropietario());

                // Agregar el ID del inspector (Importante para la relación)
                params.put("tf_inspector_id", acta.getInspectorId());

                // Datos infractor (pueden ser null)
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

                // Faltas seleccionadas (checkboxes)
                params.put("cartel_obra", acta.isCartelObra() ? "1" : "0");
                params.put("dispositivos_seguridad", acta.isDispositivosSeguridad() ? "1" : "0");
                params.put("numero_permiso", acta.isNumeroPermiso() ? "1" : "0");
                params.put("materiales_vereda", acta.isMaterialesVereda() ? "1" : "0");
                params.put("cerco_obra", acta.isCercoObra() ? "1" : "0");
                params.put("planos_aprobados", acta.isPlanosAprobados() ? "1" : "0");
                params.put("director_obra", acta.isDirectorObra() ? "1" : "0");
                params.put("varios", acta.isVarios() ? "1" : "0");

                return params;
            }
        };

        // Configurar políticas de tiempo de espera y reintentos
        request.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // Agregar la solicitud a la cola
        requestQueue.add(request);
    }

    // Métodos adicionales como login, búsqueda, etc. (opcional)
    public void loginInspector(String username, String password, final ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.POST, LOGIN_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            callback.onSuccess(jsonResponse);
                        } catch (JSONException e) {
                            callback.onError("Error al procesar la respuesta: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Error de red: " + (error.getMessage() != null ? error.getMessage() : "desconocido");
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

    public void buscarParcelaPorCodigo(String seccion, String chacra, String manzana, String parcela, final ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.POST, BASE_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            callback.onSuccess(jsonResponse);
                        } catch (JSONException e) {
                            callback.onError("Error al procesar la respuesta: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Error de red: " + (error.getMessage() != null ? error.getMessage() : "desconocido");
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
}