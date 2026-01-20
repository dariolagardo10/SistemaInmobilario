package es.rcti.demoprinterplus.sistemainmobilario;

import androidx.work.ListenableWorker;



import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        AppDb db = AppDb.get(getApplicationContext());

        // 1️⃣ Obtener actas pendientes
        List<ActaEntity> pendientes = db.actaDao().pendientes();

        if (pendientes == null || pendientes.isEmpty()) {
            return Result.success();
        }

        // 2️⃣ Procesar una por una
        for (ActaEntity acta : pendientes) {
            try {
                // ===== SUBIR ACTA =====
                int serverId = ApiSync.subirActa(acta);

                // ===== SUBIR FIRMA =====
                FirmaEntity firma = db.firmaDao().get(acta.localId);
                if (firma != null && firma.firmaBytes != null && firma.firmaBytes.length > 0) {
                    ApiSync.subirFirma(serverId, firma.firmaBytes);
                }

                // ===== SUBIR IMÁGENES =====
                List<ImagenEntity> imagenes = db.imagenDao().listByLocalId(acta.localId);
                if (imagenes != null && !imagenes.isEmpty()) {
                    ApiSync.subirImagenes(serverId, imagenes, getApplicationContext());
                }

                // ===== MARCAR COMO SINCRONIZADA =====
                db.actaDao().marcarSynced(acta.localId, serverId);

            } catch (Exception e) {
                // Si falla UNA, se reintenta todo más tarde
                return Result.retry();
            }
        }

        return Result.success();
    }
}
