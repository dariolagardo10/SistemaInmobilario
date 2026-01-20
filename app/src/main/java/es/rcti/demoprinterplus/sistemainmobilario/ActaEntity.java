package es.rcti.demoprinterplus.sistemainmobilario;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "actas_inmo")
public class ActaEntity {
    @PrimaryKey @NonNull
    public String localId;        // UUID

    public String accion;         // insertarActaInfraccion / insertarActaInspeccion
    public String numero, fecha, hora, propietario;
    public String tfInspectorId;

    public String infractorDni, infractorNombre, infractorDomicilio;
    public String lugarInfraccion, seccion, chacra, manzana, parcela, lote, partida;
    public String boletaInspeccion, observaciones;

    public String tipoActa;               // INFRACCION / INSPECCION
    public String infraccionDesc;         // para INFRACCION (texto)
    public String resultadoInspeccion;    // para INSPECCION
    // ===== FALTAS (INFRACCION) =====
    public int cartelObra;
    public int dispositivosSeguridad;
    public int numeroPermiso;
    public int materialesVereda;
    public int cercoObra;
    public int planosAprobados;
    public int directorObra;
    public int varios;

    public int incumplimiento;
    public int clausuraPreventiva;

    // ===== INSPECCION =====


    public int serverId;          // 0 si no sincronizó
    public int synced;            // 0/1
    public long createdAt;        // System.currentTimeMillis()
}

