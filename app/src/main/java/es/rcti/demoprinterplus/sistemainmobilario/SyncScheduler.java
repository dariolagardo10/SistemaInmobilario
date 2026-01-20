package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Context;
import androidx.work.*;

public class SyncScheduler {
    public static void enqueue(Context ctx) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .addTag("SYNC_INMO")
                .build();

        WorkManager.getInstance(ctx)
                .enqueueUniqueWork("SYNC_INMO_UNIQUE", ExistingWorkPolicy.KEEP, req);
    }
}

