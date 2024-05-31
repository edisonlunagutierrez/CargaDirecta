package com.optic.uberclone.retrofit;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

// Interfaz para definir las solicitudes a la API de Google Maps Directions
public interface IGoogleApi {

    // Método GET para obtener las direcciones entre dos ubicaciones
    @GET
    Call<String> getDirections(@Url String url);

}

