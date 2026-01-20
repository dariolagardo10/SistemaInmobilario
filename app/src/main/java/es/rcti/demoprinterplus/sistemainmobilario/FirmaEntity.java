package es.rcti.demoprinterplus.sistemainmobilario;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "acta_firmas")
public class FirmaEntity {
    @PrimaryKey @NonNull
    public String localId;
    public byte[] firmaBytes; // PNG/JPG
}
