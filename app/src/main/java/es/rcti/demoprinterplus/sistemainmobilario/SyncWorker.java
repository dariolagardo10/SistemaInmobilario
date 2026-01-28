package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        AppDb db = AppDb.get(getApplicationContext());

        List<ActaEntity> pendientes = db.actaDao().pendientes();
        Log.d(TAG, "Pendientes=" + (pendientes == null ? 0 : pendientes.size()));

        if (pendientes == null || pendientes.isEmpty()) {
            return Result.success();
        }

        for (ActaEntity acta : pendientes) {
            try {
                Log.d(TAG, "---- Sync acta localId=" + acta.localId + " tipo=" + acta.tipoActa);

                // 1) SUBIR ACTA
                int serverId = ApiSync.subirActa(acta);
                Log.d(TAG, "✅ Acta subida serverId=" + serverId);

                // 2) SUBIR FIRMA
                FirmaEntity firma = db.firmaDao().get(acta.localId);
                if (firma != null && firma.firmaBytes != null && firma.firmaBytes.length > 0) {
                    ApiSync.subirFirma(serverId, firma.firmaBytes);
                    Log.d(TAG, "✅ Firma subida");
                } else {
                    Log.d(TAG, "ℹ️ Sin firma para localId=" + acta.localId);
                }

                // 3) SUBIR IMÁGENES
                List<ImagenEntity> imagenes = db.imagenDao().listByLocalId(acta.localId);
                Log.d(TAG, "Imágenes encontradas=" + (imagenes == null ? 0 : imagenes.size()));

                if (imagenes != null && !imagenes.isEmpty()) {
                    // IMPORTANTE: si falla acá, NO marcamos el acta como synced
                    ApiSync.subirImagenes(serverId, imagenes, getApplicationContext());
                    Log.d(TAG, "✅ Imágenes subidas OK, marcando como synced en Room");

                    // marcar cada imagen como synced
                    for (ImagenEntity im : imagenes) {
                        db.imagenDao().marcarSynced(im.id);
                    }
                } else {
                    Log.d(TAG, "ℹ️ No hay imágenes asociadas a localId=" + acta.localId);
                }

                // 4) MARCAR ACTA COMO SINCRONIZADA (solo si todo salió bien)
                db.actaDao().marcarSynced(acta.localId, serverId);
                Log.d(TAG, "✅ Acta marcada synced localId=" + acta.localId);

            } catch (Exception e) {
                Log.e(TAG, "❌ Error sincronizando localId=" + acta.localId + " -> " + e.getMessage(), e);
                return Result.retry();
            }
        }

        return Result.success();
    }
}
