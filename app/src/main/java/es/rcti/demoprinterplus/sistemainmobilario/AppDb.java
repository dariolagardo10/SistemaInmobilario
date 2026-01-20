package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                ActaEntity.class,
                ImagenEntity.class,
                FirmaEntity.class,
                InspectorEntity.class   // 👈 AGREGADO
        },
        version = 3,               // 👈 SUBIMOS VERSIÓN
        exportSchema = false
)
public abstract class AppDb extends RoomDatabase {

    private static volatile AppDb INSTANCE;

    public abstract ActaDao actaDao();
    public abstract ImagenDao imagenDao();
    public abstract FirmaDao firmaDao();
    public abstract InspectorDao inspectorDao(); // 👈 AGREGADO

    public static AppDb get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDb.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    ctx.getApplicationContext(),
                                    AppDb.class,
                                    "inmo_offline.db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
