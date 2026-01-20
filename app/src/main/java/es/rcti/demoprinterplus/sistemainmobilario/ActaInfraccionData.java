package es.rcti.demoprinterplus.sistemainmobilario;

import android.graphics.Bitmap;
import android.net.Uri;

import java.util.List;

public class ActaInfraccionData {

    private String id;
    private Bitmap firmaInfractor;
    private List<Uri> imagenesPrueba;

    private String numeroActa;
    private String propietario;
    private String domicilio;
    private String lugarInfraccion;
    private String fecha;
    private String hora;

    private String seccion;
    private String chacra;
    private String manzana;
    private String parcela;
    private String lote;
    private String partida;

    private String infractorNombre;
    private String infractorDomicilio;
    private String infractorDni;

    // =============================
    //   CAUSAS / FALTAS (INFRACCION)
    // =============================
    private boolean cartelObra;
    private boolean dispositivosSeguridad;
    private boolean numeroPermiso;
    private boolean materialesVereda;
    private boolean cercoObra;
    private boolean planosAprobados;
    private boolean directorObra;
    private boolean varios;

    // ✅ NUEVOS
    private boolean incumplimiento;
    private boolean clausuraPreventiva;

    // =============================
    //   INSPECCION
    // =============================
    private String tipoActa;            // INFRACCION / INSPECCION
    private String resultadoInspeccion; // Resultado inspección

    private String observaciones;
    private String boletaInspeccion;

    private int logoResourceId;
    private String inspectorId;

    // =============================
    //   GETTERS & SETTERS
    // =============================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Bitmap getFirmaInfractor() { return firmaInfractor; }
    public void setFirmaInfractor(Bitmap firmaInfractor) { this.firmaInfractor = firmaInfractor; }

    public List<Uri> getImagenesPrueba() { return imagenesPrueba; }
    public void setImagenesPrueba(List<Uri> imagenesPrueba) { this.imagenesPrueba = imagenesPrueba; }

    public String getNumeroActa() { return numeroActa; }
    public void setNumeroActa(String numeroActa) { this.numeroActa = numeroActa; }

    public String getPropietario() { return propietario; }
    public void setPropietario(String propietario) { this.propietario = propietario; }

    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }

    public String getLugarInfraccion() { return lugarInfraccion; }
    public void setLugarInfraccion(String lugarInfraccion) { this.lugarInfraccion = lugarInfraccion; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getSeccion() { return seccion; }
    public void setSeccion(String seccion) { this.seccion = seccion; }

    public String getChacra() { return chacra; }
    public void setChacra(String chacra) { this.chacra = chacra; }

    public String getManzana() { return manzana; }
    public void setManzana(String manzana) { this.manzana = manzana; }

    public String getParcela() { return parcela; }
    public void setParcela(String parcela) { this.parcela = parcela; }

    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }

    public String getPartida() { return partida; }
    public void setPartida(String partida) { this.partida = partida; }

    public String getInfractorNombre() { return infractorNombre; }
    public void setInfractorNombre(String infractorNombre) { this.infractorNombre = infractorNombre; }

    public String getInfractorDomicilio() { return infractorDomicilio; }
    public void setInfractorDomicilio(String infractorDomicilio) { this.infractorDomicilio = infractorDomicilio; }

    public String getInfractorDni() { return infractorDni; }
    public void setInfractorDni(String infractorDni) { this.infractorDni = infractorDni; }

    public boolean isCartelObra() { return cartelObra; }
    public void setCartelObra(boolean cartelObra) { this.cartelObra = cartelObra; }

    public boolean isDispositivosSeguridad() { return dispositivosSeguridad; }
    public void setDispositivosSeguridad(boolean dispositivosSeguridad) { this.dispositivosSeguridad = dispositivosSeguridad; }

    public boolean isNumeroPermiso() { return numeroPermiso; }
    public void setNumeroPermiso(boolean numeroPermiso) { this.numeroPermiso = numeroPermiso; }

    public boolean isMaterialesVereda() { return materialesVereda; }
    public void setMaterialesVereda(boolean materialesVereda) { this.materialesVereda = materialesVereda; }

    public boolean isCercoObra() { return cercoObra; }
    public void setCercoObra(boolean cercoObra) { this.cercoObra = cercoObra; }

    public boolean isPlanosAprobados() { return planosAprobados; }
    public void setPlanosAprobados(boolean planosAprobados) { this.planosAprobados = planosAprobados; }

    public boolean isDirectorObra() { return directorObra; }
    public void setDirectorObra(boolean directorObra) { this.directorObra = directorObra; }

    public boolean isVarios() { return varios; }
    public void setVarios(boolean varios) { this.varios = varios; }

    // ✅ NUEVOS
    public boolean isIncumplimiento() { return incumplimiento; }
    public void setIncumplimiento(boolean incumplimiento) { this.incumplimiento = incumplimiento; }

    public boolean isClausuraPreventiva() { return clausuraPreventiva; }
    public void setClausuraPreventiva(boolean clausuraPreventiva) { this.clausuraPreventiva = clausuraPreventiva; }

    public String getTipoActa() { return tipoActa; }
    public void setTipoActa(String tipoActa) { this.tipoActa = tipoActa; }

    public String getResultadoInspeccion() { return resultadoInspeccion; }
    public void setResultadoInspeccion(String resultadoInspeccion) { this.resultadoInspeccion = resultadoInspeccion; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getBoletaInspeccion() { return boletaInspeccion; }
    public void setBoletaInspeccion(String boletaInspeccion) { this.boletaInspeccion = boletaInspeccion; }

    public int getLogoResourceId() { return logoResourceId; }
    public void setLogoResourceId(int logoResourceId) { this.logoResourceId = logoResourceId; }

    public String getInspectorId() { return inspectorId; }
    public void setInspectorId(String inspectorId) { this.inspectorId = inspectorId; }
}
