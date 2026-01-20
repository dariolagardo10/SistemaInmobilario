package es.rcti.demoprinterplus.sistemainmobilario;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "inspectores_local")
public class InspectorEntity {
    @PrimaryKey
    @NonNull
    public String inspectorId;

    public String username;   // ✅ NUEVO: lo que escribe en el login
    public String legajo;
    public String nombre;
    public String apellido;

    public String passHash;   // hash password/pin
    public long lastLoginAt;
}
