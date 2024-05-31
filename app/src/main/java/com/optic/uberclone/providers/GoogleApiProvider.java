package com.optic.uberclone.providers;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;
import com.optic.uberclone.R;
import com.optic.uberclone.retrofit.IGoogleApi;
import com.optic.uberclone.retrofit.RetrofitClient;

import java.util.Date;

import retrofit2.Call;

// Clase que proporciona métodos para interactuar con la API de Google Maps
public class GoogleApiProvider {

    // Contexto de la aplicación
    private Context context;

    // Constructor de la clase
    public GoogleApiProvider(Context context) {
        // Asigna el contexto proporcionado al contexto de la clase
        this.context = context;
    }

    // Método para obtener las direcciones entre dos ubicaciones utilizando la API de Google Maps
    public Call<String> getDirections(LatLng originLatLng, LatLng destinationLatLng) {
        // URL base para las solicitudes a la API de Google Maps
        String baseUrl = "https://maps.googleapis.com";
        // Construye la consulta para obtener las direcciones entre las ubicaciones de origen y destino
        String query = "/maps/api/directions/json?mode=driving&transit_routing_preferences=less_driving&"
                + "origin=" + originLatLng.latitude + "," + originLatLng.longitude + "&"
                + "destination=" + destinationLatLng.latitude + "," + destinationLatLng.longitude + "&"
                + "departure_time=" + (new Date().getTime() + (60*60*1000)) + "&"
                + "traffic_model=best_guess&"
                + "key=" + context.getResources().getString(R.string.google_maps_key);
        // Retorna una llamada Retrofit para obtener las direcciones utilizando la URL construida y la interfaz IGoogleApi
        return RetrofitClient.getClient(baseUrl).create(IGoogleApi.class).getDirections(baseUrl + query);
    }
}

