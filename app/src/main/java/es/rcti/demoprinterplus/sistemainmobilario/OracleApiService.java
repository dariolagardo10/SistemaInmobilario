package es.rcti.demoprinterplus.sistemainmobilario;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface OracleApiService {

    // Otros métodos existentes...
    @FormUrlEncoded
    @POST("api_inmo.php")
    Call<RespuestaSubirImagen> subirImagen(
            @Field("accion") String accion,
            @Field("actaId") String actaId,
            @Field("imagenes[]") List<String> imagenesBase64   // con los corchetes
    );

    @FormUrlEncoded
    @POST("api_inmo.php")
    Call<RespuestaSubirFirma> subirFirmaInfractor(
            @Field("accion") String accion,
            @Field("actaId") String actaId,
            @Field("firma") String firmaBase64
    );

}