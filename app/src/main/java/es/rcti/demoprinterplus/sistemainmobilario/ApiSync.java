package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class ApiSync {

    private static final String TAG = "ApiSync";
    static final String BASE_URL = "http://31.97.172.185/api_inmo.php";

    // ==============================
    // SUBIR ACTA (SYNC)
    // ==============================
    public static int subirActa(ActaEntity a) throws Exception {

        HashMap<String, String> params = paramsForActa(a);

        Log.d(TAG, "⬆️ Subiendo acta localId=" + a.localId + " tipo=" + a.tipoActa);
        String resp = HttpForm.post(BASE_URL, params);
        Log.d(TAG, "⬅️ Resp subirActa: " + resp);

        if (resp == null || resp.trim().isEmpty()) {
            throw new Exception("Respuesta vacía del servidor");
        }

        JSONObject j = new JSONObject(resp);

        // Tu API usa success
        if (!j.optBoolean("success")) {
            throw new Exception(j.optString("error", "Error al subir acta"));
        }

        // ID devuelto
        int actaId = j.optInt("acta_id", 0);
        if (actaId <= 0) {
            // fallback por si cambia el nombre en backend
            actaId = j.optInt("id", j.optInt("actaId", j.optInt("serverId", 0)));
        }

        if (actaId <= 0) {
            throw new Exception("Acta subida pero no llegó acta_id en la respuesta. RAW=" + resp);
        }

        return actaId;
    }

    // ==============================
    // SUBIR FIRMA
    // ==============================
    public static void subirFirma(int actaId, byte[] firmaBytes) throws Exception {

        String base64 = android.util.Base64.encodeToString(
                firmaBytes, android.util.Base64.NO_WRAP
        );

        HashMap<String, String> p = new HashMap<>();
        p.put("accion", "subirFirmaInfractor");
        p.put("actaId", String.valueOf(actaId));
        p.put("firma", base64);

        String resp = HttpForm.post(BASE_URL, p);
        Log.d(TAG, "⬅️ Resp subirFirma: " + resp);

        JSONObject j = new JSONObject(resp);
        if (!j.optBoolean("success")) {
            throw new Exception(j.optString("error", "Error al subir firma"));
        }
    }

    // ==============================
    // SUBIR IMÁGENES
    // ==============================
    public static void subirImagenes(int actaId, List<ImagenEntity> imgs, Context ctx) throws Exception {

        HashMap<String, String> p = new HashMap<>();
        p.put("accion", "subirImagen");
        p.put("actaId", String.valueOf(actaId));

        java.util.ArrayList<String> base64Imgs = new java.util.ArrayList<>();

        for (ImagenEntity im : imgs) {
            byte[] bytes = ContentUri.readAllBytes(ctx, android.net.Uri.parse(im.uriString));
            String b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
            base64Imgs.add(b64);
        }

        String resp = HttpForm.postWithRepeated(
                BASE_URL, p, "imagenes[]", base64Imgs
        );

        Log.d(TAG, "⬅️ Resp subirImagenes: " + resp);

        JSONObject j = new JSONObject(resp);
        if (!j.optBoolean("success")) {
            throw new Exception(j.optString("error", "Error al subir imágenes"));
        }
    }

    // ==============================
    // PARAMS ACTA (CLAVE)
    // ==============================
    private static HashMap<String, String> paramsForActa(ActaEntity a) {

        HashMap<String, String> p = new HashMap<>();

        // ✅ tipo_acta normalizado
        String tipoActa = quitarTildes(n(a.tipoActa)).trim();
        if (tipoActa.isEmpty()) tipoActa = "INFRACCION";

        // ✅ accion forzada coherente
        String accion = "INSPECCION".equalsIgnoreCase(tipoActa)
                ? "insertarActaInspeccion"
                : "insertarActaInfraccion";

        p.put("accion", accion);

        // ✅ ID LOCAL para idempotencia
        p.put("local_id", n(a.localId));

        // Base
        p.put("numero", n(a.numero));
        p.put("fecha", n(a.fecha));
        p.put("hora", n(a.hora));
        p.put("propietario", n(a.propietario));
        p.put("tf_inspector_id", n(a.tfInspectorId));

        // Infractor
        p.put("infractor_dni", n(a.infractorDni));
        p.put("infractor_nombre", n(a.infractorNombre));
        p.put("infractor_domicilio", n(a.infractorDomicilio));

        // Ubicación / parcela
        p.put("lugar_infraccion", n(a.lugarInfraccion));
        p.put("seccion", n(a.seccion));
        p.put("chacra", n(a.chacra));
        p.put("manzana", n(a.manzana));
        p.put("parcela", n(a.parcela));
        p.put("lote", n(a.lote));
        p.put("partida", n(a.partida));

        // Otros
        p.put("boleta_inspeccion", n(a.boletaInspeccion));
        p.put("observaciones", n(a.observaciones));
        p.put("tipo_acta", tipoActa);

        // ✅ Resultado inspección limpio
        String res = n(a.resultadoInspeccion).trim();
        if (res.equalsIgnoreCase("Ningún resultado seleccionado")) res = "";
        p.put("resultado_inspeccion", res);

        // ✅ FALTAS REALES (0/1) — NO MÁS "0" FIJO
        // Si tu ActaEntity tiene int 0/1, esto va directo.
        p.put("cartel_obra", String.valueOf(a.cartelObra));
        p.put("dispositivos_seguridad", String.valueOf(a.dispositivosSeguridad));
        p.put("numero_permiso", String.valueOf(a.numeroPermiso));
        p.put("materiales_vereda", String.valueOf(a.materialesVereda));
        p.put("cerco_obra", String.valueOf(a.cercoObra));
        p.put("planos_aprobados", String.valueOf(a.planosAprobados));
        p.put("director_obra", String.valueOf(a.directorObra));
        p.put("varios", String.valueOf(a.varios));

        // ✅ EXTRA (0/1)
        p.put("incumplimiento", String.valueOf(a.incumplimiento));
        p.put("clausura_preventiva", String.valueOf(a.clausuraPreventiva));

        // ✅ Log de control (clave para debug)
        Log.d(TAG, "Params acta localId=" + a.localId
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

        return p;
    }

    private static String n(String s) {
        return s == null ? "" : s;
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
