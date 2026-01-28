package es.rcti.demoprinterplus.sistemainmobilario;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ImagenDao {
    @Insert
    void insert(ImagenEntity img);

    @Query("SELECT * FROM acta_imagenes WHERE localId=:localId ORDER BY id ASC")
    List<ImagenEntity> listByLocalId(String localId);

    @Query("UPDATE acta_imagenes SET synced = 1 WHERE id = :id")
    void marcarSynced(int id);

    @Query("DELETE FROM acta_imagenes WHERE localId=:localId")
    void deleteByLocalId(String localId);
}
