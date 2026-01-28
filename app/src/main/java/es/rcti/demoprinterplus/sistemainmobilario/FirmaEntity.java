package es.rcti.demoprinterplus.sistemainmobilario;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "acta_firmas")
public class FirmaEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull public String localId;
    public byte[] firmaBytes;
    public int synced = 0;
}
