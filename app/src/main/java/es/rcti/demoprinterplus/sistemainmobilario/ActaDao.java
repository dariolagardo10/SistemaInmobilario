package es.rcti.demoprinterplus.sistemainmobilario;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;
@Dao
public interface ActaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ActaEntity a);

    // 🔹 Actas pendientes
    @Query("SELECT * FROM actas_inmo WHERE synced = 0 OR serverId = 0 ORDER BY createdAt ASC")
    List<ActaEntity> pendientes();

    // 🔹 Cantidad de pendientes
    @Query("SELECT COUNT(*) FROM actas_inmo WHERE synced = 0 OR serverId = 0")
    int countPending();

    // 🔹 Marcar como sincronizada
    @Query("UPDATE actas_inmo SET synced = 1, serverId = :serverId WHERE localId = :localId")
    void marcarSynced(String localId, int serverId);
}
