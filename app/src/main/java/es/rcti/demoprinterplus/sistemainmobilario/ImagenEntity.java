package es.rcti.demoprinterplus.sistemainmobilario;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "acta_imagenes")
public class ImagenEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull public String localId;   // UUID del acta
    public String uriString;          // content://... o file://...
}

