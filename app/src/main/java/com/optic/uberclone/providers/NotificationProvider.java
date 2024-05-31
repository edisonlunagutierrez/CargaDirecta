package com.optic.uberclone.providers;

import com.optic.uberclone.models.FCMBody;
import com.optic.uberclone.models.FCMResponse;
import com.optic.uberclone.retrofit.IFCMApi;
import com.optic.uberclone.retrofit.RetrofitClient;

import retrofit2.Call;

// Clase que proporciona métodos para enviar notificaciones utilizando FCM (Firebase Cloud Messaging)
public class NotificationProvider {

    // URL base para las solicitudes a FCM
    private String url = "https://fcm.googleapis.com";

    // Constructor de la clase
    public NotificationProvider() {
    }

    // Método para enviar una notificación utilizando FCM
    public Call<FCMResponse> sendNotification(FCMBody body) {
        // Retorna una llamada Retrofit para enviar la notificación utilizando la URL base y la interfaz IFCMApi
        return RetrofitClient.getClientObject(url).create(IFCMApi.class).send(body);
    }
}

