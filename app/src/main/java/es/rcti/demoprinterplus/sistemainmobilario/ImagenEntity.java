package es.rcti.demoprinterplus.sistemainmobilario;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "acta_imagenes")
public class ImagenEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull public String localId;   // UUID del acta
    public String uriString;          // file://... (ideal) o content://...
    public int synced = 0;            // 0 pendiente, 1 OK
}
