package com.optic.uberclone.retrofit;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

// Clase que proporciona métodos estáticos para obtener instancias de Retrofit
public class RetrofitClient {

    // Método estático para obtener un cliente Retrofit para solicitudes que devuelven una respuesta de tipo String
    public static Retrofit getClient(String url) {
        // Crea y configura un nuevo cliente Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        return retrofit;
    }

    // Método estático para obtener un cliente Retrofit para solicitudes que devuelven una respuesta de tipo objeto
    public static Retrofit getClientObject(String url) {
        // Crea y configura un nuevo cliente Retrofit con un convertidor Gson
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit;
    }
}

