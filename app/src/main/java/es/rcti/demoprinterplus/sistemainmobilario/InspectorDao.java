package es.rcti.demoprinterplus.sistemainmobilario;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface InspectorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(InspectorEntity i);

    // ✅ Login offline: probá por username primero, si no, por legajo
    @Query("SELECT * FROM inspectores_local WHERE username = :userOrLegajo LIMIT 1")
    InspectorEntity findByUsername(String userOrLegajo);

    @Query("SELECT * FROM inspectores_local WHERE legajo = :userOrLegajo LIMIT 1")
    InspectorEntity findByLegajo(String userOrLegajo);

    @Query("SELECT * FROM inspectores_local ORDER BY lastLoginAt DESC LIMIT 1")
    InspectorEntity last();
}
