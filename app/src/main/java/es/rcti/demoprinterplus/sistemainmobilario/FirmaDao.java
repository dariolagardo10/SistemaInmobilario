package es.rcti.demoprinterplus.sistemainmobilario;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface FirmaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(FirmaEntity f);

    @Query("SELECT * FROM acta_firmas WHERE localId=:localId LIMIT 1")
    FirmaEntity get(String localId);

    @Query("DELETE FROM acta_firmas WHERE localId=:localId")
    void delete(String localId);
}
